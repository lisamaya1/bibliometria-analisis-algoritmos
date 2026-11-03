package com.uniquindio.bibliometria.algoritmos.similitud.clasicos;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Sørensen-Dice sobre conjuntos de tokens ya procesados. */
public final class Dice {
    public record Resultado(List<String> conjuntoA, List<String> conjuntoB,
                            List<String> interseccion, int cardinalidadA, int cardinalidadB,
                            int cardinalidadInterseccion, double similitud) {
        public Resultado {
            conjuntoA = List.copyOf(conjuntoA);
            conjuntoB = List.copyOf(conjuntoB);
            interseccion = List.copyOf(interseccion);
        }
    }

    public Resultado comparar(List<String> izquierda, List<String> derecha) {
        Set<String> a = unicos(izquierda);
        Set<String> b = unicos(derecha);
        Set<String> interseccion = new LinkedHashSet<>();
        for (String palabra : a) {
            if (b.contains(palabra)) interseccion.add(palabra);
        }
        double denominador = (double) a.size() + b.size();
        if (denominador == 0) {
            throw new IllegalArgumentException("Dice no está definido para dos conjuntos vacíos (0/0).");
        }
        double similitud = 2.0 * interseccion.size() / denominador;
        return new Resultado(List.copyOf(a), List.copyOf(b), List.copyOf(interseccion),
                a.size(), b.size(), interseccion.size(), similitud);
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
