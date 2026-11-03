package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.algoritmos.agrupamiento.*;
import com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace.*;
import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.util.JsonUtil;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EvaluacionClusteringServiceTest {
    @Test void corpusRealEvalua54ParticionesConUnaCargaYReproduceCadaResultado() throws Exception {
        byte[] antes=Files.readAllBytes(PreprocesamientoService.RUTA_JSON_DEFECTO);
        var corpus=JsonUtil.cargarArticulosDesdeJson(PreprocesamientoService.RUTA_JSON_DEFECTO);
        int[] lecturas={0};
        var servicio=new ClusteringService(new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path a,Path b) {lecturas[0]++;return corpus;}
        });
        var r=servicio.evaluar();
        assertEquals(1,lecturas[0]);
        assertEquals(20,r.cantidadArticulos());
        var matriz=MatrizDistancias.desdeCorpus(corpus);
        AglomeradorJerarquico[] algoritmos={new SingleLinkage(),new CompleteLinkage(),new AverageLinkage()};
        for (int i=0;i<3;i++) {
            var c=r.criterios().get(i);
            assertEquals(18,c.resultados().size());
            var historial=algoritmos[i].agrupar(matriz).fusiones();
            for (var k:c.resultados()) {
                assertTrue(k.k()>=2 && k.k()<=19);
                var grupos=Particiones.cortar(matriz.articulos(),historial,k.k());
                assertEquals(k.k(),grupos.size());
                assertEquals(20,k.tamanos().stream().mapToInt(Integer::intValue).sum());
                var s=new Silhouette().calcular(matriz,grupos);
                assertEquals(s.global(),k.silhouette());
                for (var individual:s.individuales()) assertTrue(individual.silhouette()>=-1 && individual.silhouette()<=1);
            }
            var esperado=new EvaluadorClustering().elegirMejor(c.resultados());
            assertEquals(esperado.k(),c.mejor().k());
            assertEquals(esperado.silhouette(),c.mejor().silhouette());
            assertEquals(Particiones.cortar(matriz.articulos(),historial,esperado.k()),c.mejor().clusters());
        }
        assertEquals(r,servicio.evaluar());
        assertArrayEquals(antes,Files.readAllBytes(PreprocesamientoService.RUTA_JSON_DEFECTO));
    }
}
