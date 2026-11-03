package com.uniquindio.bibliometria.algoritmos.agrupamiento;

import com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace.AverageLinkage;
import com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace.SingleLinkage;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AverageLinkageTest {
    private final AverageLinkage algoritmo = new AverageLinkage();
    private MatrizDistancias ejemplo() {
        return new MatrizDistancias(List.of(1, 2, 3, 4), new double[][]{
                {0, .1, .6, .8}, {.1, 0, .3, .7}, {.6, .3, 0, .4}, {.8, .7, .4, 0}});
    }
    @Test void promedioExplicitoSimetricoEntreMiembros() {
        var c12 = Cluster.fusionar(5, Cluster.hoja(1), Cluster.hoja(2), .1);
        assertEquals(.45, algoritmo.distancia(c12, Cluster.hoja(3), ejemplo()), 1e-12);
        assertEquals(.75, algoritmo.distancia(c12, Cluster.hoja(4), ejemplo()), 1e-12);
        var c34 = Cluster.fusionar(6, Cluster.hoja(3), Cluster.hoja(4), .4);
        assertEquals(.6, algoritmo.distancia(c12, c34, ejemplo()), 1e-12);
        assertEquals(algoritmo.distancia(c12, c34, ejemplo()), algoritmo.distancia(c34, c12, ejemplo()));
    }
    @Test void ponderaTodasLasParejasConClustersDeDistintosTamanos() {
        var m = new MatrizDistancias(List.of(1, 2, 3, 4), new double[][]{
                {0,.1,.2,.2},{.1,0,.3,.4},{.2,.3,0,.9},{.2,.4,.9,0}});
        var c12 = Cluster.fusionar(5, Cluster.hoja(1), Cluster.hoja(2), .1);
        var c123 = Cluster.fusionar(6, Cluster.hoja(3), c12, .25);
        // Promedia los tres pares (.2+.4+.9)/3=.5, no promedios de hijos sin ponderar (.6).
        assertEquals(.5, algoritmo.distancia(c123, Cluster.hoja(4), m), 1e-12);
    }
    @Test void casoPequenoCompleto() {
        var r = algoritmo.agrupar(ejemplo());
        assertEquals(3, r.fusiones().size());
        int[][] hijos = {{1,2},{3,4},{5,6}};
        double[] distancias = {.1,.4,.6};
        for (int i=0; i<3; i++) {
            assertEquals(5+i, r.fusiones().get(i).id());
            assertEquals(hijos[i][0], r.fusiones().get(i).izquierdo());
            assertEquals(hijos[i][1], r.fusiones().get(i).derecho());
            assertEquals(distancias[i], r.fusiones().get(i).distancia(), 1e-12);
        }
        assertEquals(List.of(1,2,3,4), r.raiz().articulos());
        assertEquals(4, r.raiz().tamano());
    }
    @Test void empatesYOrdenDeEntradaRespetanLaReglaComun() {
        double[][] valores = {{0,.5,.5,.5},{.5,0,.5,.5},{.5,.5,0,.5},{.5,.5,.5,0}};
        var m = new MatrizDistancias(List.of(1,2,3,4), valores);
        assertEquals(new SingleLinkage().agrupar(m), algoritmo.agrupar(m));
        assertEquals(algoritmo.agrupar(m), algoritmo.agrupar(new MatrizDistancias(List.of(4,2,1,3), valores)));
    }
    @Test void conservaMatrizAdmiteCeroYValidaEntradaComun() {
        var m = ejemplo();
        algoritmo.agrupar(m);
        assertEquals(.6, m.distancia(1,3));
        assertEquals(.3, m.distancia(2,3));
        var r = algoritmo.agrupar(new MatrizDistancias(List.of(50,100), new double[][]{{0,0},{0,0}}));
        assertEquals(101,r.raiz().id());
        assertEquals(0,r.raiz().distancia());
        assertThrows(IllegalArgumentException.class, () -> algoritmo.agrupar(null));
    }
}
