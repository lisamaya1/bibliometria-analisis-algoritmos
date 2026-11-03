package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.algoritmos.agrupamiento.MatrizDistancias;
import com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace.CompleteLinkage;
import com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace.SingleLinkage;
import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.util.JsonUtil;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class CompleteClusteringServiceTest {
    @Test void corpusRealHistorialCompletoReproducibleSinDuplicadosYConMaximosCorrectos() throws Exception {
        byte[] antes = Files.readAllBytes(PreprocesamientoService.RUTA_JSON_DEFECTO);
        var corpus = JsonUtil.cargarArticulosDesdeJson(PreprocesamientoService.RUTA_JSON_DEFECTO);
        int[] lecturas = {0};
        var servicio = new ClusteringService(new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path a, Path b) { lecturas[0]++; return corpus; }
        });
        var r = servicio.complete();
        assertEquals(1, lecturas[0]);
        assertEquals("complete", r.criterio());
        assertEquals(20, r.cantidadArticulos());
        assertEquals(19, r.fusiones().size());
        assertEquals(39, r.raiz());
        var activos = new HashMap<Integer, Set<Integer>>();
        for (var a : corpus) activos.put(a.id(), Set.of(a.id()));
        var matriz = MatrizDistancias.desdeCorpus(corpus);
        assertEquals(new CompleteLinkage().agrupar(matriz).fusiones(), r.fusiones());
        double anterior = 0;
        for (var f : r.fusiones()) {
            var izquierda = activos.remove(f.izquierdo());
            var derecha = activos.remove(f.derecho());
            assertNotNull(izquierda);
            assertNotNull(derecha);
            var miembros = new HashSet<>(izquierda);
            for (int id : derecha) assertTrue(miembros.add(id));
            assertEquals(miembros, new HashSet<>(f.articulos()));
            assertEquals(miembros.size(), f.tamano());
            assertEquals(f.tamano(), f.articulos().size());
            double maximo = 0;
            for (int a : izquierda) for (int b : derecha) maximo = Math.max(maximo, matriz.distancia(a, b));
            assertEquals(maximo, f.distancia());
            assertTrue(f.distancia() >= anterior);
            assertTrue(f.izquierdo() < f.derecho());
            anterior = f.distancia();
            activos.put(f.id(), miembros);
        }
        assertEquals(1, activos.size());
        assertEquals(20, activos.get(r.raiz()).size());
        assertEquals(r, servicio.complete());
        assertArrayEquals(antes, Files.readAllBytes(PreprocesamientoService.RUTA_JSON_DEFECTO));
    }
    @Test void singleConservaTodasLasParejasYDistanciasValidadasAntesDeLaExtraccion() throws Exception {
        var corpus = JsonUtil.cargarArticulosDesdeJson(PreprocesamientoService.RUTA_JSON_DEFECTO);
        var r = new SingleLinkage().agrupar(MatrizDistancias.desdeCorpus(corpus));
        int[][] hijos = {{2,6},{1,11},{12,20},{5,21},{9,24},{10,25},{23,26},{8,27},{17,19},{28,29},
                {22,30},{16,31},{15,32},{4,33},{18,34},{7,35},{14,36},{13,37},{3,38}};
        double[] distancias = {.8324093325358022,.8476840653718773,.8578908916091218,.8603436430117323,
                .8624168595720059,.8810203707246097,.8833965693734883,.8841211362227599,.9015671227962578,
                .9047059028272006,.9059327765055991,.9073028875060953,.9169238646755918,.9237635408890643,
                .926223955910241,.9264799162444617,.9282443559025819,.9307330007281855,.9327793907732033};
        assertEquals(19, r.fusiones().size());
        for (int i = 0; i < hijos.length; i++) {
            var f = r.fusiones().get(i);
            assertEquals(21+i, f.id());
            assertEquals(hijos[i][0], f.izquierdo());
            assertEquals(hijos[i][1], f.derecho());
            assertEquals(distancias[i], f.distancia(), 1e-14);
        }
    }
}
