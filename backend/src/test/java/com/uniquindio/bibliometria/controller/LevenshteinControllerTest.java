package com.uniquindio.bibliometria.controller;

import com.uniquindio.bibliometria.service.LevenshteinService;
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

class LevenshteinControllerTest {
    @Test void exponeElCasoRealComoJsonYRechazaParametrosInvalidos() throws Exception {
        var corpus = JsonUtil.cargarArticulosDesdeJson(PreprocesamientoService.RUTA_JSON_DEFECTO);
        var lector = new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path pdfs, Path json) { return corpus; }
        };
        var mvc = MockMvcBuilders.standaloneSetup(new LevenshteinController(new LevenshteinService(lector))).build();
        var respuesta = mvc.perform(get("/api/levenshtein").param("articuloA", "15").param("articuloB", "19"))
                .andReturn().getResponse();
        assertEquals(200, respuesta.getStatus());
        var json = new JsonMapper().readTree(respuesta.getContentAsByteArray());
        assertEquals(1476, json.get("resultado").get("longitudA").asInt());
        assertEquals(1425, json.get("resultado").get("longitudB").asInt());
        assertEquals(1123, json.get("resultado").get("distancia").asInt());
        assertEquals(1.0 - 1123.0 / 1476, json.get("resultado").get("similitud").asDouble(), 1e-12);
        assertEquals(100 * (1.0 - 1123.0 / 1476), json.get("porcentaje").asDouble(), 1e-10);
        assertFalse(respuesta.getContentAsString().contains("matriz"));
        assertEquals(400, mvc.perform(get("/api/levenshtein").param("articuloA", "0").param("articuloB", "19"))
                .andReturn().getResponse().getStatus());
        assertEquals(400, mvc.perform(get("/api/levenshtein").param("articuloA", "15").param("articuloB", "99"))
                .andReturn().getResponse().getStatus());
    }

    @Test void requiereAmbosIdentificadoresNumericos() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new LevenshteinController(new LevenshteinService(new PreprocesamientoService()))).build();
        assertEquals(400, mvc.perform(get("/api/levenshtein").param("articuloA", "15"))
                .andReturn().getResponse().getStatus());
        assertEquals(400, mvc.perform(get("/api/levenshtein").param("articuloA", "abc").param("articuloB", "19"))
                .andReturn().getResponse().getStatus());
    }

    @Test void informaErrorDeLecturaSinExponerDetallesInternos() throws Exception {
        var lector = new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path pdfs, Path json) throws IOException {
                throw new IOException("detalle interno");
            }
        };
        var mvc = MockMvcBuilders.standaloneSetup(new LevenshteinController(new LevenshteinService(lector))).build();
        var respuesta = mvc.perform(get("/api/levenshtein").param("articuloA", "15").param("articuloB", "19"))
                .andReturn().getResponse();
        assertEquals(500, respuesta.getStatus());
        assertFalse(respuesta.getContentAsString().contains("detalle interno"));
    }
}
