package com.uniquindio.bibliometria.algoritmos.busqueda;

import java.util.ArrayList;
import java.util.List;

/** KMP explícito: no normaliza las entradas ni utiliza funciones de búsqueda. */
public final class Kmp implements BusquedaExacta {
    public record Paso(int indiceTexto, int indicePatron, int caracterTexto, int caracterPatron,
                       boolean iguales, int siguienteTexto, int siguientePatron, Integer posicionEncontrada) { }
    public record Ejecucion(ResultadoBusqueda resultado, List<Integer> lps, List<Paso> pasos) {
        public Ejecucion { lps = List.copyOf(lps); pasos = List.copyOf(pasos); }
    }
    private record Preparacion(int[] lps, long comparaciones) { }

    @Override public ResultadoBusqueda buscar(String texto, String patron) {
        return ejecutar(texto, patron, false).resultado();
    }

    /** Traza opcional del mismo algoritmo para ejemplos pequeños; no se expone por HTTP. */
    public Ejecucion buscarConTraza(String texto, String patron) { return ejecutar(texto, patron, true); }

    public List<Integer> construirLps(String patron) {
        validarPatron(patron);
        return lista(preparar(patron.codePoints().toArray()).lps());
    }

    private void validarPatron(String patron) {
        if (patron == null || patron.isEmpty()) throw new IllegalArgumentException("El patrón no puede ser nulo ni vacío.");
    }

    private Preparacion preparar(int[] patron) {
        int[] lps = new int[patron.length];
        int longitud = 0;
        int i = 1;
        long comparaciones = 0;
        while (i < patron.length) {
            comparaciones++;
            boolean iguales = patron[i] == patron[longitud];
            if (iguales) {
                lps[i++] = ++longitud;
            } else if (longitud > 0) {
                longitud = lps[longitud - 1];
            } else {
                lps[i++] = 0;
            }
        }
        return new Preparacion(lps, comparaciones);
    }

    private Ejecucion ejecutar(String texto, String patron, boolean registrar) {
        if (texto == null) throw new IllegalArgumentException("El texto no puede ser nulo.");
        validarPatron(patron);
        int[] t = texto.codePoints().toArray();
        int[] p = patron.codePoints().toArray();
        long inicio = System.nanoTime();
        var preparacion = preparar(p);
        long tiempoPreparacion = System.nanoTime() - inicio;
        int[] lps = preparacion.lps();
        var posiciones = new ArrayList<Integer>();
        var pasos = new ArrayList<Paso>();
        int i = 0;
        int j = 0;
        long comparaciones = 0;
        inicio = System.nanoTime();
        if (p.length <= t.length) {
            while (i < t.length) {
                int anteriorI = i;
                int anteriorJ = j;
                comparaciones++;
                boolean iguales = t[i] == p[j];
                Integer encontrada = null;
                if (iguales) {
                    i++;
                    j++;
                    if (j == p.length) {
                        encontrada = i - j;
                        posiciones.add(encontrada);
                        // Continúa desde el prefijo reutilizable para conservar superposiciones.
                        j = lps[j - 1];
                    }
                } else if (j > 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
                if (registrar) pasos.add(new Paso(anteriorI, anteriorJ, t[anteriorI], p[anteriorJ], iguales, i, j, encontrada));
            }
        }
        long tiempoBusqueda = System.nanoTime() - inicio;
        var resultado = new ResultadoBusqueda("kmp", patron, t.length, p.length, posiciones, posiciones.size(),
                !posiciones.isEmpty(), comparaciones, preparacion.comparaciones(), tiempoPreparacion, tiempoBusqueda);
        return new Ejecucion(resultado, lista(lps), pasos);
    }

    private List<Integer> lista(int[] valores) {
        var resultado = new ArrayList<Integer>();
        for (int valor : valores) resultado.add(valor);
        return resultado;
    }
}
