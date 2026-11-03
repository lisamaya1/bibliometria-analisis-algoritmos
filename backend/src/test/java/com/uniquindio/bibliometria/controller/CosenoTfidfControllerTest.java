package com.uniquindio.bibliometria.controller;

import com.uniquindio.bibliometria.service.CosenoTfidfService;
import com.uniquindio.bibliometria.service.PreprocesamientoService;
import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.util.JsonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class CosenoTfidfControllerTest {
    @Test void devuelveErrorClaroSiElVectorTieneNormaCero() throws Exception {
        var articulo = new ArticuloDocumental(1, "ejemplo.pdf", "Título", List.of(),
                "Original", "comun", List.of("comun"), 1);
        var lector = new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path pdfs, Path json) { return List.of(articulo); }
        };
        var mvc = MockMvcBuilders.standaloneSetup(new CosenoTfidfController(new CosenoTfidfService(lector))).build();
        var respuesta = mvc.perform(get("/api/coseno-tfidf").param("articuloA", "1").param("articuloB", "1"))
                .andReturn().getResponse();
        assertEquals(400, respuesta.getStatus());
        var json = new JsonMapper().readTree(respuesta.getContentAsByteArray());
        assertTrue(json.get("mensaje").asString().contains("norma TF-IDF cero"));
    }

    @Test void exponeElCasoRealComoJsonYRechazaParametrosInvalidos() throws Exception {
        var corpus = JsonUtil.cargarArticulosDesdeJson(PreprocesamientoService.RUTA_JSON_DEFECTO);
        var lector = new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path pdfs, Path json) { return corpus; }
        };
        var mvc = MockMvcBuilders.standaloneSetup(new CosenoTfidfController(new CosenoTfidfService(lector))).build();
        var respuesta = mvc.perform(get("/api/coseno-tfidf").param("articuloA", "15").param("articuloB", "19"))
                .andReturn().getResponse();
        assertEquals(200, respuesta.getStatus());
        var json = new JsonMapper().readTree(respuesta.getContentAsByteArray());
        assertEquals(20, json.get("resultado").get("totalDocumentos").asInt());
        assertEquals(1131, json.get("resultado").get("tamanoVocabulario").asInt());
        assertEquals(0.03858898108939938, json.get("resultado").get("similitud").asDouble(), 1e-12);
        assertEquals(3.8588981089399375, json.get("porcentaje").asDouble(), 1e-10);
        assertFalse(respuesta.getContentAsString().contains("pesos"));
        assertFalse(respuesta.getContentAsString().contains("frecuencias"));
        assertEquals(400, mvc.perform(get("/api/coseno-tfidf").param("articuloA", "0").param("articuloB", "19"))
                .andReturn().getResponse().getStatus());
        assertEquals(400, mvc.perform(get("/api/coseno-tfidf").param("articuloA", "15").param("articuloB", "99"))
                .andReturn().getResponse().getStatus());
    }

    @Test void requiereAmbosIdentificadoresNumericos() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new CosenoTfidfController(new CosenoTfidfService(new PreprocesamientoService()))).build();
        assertEquals(400, mvc.perform(get("/api/coseno-tfidf").param("articuloA", "15"))
                .andReturn().getResponse().getStatus());
        assertEquals(400, mvc.perform(get("/api/coseno-tfidf").param("articuloA", "abc").param("articuloB", "19"))
                .andReturn().getResponse().getStatus());
    }

    @Test void informaErrorDeLecturaSinExponerDetallesInternos() throws Exception {
        var lector = new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path pdfs, Path json) throws IOException {
                throw new IOException("detalle interno");
            }
        };
        var mvc = MockMvcBuilders.standaloneSetup(new CosenoTfidfController(new CosenoTfidfService(lector))).build();
        var respuesta = mvc.perform(get("/api/coseno-tfidf").param("articuloA", "15").param("articuloB", "19"))
                .andReturn().getResponse();
        assertEquals(500, respuesta.getStatus());
        assertFalse(respuesta.getContentAsString().contains("detalle interno"));
    }
}
