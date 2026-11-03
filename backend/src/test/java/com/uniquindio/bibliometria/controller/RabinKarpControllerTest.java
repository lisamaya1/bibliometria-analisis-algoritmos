package com.uniquindio.bibliometria.controller;

import com.uniquindio.bibliometria.service.BusquedaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class RabinKarpControllerTest {
    @TempDir Path temporal;
    @Test void endpointsAnterioresNoExponenCamposDeHashNulos() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new BusquedaController(new BusquedaService())).build();
        for (String algoritmo:java.util.List.of("kmp","boyer-moore")) {
            var respuesta=mvc.perform(get("/api/busqueda/"+algoritmo).param("articulo","19").param("patron","chatgpt")).andReturn().getResponse();
            assertEquals(200,respuesta.getStatus());
            var r=new JsonMapper().readTree(respuesta.getContentAsByteArray()).get("resultado");
            assertFalse(r.has("estadisticasHash"));
            assertEquals(5,r.get("ocurrencias").asInt());
        }
    }
    @Test void endpointRealEntregaPosicionesContadoresYUnidades() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new BusquedaController(new BusquedaService())).build();
        var respuesta=mvc.perform(get("/api/busqueda/rabin-karp").param("articulo","19").param("patron","chatgpt")).andReturn().getResponse();
        assertEquals(200,respuesta.getStatus());
        var json=new JsonMapper().readTree(respuesta.getContentAsByteArray());
        assertEquals(19,json.get("articulo").get("id").asInt());
        assertEquals("abstractProcesado",json.get("fuenteTexto").asString());
        var r=json.get("resultado");
        assertEquals("rabin-karp",r.get("algoritmo").asString());
        assertEquals("chatgpt",r.get("patron").asString());
        assertEquals(r.get("posiciones").size(),r.get("ocurrencias").asInt());
        assertTrue(r.get("encontrado").asBoolean());
        assertTrue(r.get("comparaciones").asLong()>0);
        assertTrue(r.get("tiempoBusquedaNs").asLong()>=0);
        assertFalse(respuesta.getContentAsString().contains("abstractOriginal"));
        assertEquals(1419,r.get("estadisticasHash").get("ventanasProcesadas").asInt());
        assertEquals(5,r.get("estadisticasHash").get("coincidenciasHash").asInt());
        assertEquals(0,r.get("estadisticasHash").get("colisiones").asInt());
    }
    @Test void erroresDeParametrosDevuelven400() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new BusquedaController(new BusquedaService())).build();
        assertEquals(400,mvc.perform(get("/api/busqueda/rabin-karp").param("articulo","99").param("patron","a")).andReturn().getResponse().getStatus());
        assertEquals(400,mvc.perform(get("/api/busqueda/rabin-karp").param("articulo","19").param("patron","")).andReturn().getResponse().getStatus());
        assertEquals(400,mvc.perform(get("/api/busqueda/rabin-karp").param("articulo","abc").param("patron","a")).andReturn().getResponse().getStatus());
        assertEquals(400,mvc.perform(get("/api/busqueda/rabin-karp")).andReturn().getResponse().getStatus());
    }
    @Test void faltaDeJsonDevuelve500SinRegenerar() throws Exception {
        var servicio=new BusquedaService(temporal.resolve("ausente.json"));
        var mvc=MockMvcBuilders.standaloneSetup(new BusquedaController(servicio)).build();
        var respuesta=mvc.perform(get("/api/busqueda/rabin-karp").param("articulo","19").param("patron","a")).andReturn().getResponse();
        assertEquals(500,respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("JSON"));
    }
}
