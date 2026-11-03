package com.uniquindio.bibliometria.algoritmos.busqueda;

/** Contrato para buscar todas las ocurrencias, incluidas las superpuestas. */
public interface BusquedaExacta {
    ResultadoBusqueda buscar(String texto, String patron);
}
