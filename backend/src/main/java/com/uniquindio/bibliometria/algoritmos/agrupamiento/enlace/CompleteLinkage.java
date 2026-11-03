package com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace;

import com.uniquindio.bibliometria.algoritmos.agrupamiento.Cluster;
import com.uniquindio.bibliometria.algoritmos.agrupamiento.MatrizDistancias;

/** Distancia máxima entre miembros de clusters distintos, calculada explícitamente. */
public final class CompleteLinkage extends AglomeradorJerarquico {
    @Override public double distancia(Cluster a, Cluster b, MatrizDistancias matriz) {
        double maxima = Double.NEGATIVE_INFINITY;
        for (int x : a.articulos())
            for (int y : b.articulos()) {
                double d = matriz.distancia(x, y);
                if (d > maxima) maxima = d;
            }
        return maxima;
    }
}
