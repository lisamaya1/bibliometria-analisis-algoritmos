package com.uniquindio.bibliometria.controller;

import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.service.ClusteringService;
import com.uniquindio.bibliometria.service.PreprocesamientoService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class EvaluacionClusteringControllerTest {
    @Test void endpointRealDevuelveHistorialReconstruibleSinMatriz() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new ClusteringController(new ClusteringService(new PreprocesamientoService()))).build();
        var respuesta = mvc.perform(get("/api/clustering/evaluacion")).andReturn().getResponse();
        assertEquals(200, respuesta.getStatus());
        var json = new JsonMapper().readTree(respuesta.getContentAsByteArray());
        assertEquals(20, json.get("cantidadArticulos").asInt());
        assertEquals(3, json.get("criterios").size());
        assertEquals(18, json.get("criterios").get(0).get("resultados").size());
        assertTrue(json.get("criteriosMasCoherentes").size() >= 1);
        assertFalse(json.has("matriz"));
        assertFalse(respuesta.getContentAsString().contains("promediosExternos"));
    }
    @Test void corpusInvalidoDevuelve400() throws Exception {
        var lector = new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path a, Path b) { return List.of(); }
        };
        var mvc = MockMvcBuilders.standaloneSetup(new ClusteringController(new ClusteringService(lector))).build();
        assertEquals(400, mvc.perform(get("/api/clustering/evaluacion")).andReturn().getResponse().getStatus());
    }
    @Test void lecturaFallidaDevuelve500SinDetallesInternos() throws Exception {
        var lector = new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path a, Path b) throws IOException { throw new IOException("privado"); }
        };
        var mvc = MockMvcBuilders.standaloneSetup(new ClusteringController(new ClusteringService(lector))).build();
        var respuesta = mvc.perform(get("/api/clustering/evaluacion")).andReturn().getResponse();
        assertEquals(500, respuesta.getStatus());
        assertFalse(respuesta.getContentAsString().contains("privado"));
    }
}
