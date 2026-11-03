package com.uniquindio.bibliometria.algoritmos.agrupamiento;

import java.util.ArrayList;
import java.util.List;

/** Nodo inmutable: una hoja o una fusión binaria. Los hijos conservan el árbol. */
public record Cluster(int id, List<Integer> articulos, Cluster izquierdo, Cluster derecho,
                      double distancia, int tamano) {
    public Cluster {
        articulos = List.copyOf(articulos);
        if (id < 1 || articulos.isEmpty() || tamano != articulos.size() ||
                !Double.isFinite(distancia) || distancia < 0 || distancia > 1 ||
                ((izquierdo == null) != (derecho == null)))
            throw new IllegalArgumentException("Cluster inválido.");
        if (izquierdo == null && (tamano != 1 || distancia != 0 || articulos.get(0) != id))
            throw new IllegalArgumentException("Hoja inválida.");
        if (izquierdo != null) {
            var esperados = new ArrayList<>(izquierdo.articulos());
            esperados.addAll(derecho.articulos());
            if (!esperados.equals(articulos) || id <= Math.max(izquierdo.id(), derecho.id()))
                throw new IllegalArgumentException("Los hijos no corresponden a la fusión.");
            if (new java.util.HashSet<>(articulos).size() != tamano)
                throw new IllegalArgumentException("Los clusters fusionados deben ser disjuntos.");
        }
    }

    public static Cluster hoja(int articulo) {
        return new Cluster(articulo, List.of(articulo), null, null, 0, 1);
    }

    public static Cluster fusionar(int id, Cluster izquierdo, Cluster derecho, double distancia) {
        var articulos = new ArrayList<>(izquierdo.articulos());
        articulos.addAll(derecho.articulos());
        return new Cluster(id, articulos, izquierdo, derecho, distancia, articulos.size());
    }
}
