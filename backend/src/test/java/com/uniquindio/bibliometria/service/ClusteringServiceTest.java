package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.util.JsonUtil;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ClusteringServiceTest {
    @Test void corpusRealDiecinueveFusionesArbolCompletoYReproducible() throws Exception {
        byte[] antes = Files.readAllBytes(PreprocesamientoService.RUTA_JSON_DEFECTO);
        var corpus = JsonUtil.cargarArticulosDesdeJson(PreprocesamientoService.RUTA_JSON_DEFECTO);
        int[] lecturas = {0};
        var servicio = new ClusteringService(new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path a, Path b) { lecturas[0]++; return corpus; }
        });
        var r = servicio.single();
        assertEquals(1, lecturas[0]);
        assertEquals(20, r.cantidadArticulos());
        assertEquals("single", r.criterio());
        assertEquals(19, r.fusiones().size());
        var activos = new HashSet<Integer>();
        for (var a : r.articulos()) activos.add(a.id());
        double anterior = 0;
        for (var f : r.fusiones()) {
            assertTrue(activos.remove(f.izquierdo()));
            assertTrue(activos.remove(f.derecho()));
            assertTrue(activos.add(f.id()));
            assertEquals(f.tamano(), new HashSet<>(f.articulos()).size());
            assertTrue(f.distancia() >= anterior);
            anterior = f.distancia();
        }
        assertEquals(java.util.Set.of(r.raiz()), activos);
        assertEquals(20, r.fusiones().get(18).tamano());
        assertEquals(r, servicio.single());
        assertArrayEquals(antes, Files.readAllBytes(PreprocesamientoService.RUTA_JSON_DEFECTO));
    }
    @Test void corpusInsuficienteSeRechaza() {
        var servicio = new ClusteringService(new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path a, Path b) { return List.of(); }
        });
        assertThrows(IllegalArgumentException.class, servicio::single);
    }
}
