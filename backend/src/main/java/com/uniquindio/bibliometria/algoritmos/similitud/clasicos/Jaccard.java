package com.uniquindio.bibliometria.algoritmos.similitud.clasicos;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Construye conjuntos, intersección y unión mediante recorridos explícitos. */
public final class Jaccard {
    public record Resultado(List<String> conjuntoA, List<String> conjuntoB,
                            List<String> interseccion, List<String> union, double similitud) {
        public Resultado {
            conjuntoA = List.copyOf(conjuntoA);
            conjuntoB = List.copyOf(conjuntoB);
            interseccion = List.copyOf(interseccion);
            union = List.copyOf(union);
        }
    }

    /** Recibe tokens ya procesados; no limpia, filtra ni modifica las entradas. */
    public Resultado comparar(List<String> izquierda, List<String> derecha) {
        Set<String> a = unicos(izquierda);
        Set<String> b = unicos(derecha);
        Set<String> interseccion = new LinkedHashSet<>();
        Set<String> union = new LinkedHashSet<>();
        for (String palabra : a) {
            union.add(palabra);
            if (b.contains(palabra)) interseccion.add(palabra);
        }
        for (String palabra : b) union.add(palabra);
        if (union.isEmpty()) {
            throw new IllegalArgumentException("Jaccard no está definido para dos conjuntos vacíos (0/0).");
        }
        double similitud = (double) interseccion.size() / union.size();
        return new Resultado(List.copyOf(a), List.copyOf(b), List.copyOf(interseccion),
                List.copyOf(union), similitud);
    }

    private Set<String> unicos(List<String> palabras) {
        Objects.requireNonNull(palabras, "La lista de palabras no puede ser nula.");
        Set<String> resultado = new LinkedHashSet<>();
        for (String palabra : palabras) {
            Objects.requireNonNull(palabra, "Una palabra no puede ser nula.");
            if (palabra.isBlank()) throw new IllegalArgumentException("No se admiten palabras en blanco.");
            resultado.add(palabra);
        }
        return resultado;
    }
}
