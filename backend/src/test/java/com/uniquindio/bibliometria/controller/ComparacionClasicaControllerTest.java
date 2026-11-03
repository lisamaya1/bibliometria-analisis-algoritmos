package com.uniquindio.bibliometria.controller;

import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.service.*;
import com.uniquindio.bibliometria.util.JsonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class ComparacionClasicaControllerTest {
    private static MockMvc controladorReal() throws IOException {
        var corpus = JsonUtil.cargarArticulosDesdeJson(PreprocesamientoService.RUTA_JSON_DEFECTO);
        var lector = new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path pdfs, Path json) { return corpus; }
        };
        return MockMvcBuilders.standaloneSetup(new ComparacionClasicaController(new ComparacionClasicaService(lector)),
                new JaccardController(new JaccardService(lector)), new DiceController(new DiceService(lector)),
                new LevenshteinController(new LevenshteinService(lector)), new CosenoTfidfController(new CosenoTfidfService(lector))).build();
    }

    @Test void losCuatroResultadosCoincidenConLosEndpointsIndividuales() throws Exception {
        var mvc = controladorReal();
        var respuesta = mvc.perform(get("/api/similitud/clasicos").param("articulos", "15,19")).andReturn().getResponse();
        assertEquals(200, respuesta.getStatus());
        var mapper = new JsonMapper();
        var json = mapper.readTree(respuesta.getContentAsByteArray());
        assertEquals(1, json.get("totalParejas").asInt());
        assertEquals(20, json.get("totalDocumentosIdf").asInt());
        var pareja = json.get("parejas").get(0);
        String[][] algoritmos = {{"jaccard", "jaccard"}, {"dice", "dice"}, {"levenshtein", "levenshtein"}, {"coseno-tfidf", "cosenoTfidf"}};
        for (var algoritmo : algoritmos) {
            var individual = mvc.perform(get("/api/" + algoritmo[0]).param("articuloA", "15").param("articuloB", "19")).andReturn().getResponse();
            assertEquals(200, individual.getStatus());
            var datos = mapper.readTree(individual.getContentAsByteArray());
            assertEquals(datos.get("resultado").get("similitud").asDouble(), pareja.get(algoritmo[1]).get("similitud").asDouble(), 1e-12);
            assertEquals(datos.get("porcentaje").asDouble(), pareja.get(algoritmo[1]).get("porcentaje").asDouble(), 1e-10);
            if (algoritmo[0].equals("levenshtein")) assertEquals(datos.get("resultado").get("distancia").asInt(), pareja.get("levenshtein").get("distancia").asInt());
        }
        assertFalse(respuesta.getContentAsString().contains("conjuntoA"));
        assertFalse(respuesta.getContentAsString().contains("matriz"));
    }

    @Test void admiteListaSeparadaPorComasYTresArticulosConRepetidos() throws Exception {
        var respuesta = controladorReal().perform(get("/api/similitud/clasicos").param("articulos", "19,3,15,3")).andReturn().getResponse();
        assertEquals(200, respuesta.getStatus());
        var json = new JsonMapper().readTree(respuesta.getContentAsByteArray());
        assertEquals(3, json.get("totalParejas").asInt());
        assertEquals(3, json.get("articulosSeleccionados").size());
        assertEquals(3, json.get("parejas").get(0).get("articuloA").get("id").asInt());
        assertEquals(15, json.get("parejas").get(0).get("articuloB").get("id").asInt());
    }

    @Test void rechazaParametrosInvalidosYArticuloInexistente() throws Exception {
        var mvc = controladorReal();
        assertEquals(400, mvc.perform(get("/api/similitud/clasicos")).andReturn().getResponse().getStatus());
        for (String ids : List.of("", "abc", "15", "15,15", "0,15", "15,99")) {
            assertEquals(400, mvc.perform(get("/api/similitud/clasicos").param("articulos", ids)).andReturn().getResponse().getStatus(), ids);
        }
    }

    @Test void errorDeLecturaDevuelve500SinDetalleInterno() throws Exception {
        var lector = new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path pdfs, Path json) throws IOException { throw new IOException("detalle interno"); }
        };
        var mvc = MockMvcBuilders.standaloneSetup(new ComparacionClasicaController(new ComparacionClasicaService(lector))).build();
        var respuesta = mvc.perform(get("/api/similitud/clasicos").param("articulos", "15,19")).andReturn().getResponse();
        assertEquals(500, respuesta.getStatus());
        assertFalse(respuesta.getContentAsString().contains("detalle interno"));
    }

    @Test void normaCeroDevuelve400IdentificandoPareja() throws Exception {
        var lector = new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path pdfs, Path json) {
                return List.of(new ArticuloDocumental(1, "a.pdf", "A", List.of(), "comun", "comun", List.of("comun"), 1),
                        new ArticuloDocumental(2, "b.pdf", "B", List.of(), "comun", "comun", List.of("comun"), 1));
            }
        };
        var mvc = MockMvcBuilders.standaloneSetup(new ComparacionClasicaController(new ComparacionClasicaService(lector))).build();
        var respuesta = mvc.perform(get("/api/similitud/clasicos").param("articulos", "1,2")).andReturn().getResponse();
        assertEquals(400, respuesta.getStatus());
        var mensaje = new JsonMapper().readTree(respuesta.getContentAsByteArray()).get("mensaje").asString();
        assertTrue(mensaje.contains("1 vs 2"));
        assertTrue(mensaje.contains("norma TF-IDF cero"));
    }
}
