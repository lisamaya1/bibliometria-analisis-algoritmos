package com.uniquindio.bibliometria.algoritmos.agrupamiento;

import com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace.CompleteLinkage;
import com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace.SingleLinkage;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CompleteLinkageTest {
    private final CompleteLinkage algoritmo = new CompleteLinkage();
    private MatrizDistancias ejemplo() {
        return new MatrizDistancias(List.of(1, 2, 3, 4), new double[][]{
                {0, .1, .6, .8}, {.1, 0, .3, .7}, {.6, .3, 0, .4}, {.8, .7, .4, 0}});
    }
    @Test void distanciaEsElMaximoEntreMiembrosNoElMinimo() {
        var matriz = ejemplo();
        var c12 = Cluster.fusionar(5, Cluster.hoja(1), Cluster.hoja(2), .1);
        assertEquals(.6, algoritmo.distancia(c12, Cluster.hoja(3), matriz));
        assertEquals(.8, algoritmo.distancia(c12, Cluster.hoja(4), matriz));
        assertEquals(.6, algoritmo.distancia(Cluster.hoja(3), c12, matriz));
        var c34 = Cluster.fusionar(6, Cluster.hoja(3), Cluster.hoja(4), .4);
        assertEquals(.8, algoritmo.distancia(c12, c34, matriz));
    }
    @Test void casoControladoCompletoFusionaTresYCuatroAntesDeUnirAmbosGrupos() {
        var r = algoritmo.agrupar(ejemplo());
        assertEquals(List.of(
                new CompleteLinkage.Fusion(5, 1, 2, List.of(1, 2), .1, 2),
                new CompleteLinkage.Fusion(6, 3, 4, List.of(3, 4), .4, 2),
                new CompleteLinkage.Fusion(7, 5, 6, List.of(1, 2, 3, 4), .8, 4)), r.fusiones());
        assertEquals(4, r.raiz().tamano());
        assertEquals(List.of(1, 2), r.raiz().izquierdo().articulos());
        assertEquals(List.of(3, 4), r.raiz().derecho().articulos());
    }
    @Test void reglaDeEmpatesEsLaMismaQueSingleYNoDependeDelOrden() {
        double[][] iguales = {{0, .5, .5, .5}, {.5, 0, .5, .5}, {.5, .5, 0, .5}, {.5, .5, .5, 0}};
        var m = new MatrizDistancias(List.of(1, 2, 3, 4), iguales);
        assertEquals(new SingleLinkage().agrupar(m), algoritmo.agrupar(m));
        assertEquals(algoritmo.agrupar(m), algoritmo.agrupar(new MatrizDistancias(List.of(4, 2, 1, 3), iguales)));
    }
    @Test void comparteLaMismaMatrizSinModificarla() {
        var m = ejemplo();
        double[][] antes = new double[4][4];
        for (int i = 1; i <= 4; i++) for (int j = 1; j <= 4; j++) antes[i-1][j-1] = m.distancia(i, j);
        var single = new SingleLinkage().agrupar(m);
        var complete = algoritmo.agrupar(m);
        assertEquals(single.fusiones().get(0), complete.fusiones().get(0));
        assertNotEquals(single.fusiones().get(1), complete.fusiones().get(1));
        for (int i = 1; i <= 4; i++) for (int j = 1; j <= 4; j++) assertEquals(antes[i-1][j-1], m.distancia(i, j));
    }
    @Test void cerosIdsNoConsecutivosYValidacionComun() {
        var r = algoritmo.agrupar(new MatrizDistancias(List.of(50, 100), new double[][]{{0, 0}, {0, 0}}));
        assertEquals(101, r.raiz().id());
        assertEquals(0, r.raiz().distancia());
        assertEquals(1, r.fusiones().size());
        assertThrows(IllegalArgumentException.class, () -> algoritmo.agrupar(null));
    }
}
