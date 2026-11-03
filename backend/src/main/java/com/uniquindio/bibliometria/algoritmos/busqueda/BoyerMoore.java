package com.uniquindio.bibliometria.algoritmos.busqueda;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Boyer–Moore con Bad Character; sin Good Suffix ni búsquedas externas. */
public final class BoyerMoore implements BusquedaExacta {
    public record TablaUltimaAparicion(Map<Integer, Integer> posiciones, long actualizaciones) {
        public TablaUltimaAparicion { posiciones = Map.copyOf(posiciones); }
    }
    public record Comparacion(int indiceTexto, int indicePatron, int caracterTexto, int caracterPatron, boolean iguales) { }
    public record Paso(int alineacion, List<Comparacion> comparaciones, Integer caracterFallo,
                       Integer ultimaAparicion, int desplazamiento, int siguienteAlineacion, Integer posicionEncontrada) {
        public Paso { comparaciones = List.copyOf(comparaciones); }
    }
    public record Ejecucion(ResultadoBusqueda resultado, TablaUltimaAparicion preparacion, List<Paso> pasos) {
        public Ejecucion { pasos = List.copyOf(pasos); }
    }

    @Override public ResultadoBusqueda buscar(String texto, String patron) { return ejecutar(texto, patron, false).resultado(); }
    public Ejecucion buscarConTraza(String texto, String patron) { return ejecutar(texto, patron, true); }

    public TablaUltimaAparicion preparar(String patron) {
        validarPatron(patron);
        return preparar(patron.codePoints().toArray());
    }

    private void validarPatron(String patron) {
        if (patron == null || patron.isEmpty()) throw new IllegalArgumentException("El patrón no puede ser nulo ni vacío.");
    }

    private TablaUltimaAparicion preparar(int[] patron) {
        var ultima = new HashMap<Integer, Integer>();
        long actualizaciones = 0;
        for (int j=0; j<patron.length; j++) {
            ultima.put(patron[j], j);
            actualizaciones++;
        }
        // Sobrescribir conserva la última posición. No evalúa igualdad explícita entre caracteres.
        return new TablaUltimaAparicion(ultima, actualizaciones);
    }

    private Ejecucion ejecutar(String texto, String patron, boolean registrar) {
        if (texto == null) throw new IllegalArgumentException("El texto no puede ser nulo.");
        validarPatron(patron);
        int[] t = texto.codePoints().toArray();
        int[] p = patron.codePoints().toArray();
        long inicio = System.nanoTime();
        var tabla = preparar(p);
        long tiempoPreparacion = System.nanoTime()-inicio;
        var posiciones = new ArrayList<Integer>();
        var pasos = new ArrayList<Paso>();
        long comparaciones = 0;
        int s = 0;
        inicio = System.nanoTime();
        while (s <= t.length-p.length) {
            int j = p.length-1;
            List<Comparacion> detalle = registrar ? new ArrayList<>() : null;
            while (j >= 0) {
                comparaciones++;
                boolean iguales = t[s+j] == p[j];
                if (registrar) detalle.add(new Comparacion(s+j,j,t[s+j],p[j],iguales));
                if (!iguales) break;
                j--;
            }
            Integer encontrada = null;
            Integer fallo = null;
            Integer ultima = null;
            int desplazamiento;
            if (j < 0) {
                encontrada = s;
                posiciones.add(s);
                desplazamiento = 1; // Continuación conservadora: no omite coincidencias superpuestas.
            } else {
                fallo = t[s+j];
                ultima = tabla.posiciones().getOrDefault(fallo,-1);
                desplazamiento = j-ultima;
                if (desplazamiento < 1) desplazamiento = 1;
            }
            int siguiente = s+desplazamiento;
            if (registrar) pasos.add(new Paso(s,detalle,fallo,ultima,desplazamiento,siguiente,encontrada));
            s = siguiente;
        }
        long tiempoBusqueda = System.nanoTime()-inicio;
        var resultado = new ResultadoBusqueda("boyer-moore",patron,t.length,p.length,posiciones,posiciones.size(),
                !posiciones.isEmpty(),comparaciones,0,tiempoPreparacion,tiempoBusqueda);
        return new Ejecucion(resultado,tabla,pasos);
    }
}
