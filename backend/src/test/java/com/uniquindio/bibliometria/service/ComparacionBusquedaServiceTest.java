package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.util.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ComparacionBusquedaServiceTest {
    @TempDir Path temporal;
    @Test void tresCasosRealesYRegresionConElMismoJson() throws Exception {
        byte[] antes=Files.readAllBytes(PreprocesamientoService.RUTA_JSON_DEFECTO);
        var servicio=new ComparacionBusquedaService();
        var individuales=new BusquedaService();
        String[] patrones={"chatgpt","november 2022","quantum entanglement"};
        int[] ocurrencias={5,1,0};
        for (int i=0;i<patrones.length;i++) {
            var r=servicio.comparar(19,patrones[i]);
            for (var algoritmo:r.resultados()) assertEquals(ocurrencias[i],algoritmo.ocurrencias());
            assertEquals(individuales.buscarKmp(19,patrones[i]).resultado().posiciones(),r.resultados().get(0).posiciones());
            assertEquals(individuales.buscarBoyerMoore(19,patrones[i]).resultado().posiciones(),r.resultados().get(1).posiciones());
            assertEquals(individuales.buscarRabinKarp(19,patrones[i]).resultado().posiciones(),r.resultados().get(2).posiciones());
        }
        var principal=servicio.comparar(19,"chatgpt");
        assertEquals(List.of(42,243,644,825,1386),principal.resultados().get(0).posiciones());
        assertEquals(1471,principal.resultados().get(0).comparaciones());
        assertEquals(275,principal.resultados().get(1).comparaciones());
        assertEquals(35,principal.resultados().get(2).comparaciones());
        assertEquals(7,principal.actualizacionesTablaBadCharacter());
        assertArrayEquals(antes,Files.readAllBytes(PreprocesamientoService.RUTA_JSON_DEFECTO));
    }
    @Test void unaEntradaCargadaSeComparaSinVolverALeerJson() throws Exception {
        Path json=temporal.resolve("corpus.json");
        JsonUtil.guardarArticulosEnJson(List.of(new ArticuloDocumental(19,"a.pdf","a",List.of(),"otro","😀aaaa",List.of("distintos"),1)),json);
        var servicio=new ComparacionBusquedaService(json);
        var entrada=servicio.cargarEntrada(19,"aa");
        Files.delete(json);
        var r=servicio.comparar(entrada);
        for (var algoritmo:r.resultados()) assertEquals(List.of(1,2,3),algoritmo.posiciones());
    }
    @Test void validaEntradasYArticuloInexistente() {
        var servicio=new ComparacionBusquedaService();
        assertThrows(IllegalArgumentException.class,()->servicio.comparar(0,"a"));
        assertThrows(IllegalArgumentException.class,()->servicio.comparar(19,""));
        assertThrows(IllegalArgumentException.class,()->servicio.comparar(99,"a"));
    }
}
