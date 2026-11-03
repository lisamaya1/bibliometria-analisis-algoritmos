package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.algoritmos.busqueda.KmpTest;
import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.util.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RabinKarpServiceTest {
    @TempDir Path temporal;
    @Test void casoComunYRegresionDeLosDosAlgoritmosAnteriores() throws Exception {
        var servicio=new BusquedaService();
        var rk=servicio.buscarRabinKarp(19,"chatgpt").resultado();
        var kmp=servicio.buscarKmp(19,"chatgpt").resultado();
        var bm=servicio.buscarBoyerMoore(19,"chatgpt").resultado();
        assertEquals(List.of(42,243,644,825,1386),rk.posiciones());
        assertEquals(kmp.posiciones(),rk.posiciones());
        assertEquals(bm.posiciones(),rk.posiciones());
        assertEquals(1471,kmp.comparaciones());
        assertEquals(6,kmp.comparacionesPreparacion());
        assertEquals(275,bm.comparaciones());
        assertEquals(0,bm.comparacionesPreparacion());
        assertNull(kmp.estadisticasHash());
        assertNull(bm.estadisticasHash());
        assertEquals(35,rk.comparaciones());
        assertEquals(1419,rk.estadisticasHash().ventanasProcesadas());
    }
    @Test void usaSoloAbstractProcesadoSinRetokenizarNiCambiarPatron() throws Exception {
        Path json=temporal.resolve("corpus.json");
        JsonUtil.guardarArticulosEnJson(List.of(new ArticuloDocumental(19,"a.pdf","Artículo A",List.of(),
                "Original ChatGPT", "aaaa", List.of("otros","tokens"),2)),json);
        var servicio=new BusquedaService(json);
        var r=servicio.buscarRabinKarp(19,"aa");
        assertEquals(List.of(0,1,2),r.resultado().posiciones());
        assertEquals("abstractProcesado",r.fuenteTexto());
        assertEquals("aa",r.resultado().patron());
        assertFalse(servicio.buscarRabinKarp(19,"ChatGPT").resultado().encontrado());
    }
    @Test void corpusRealChatgptPosicionesVerificadasYJsonIntacto() throws Exception {
        byte[] antes=Files.readAllBytes(PreprocesamientoService.RUTA_JSON_DEFECTO);
        var r=new BusquedaService().buscarRabinKarp(19,"chatgpt");
        var a=JsonUtil.cargarArticulosDesdeJson(PreprocesamientoService.RUTA_JSON_DEFECTO).stream().filter(x->x.id()==19).findFirst().orElseThrow();
        assertEquals(KmpTest.referencia(a.abstractProcesado(),"chatgpt"),r.resultado().posiciones());
        assertTrue(r.resultado().encontrado());
        assertEquals(1425,r.resultado().longitudTexto());
        assertArrayEquals(antes,Files.readAllBytes(PreprocesamientoService.RUTA_JSON_DEFECTO));
    }
    @Test void validaIdPatronYArticuloInexistente() {
        var servicio=new BusquedaService();
        assertThrows(IllegalArgumentException.class,()->servicio.buscarRabinKarp(0,"a"));
        assertThrows(IllegalArgumentException.class,()->servicio.buscarRabinKarp(19,""));
        assertThrows(IllegalArgumentException.class,()->servicio.buscarRabinKarp(99,"chatgpt"));
    }
    @Test void jsonAusenteNoSeRegeneraNiSeLeenPdfs() {
        Path ausente=temporal.resolve("no-existe.json");
        assertThrows(IOException.class,()->new BusquedaService(ausente).buscarRabinKarp(19,"a"));
        assertFalse(Files.exists(ausente));
    }
}
