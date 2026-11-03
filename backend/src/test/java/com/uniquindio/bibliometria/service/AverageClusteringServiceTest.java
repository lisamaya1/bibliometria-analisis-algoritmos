package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.algoritmos.agrupamiento.MatrizDistancias;
import com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace.AverageLinkage;
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

class AverageClusteringServiceTest {
    @Test void corpusRealHistorialCompletoReproducibleSinDuplicadosYConPromediosCorrectos() throws Exception {
        byte[] antes = Files.readAllBytes(PreprocesamientoService.RUTA_JSON_DEFECTO);
        var corpus = JsonUtil.cargarArticulosDesdeJson(PreprocesamientoService.RUTA_JSON_DEFECTO);
        int[] lecturas = {0};
        var servicio = new ClusteringService(new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path a, Path b) { lecturas[0]++; return corpus; }
        });
        var r = servicio.average();
        assertEquals(1, lecturas[0]);
        assertEquals("average", r.criterio());
        assertEquals(20, r.cantidadArticulos());
        assertEquals(19, r.fusiones().size());
        assertEquals(39, r.raiz());
        var activos = new HashMap<Integer, Set<Integer>>();
        for (var a : corpus) activos.put(a.id(), Set.of(a.id()));
        var matriz = MatrizDistancias.desdeCorpus(corpus);
        assertEquals(new AverageLinkage().agrupar(matriz).fusiones(), r.fusiones());
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
            double suma = 0;
            for (int a : izquierda) for (int b : derecha) suma += matriz.distancia(a, b);
            assertEquals(suma / ((long) izquierda.size() * derecha.size()), f.distancia(), 1e-12);
            assertTrue(f.distancia() >= anterior);
            assertTrue(f.izquierdo() < f.derecho());
            anterior = f.distancia();
            activos.put(f.id(), miembros);
        }
        assertEquals(1, activos.size());
        assertEquals(20, activos.get(r.raiz()).size());
        assertEquals(r, servicio.average());
        assertArrayEquals(antes, Files.readAllBytes(PreprocesamientoService.RUTA_JSON_DEFECTO));
    }
    @Test void completeConservaLasDiecinueveFusionesValidadas() throws Exception {
        var corpus = JsonUtil.cargarArticulosDesdeJson(PreprocesamientoService.RUTA_JSON_DEFECTO);
        var r = new com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace.CompleteLinkage()
                .agrupar(MatrizDistancias.desdeCorpus(corpus));
        int[][] hijos = {{2,6},{1,11},{12,20},{5,21},{8,10},{17,19},{9,24},{15,22},{7,27},
                {13,18},{3,28},{4,23},{25,32},{16,26},{31,33},{30,34},{29,35},{36,37},{14,38}};
        double[] distancias = {.8324093325358022,.8476840653718773,.8578908916091218,.8794346648997046,
                .8841211362227599,.9015671227962578,.9091724685488292,.9295071353001251,.9427044974686034,
                .945918622374066,.9520039400153802,.9538426966107149,.967275126631856,.9687014425623537,
                .9727685541402682,.9741095725813127,.9840599393474511,.9886913702425133,.9911273471622725};
        assertEquals(19, r.fusiones().size());
        for (int i=0; i<19; i++) {
            var f = r.fusiones().get(i);
            assertEquals(21+i,f.id());
            assertEquals(hijos[i][0],f.izquierdo());
            assertEquals(hijos[i][1],f.derecho());
            assertEquals(distancias[i],f.distancia(),1e-14);
        }
    }

    @Test void historialDeLosTresCriteriosPermiteReconstruirCadaParticionK() throws Exception {
        var servicio = new ClusteringService(new PreprocesamientoService());
        for (var r : List.of(servicio.single(), servicio.complete(), servicio.average())) {
            var activos = new HashMap<Integer, Set<Integer>>();
            var todos = new HashSet<Integer>();
            for (var a : r.articulos()) { activos.put(a.id(), Set.of(a.id())); todos.add(a.id()); }
            // Reconstrucción solo en la prueba: no se añade una API de cortes todavía.
            for (int paso=0; paso<=r.fusiones().size(); paso++) {
                int k = r.cantidadArticulos() - paso;
                assertEquals(k,activos.size());
                var union = new HashSet<Integer>();
                for (var miembros : activos.values()) for (int id : miembros) assertTrue(union.add(id));
                assertEquals(todos,union);
                if (paso == r.fusiones().size()) break;
                var f = r.fusiones().get(paso);
                var izquierda = activos.remove(f.izquierdo());
                var derecha = activos.remove(f.derecho());
                assertNotNull(izquierda);
                assertNotNull(derecha);
                var miembros = new HashSet<>(izquierda);
                miembros.addAll(derecha);
                assertEquals(miembros,new HashSet<>(f.articulos()));
                activos.put(f.id(),miembros);
            }
            assertEquals(Set.of(r.raiz()),activos.keySet());
        }
    }
}
