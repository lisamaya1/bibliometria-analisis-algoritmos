package com.uniquindio.bibliometria.controller;

import com.uniquindio.bibliometria.service.ComparacionBusquedaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class ComparacionBusquedaControllerTest {
    @TempDir Path temporal;
    @Test void respuestaRealContieneLosTresResultadosConPosicionesIguales() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new ComparacionBusquedaController(new ComparacionBusquedaService())).build();
        var respuesta=mvc.perform(get("/api/busqueda/comparar").param("articulo","19").param("patron","chatgpt")).andReturn().getResponse();
        assertEquals(200,respuesta.getStatus());
        var json=new JsonMapper().readTree(respuesta.getContentAsByteArray());
        var resultados=json.get("resultados");
        assertEquals(3,resultados.size());
        assertEquals(resultados.get(0).get("posiciones"),resultados.get(1).get("posiciones"));
        assertEquals(resultados.get(0).get("posiciones"),resultados.get(2).get("posiciones"));
        assertEquals(7,json.get("actualizacionesTablaBadCharacter").asInt());
    }
    @Test void parametrosInvalidosDevuelven400() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new ComparacionBusquedaController(new ComparacionBusquedaService())).build();
        assertEquals(400,mvc.perform(get("/api/busqueda/comparar").param("articulo","99").param("patron","a")).andReturn().getResponse().getStatus());
        assertEquals(400,mvc.perform(get("/api/busqueda/comparar").param("articulo","19").param("patron","")).andReturn().getResponse().getStatus());
        assertEquals(400,mvc.perform(get("/api/busqueda/comparar")).andReturn().getResponse().getStatus());
    }
    @Test void jsonAusenteDevuelve500SinRegeneracion() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new ComparacionBusquedaController(new ComparacionBusquedaService(temporal.resolve("ausente.json")))).build();
        assertEquals(500,mvc.perform(get("/api/busqueda/comparar").param("articulo","19").param("patron","a")).andReturn().getResponse().getStatus());
    }
}
