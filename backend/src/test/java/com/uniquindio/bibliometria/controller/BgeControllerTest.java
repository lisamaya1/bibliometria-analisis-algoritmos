package com.uniquindio.bibliometria.controller;

import com.uniquindio.bibliometria.algoritmos.similitud.ia.ModeloBgeOnnx;
import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.service.BgeService;
import com.uniquindio.bibliometria.service.BgeServiceTest;
import com.uniquindio.bibliometria.service.PreprocesamientoService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class BgeControllerTest {
    @Test void respuestaHttpResumidaYErroresDeParametros() throws Exception {
        var lector = new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path a, Path b) { return BgeServiceTest.corpus(); }
        };
        var servicio = new BgeService(lector, BgeServiceTest.modelo(new ArrayList<>()));
        var mvc = MockMvcBuilders.standaloneSetup(new BgeController(servicio)).build();
        var respuesta = mvc.perform(get("/api/ia/bge").param("articuloA", "15").param("articuloB", "19")).andReturn().getResponse();
        assertEquals(200, respuesta.getStatus());
        var json = new JsonMapper().readTree(respuesta.getContentAsByteArray());
        assertEquals(ModeloBgeOnnx.MODELO, json.get("modelo").asString());
        assertEquals(1, json.get("articuloA").get("tokensContenido").asInt());
        assertEquals(Math.sqrt(2), json.get("resultado").get("distancia").asDouble(), 1e-12);
        assertFalse(json.has("porcentaje"));
        assertFalse(respuesta.getContentAsString().contains("vector"));
        assertEquals(400, mvc.perform(get("/api/ia/bge").param("articuloA", "15").param("articuloB", "99")).andReturn().getResponse().getStatus());
        assertEquals(400, mvc.perform(get("/api/ia/bge").param("articuloA", "abc").param("articuloB", "19")).andReturn().getResponse().getStatus());
        assertEquals(400, mvc.perform(get("/api/ia/bge")).andReturn().getResponse().getStatus());
    }
    @Test void sinConfiguracionDelModeloDevuelve503() throws Exception {
        var lector = new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path a, Path b) { return BgeServiceTest.corpus(); }
        };
        try (var modelo = new ModeloBgeOnnx("")) {
            var mvc = MockMvcBuilders.standaloneSetup(new BgeController(new BgeService(lector, modelo))).build();
            var respuesta = mvc.perform(get("/api/ia/bge").param("articuloA", "15").param("articuloB", "19")).andReturn().getResponse();
            assertEquals(503, respuesta.getStatus());
            assertTrue(respuesta.getContentAsString().contains("BGE_MODEL_DIR"));
        }
    }
    @Test void errorDeLecturaDevuelve500() throws Exception {
        var lector = new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path a, Path b) throws IOException { throw new IOException("privado"); }
        };
        var mvc = MockMvcBuilders.standaloneSetup(new BgeController(new BgeService(lector, BgeServiceTest.modelo(new ArrayList<>())))).build();
        var respuesta = mvc.perform(get("/api/ia/bge").param("articuloA", "15").param("articuloB", "19")).andReturn().getResponse();
        assertEquals(500, respuesta.getStatus());
        assertFalse(respuesta.getContentAsString().contains("privado"));
    }
}
