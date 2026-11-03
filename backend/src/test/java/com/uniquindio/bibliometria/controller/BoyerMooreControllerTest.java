package com.uniquindio.bibliometria.controller;

import com.uniquindio.bibliometria.service.BusquedaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class BoyerMooreControllerTest {
    @TempDir Path temporal;
    @Test void endpointRealEntregaPosicionesContadoresYUnidades() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new BusquedaController(new BusquedaService())).build();
        var respuesta=mvc.perform(get("/api/busqueda/boyer-moore").param("articulo","19").param("patron","chatgpt")).andReturn().getResponse();
        assertEquals(200,respuesta.getStatus());
        var json=new JsonMapper().readTree(respuesta.getContentAsByteArray());
        assertEquals(19,json.get("articulo").get("id").asInt());
        assertEquals("abstractProcesado",json.get("fuenteTexto").asString());
        var r=json.get("resultado");
        assertEquals("boyer-moore",r.get("algoritmo").asString());
        assertEquals("chatgpt",r.get("patron").asString());
        assertEquals(r.get("posiciones").size(),r.get("ocurrencias").asInt());
        assertTrue(r.get("encontrado").asBoolean());
        assertTrue(r.get("comparaciones").asLong()>0);
        assertTrue(r.get("tiempoBusquedaNs").asLong()>=0);
        assertFalse(respuesta.getContentAsString().contains("abstractOriginal"));
    }
    @Test void erroresDeParametrosDevuelven400() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new BusquedaController(new BusquedaService())).build();
        assertEquals(400,mvc.perform(get("/api/busqueda/boyer-moore").param("articulo","99").param("patron","a")).andReturn().getResponse().getStatus());
        assertEquals(400,mvc.perform(get("/api/busqueda/boyer-moore").param("articulo","19").param("patron","")).andReturn().getResponse().getStatus());
        assertEquals(400,mvc.perform(get("/api/busqueda/boyer-moore").param("articulo","abc").param("patron","a")).andReturn().getResponse().getStatus());
        assertEquals(400,mvc.perform(get("/api/busqueda/boyer-moore")).andReturn().getResponse().getStatus());
    }
    @Test void faltaDeJsonDevuelve500SinRegenerar() throws Exception {
        var servicio=new BusquedaService(temporal.resolve("ausente.json"));
        var mvc=MockMvcBuilders.standaloneSetup(new BusquedaController(servicio)).build();
        var respuesta=mvc.perform(get("/api/busqueda/boyer-moore").param("articulo","19").param("patron","a")).andReturn().getResponse();
        assertEquals(500,respuesta.getStatus());
        assertTrue(respuesta.getContentAsString().contains("JSON"));
    }
}
