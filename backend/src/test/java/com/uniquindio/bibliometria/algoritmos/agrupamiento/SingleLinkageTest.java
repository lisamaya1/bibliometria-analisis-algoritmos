package com.uniquindio.bibliometria.algoritmos.agrupamiento;

import com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace.SingleLinkage;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SingleLinkageTest {
    private final SingleLinkage algoritmo = new SingleLinkage();
    private MatrizDistancias ejemplo() {
        return new MatrizDistancias(List.of(1, 2, 3, 4), new double[][]{
                {0, .1, .6, .8}, {.1, 0, .3, .7}, {.6, .3, 0, .4}, {.8, .7, .4, 0}});
    }
    @Test void ejemploControladoPasoAPaso() {
        var r = algoritmo.agrupar(ejemplo());
        assertEquals(3, r.fusiones().size());
        var primera = r.fusiones().get(0);
        assertEquals(new SingleLinkage.Fusion(5, 1, 2, List.of(1, 2), .1, 2), primera);
        var c12 = Cluster.fusionar(5, Cluster.hoja(1), Cluster.hoja(2), .1);
        assertEquals(.3, algoritmo.distancia(c12, Cluster.hoja(3), ejemplo()));
        assertEquals(.7, algoritmo.distancia(c12, Cluster.hoja(4), ejemplo()));
        assertEquals(new SingleLinkage.Fusion(6, 3, 5, List.of(3, 1, 2), .3, 3), r.fusiones().get(1));
        assertEquals(new SingleLinkage.Fusion(7, 4, 6, List.of(4, 3, 1, 2), .4, 4), r.fusiones().get(2));
        assertEquals(4, r.raiz().tamano());
        assertEquals(4, r.raiz().izquierdo().id());
        assertEquals(6, r.raiz().derecho().id());
    }
    @Test void desempateLexicograficoIndependienteDelOrdenDeLaMatriz() {
        double[][] iguales = {{0, .5, .5, .5}, {.5, 0, .5, .5}, {.5, .5, 0, .5}, {.5, .5, .5, 0}};
        var a = algoritmo.agrupar(new MatrizDistancias(List.of(1, 2, 3, 4), iguales));
        var b = algoritmo.agrupar(new MatrizDistancias(List.of(4, 2, 1, 3), iguales));
        assertEquals(a, b);
        assertEquals(1, a.fusiones().get(0).izquierdo());
        assertEquals(2, a.fusiones().get(0).derecho());
        assertEquals(3, a.fusiones().get(1).izquierdo());
        assertEquals(4, a.fusiones().get(1).derecho());
    }
    @Test void admiteDistanciasCeroEntreArticulosDistintos() {
        var r = algoritmo.agrupar(new MatrizDistancias(List.of(15, 19), new double[][]{{0, 0}, {0, 0}}));
        assertEquals(1, r.fusiones().size());
        assertEquals(20, r.raiz().id());
        assertEquals(0, r.raiz().distancia());
    }
    @Test void estructuraInmutableYClusterValido() {
        var r = algoritmo.agrupar(ejemplo());
        assertThrows(UnsupportedOperationException.class, () -> r.raiz().articulos().clear());
        assertThrows(UnsupportedOperationException.class, () -> r.fusiones().clear());
        assertThrows(IllegalArgumentException.class, () -> Cluster.fusionar(5, Cluster.hoja(1), Cluster.hoja(1), .1));
        assertThrows(IllegalArgumentException.class, () -> algoritmo.agrupar(null));
    }
    @Test void idsNuevosNoColisionanConArticulos() {
        var r = algoritmo.agrupar(new MatrizDistancias(List.of(50, 100), new double[][]{{0, .2}, {.2, 0}}));
        assertEquals(101, r.raiz().id());
        assertEquals(List.of(50, 100), r.raiz().articulos());
    }
}
