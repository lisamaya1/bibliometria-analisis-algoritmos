package com.uniquindio.bibliometria.algoritmos.busqueda;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Longitudes y posiciones en puntos de código Unicode, con índice base cero. */
public record ResultadoBusqueda(String algoritmo, String patron, int longitudTexto, int longitudPatron,
                                List<Integer> posiciones, int ocurrencias, boolean encontrado,
                                long comparaciones, long comparacionesPreparacion,
                                long tiempoPreparacionNs, long tiempoBusquedaNs,
                                @JsonInclude(JsonInclude.Include.NON_NULL) EstadisticasHash estadisticasHash) {
    /** Constructor anterior: conserva el contrato de los algoritmos sin hash. */
    public ResultadoBusqueda(String algoritmo, String patron, int longitudTexto, int longitudPatron,
                             List<Integer> posiciones, int ocurrencias, boolean encontrado,
                             long comparaciones, long comparacionesPreparacion,
                             long tiempoPreparacionNs, long tiempoBusquedaNs) {
        this(algoritmo, patron, longitudTexto, longitudPatron, posiciones, ocurrencias, encontrado,
                comparaciones, comparacionesPreparacion, tiempoPreparacionNs, tiempoBusquedaNs, null);
    }
    public ResultadoBusqueda {
        posiciones = List.copyOf(posiciones);
        if (ocurrencias != posiciones.size() || encontrado != !posiciones.isEmpty())
            throw new IllegalArgumentException("Las ocurrencias deben corresponder a las posiciones.");
    }
}
