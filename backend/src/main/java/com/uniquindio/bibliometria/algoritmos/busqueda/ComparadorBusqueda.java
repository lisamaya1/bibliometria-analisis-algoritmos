package com.uniquindio.bibliometria.algoritmos.busqueda;

import java.util.List;

/** Ejecuta los algoritmos existentes sobre las mismas cadenas y contrasta sus resultados. */
public final class ComparadorBusqueda {
    private final List<BusquedaExacta> algoritmos=List.of(new Kmp(),new BoyerMoore(),new RabinKarp());

    public List<BusquedaExacta> algoritmos() { return algoritmos; }

    public List<ResultadoBusqueda> comparar(String texto,String patron) {
        var resultados=new java.util.ArrayList<ResultadoBusqueda>();
        for (var algoritmo:algoritmos) resultados.add(algoritmo.buscar(texto,patron));
        for (var r:resultados) comprobarIgualdad(resultados.get(0),r);
        return List.copyOf(resultados);
    }

    public static void comprobarIgualdad(ResultadoBusqueda esperado,ResultadoBusqueda actual) {
        if (!esperado.posiciones().equals(actual.posiciones()) || esperado.ocurrencias()!=actual.ocurrencias() ||
                esperado.encontrado()!=actual.encontrado() || esperado.longitudTexto()!=actual.longitudTexto() ||
                esperado.longitudPatron()!=actual.longitudPatron() || !esperado.patron().equals(actual.patron()))
            throw new IllegalStateException("Los algoritmos no coinciden en sus resultados funcionales.");
    }
}
