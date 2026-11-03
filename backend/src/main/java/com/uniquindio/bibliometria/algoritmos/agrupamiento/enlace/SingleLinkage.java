package com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace;

import com.uniquindio.bibliometria.algoritmos.agrupamiento.Cluster;
import com.uniquindio.bibliometria.algoritmos.agrupamiento.MatrizDistancias;

/** Conserva el criterio minimo y utiliza el procedimiento aglomerativo comun. */
public final class SingleLinkage extends AglomeradorJerarquico {
    @Override
    public double distancia(Cluster a, Cluster b, MatrizDistancias matriz) {
        double minima = Double.POSITIVE_INFINITY;
        for (int x : a.articulos())
            for (int y : b.articulos()) {
                double d = matriz.distancia(x, y);
                if (d < minima) minima = d;
            }
        return minima;
    }

}
