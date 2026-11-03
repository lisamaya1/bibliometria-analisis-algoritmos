package com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace;

import com.uniquindio.bibliometria.algoritmos.agrupamiento.Cluster;
import com.uniquindio.bibliometria.algoritmos.agrupamiento.MatrizDistancias;

/** Promedio de todas las parejas de artículos entre dos clusters (UPGMA). */
public final class AverageLinkage extends AglomeradorJerarquico {
    @Override public double distancia(Cluster a, Cluster b, MatrizDistancias matriz) {
        // Mantiene el mismo orden de sumas al consultar la pareja invertida.
        Cluster primero = a.id() < b.id() ? a : b;
        Cluster segundo = a.id() < b.id() ? b : a;
        double suma = 0;
        for (int x : primero.articulos())
            for (int y : segundo.articulos())
                suma += matriz.distancia(x, y);
        long parejas = (long) primero.tamano() * segundo.tamano();
        return suma / parejas;
    }
}
