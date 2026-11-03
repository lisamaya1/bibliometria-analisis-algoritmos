package com.uniquindio.bibliometria.algoritmos.agrupamiento;

import com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace.AglomeradorJerarquico;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/** Reconstruye los clusters activos después de n-k fusiones. */
public final class Particiones {
    private Particiones() { }
    public record Grupo(int id, List<Integer> articulos, int tamano) {
        public Grupo { articulos = List.copyOf(articulos); }
    }

    public static List<Grupo> cortar(List<Integer> articulos, List<AglomeradorJerarquico.Fusion> historial, int k) {
        if (articulos == null || historial == null || k < 1 || k > articulos.size() || historial.size() < articulos.size()-k)
            throw new IllegalArgumentException("El corte requiere IDs, historial suficiente y un k entre 1 y n.");
        var activos = new HashMap<Integer, Grupo>();
        for (Integer id : articulos) {
            if (id == null || id < 1 || activos.put(id, new Grupo(id, List.of(id), 1)) != null)
                throw new IllegalArgumentException("Los artículos del corte deben ser positivos y distintos.");
        }
        for (int paso=0; paso<articulos.size()-k; paso++) {
            var f = historial.get(paso);
            var a = activos.remove(f.izquierdo());
            var b = activos.remove(f.derecho());
            if (a == null || b == null || activos.containsKey(f.id()) || f.id() <= Math.max(a.id(),b.id()))
                throw new IllegalArgumentException("Historial de fusiones inválido.");
            var miembros = new ArrayList<>(a.articulos());
            miembros.addAll(b.articulos());
            if (!miembros.equals(f.articulos()) || f.tamano() != miembros.size())
                throw new IllegalArgumentException("Los miembros no corresponden a los hijos de la fusión.");
            activos.put(f.id(), new Grupo(f.id(), miembros, miembros.size()));
        }
        var grupos = new ArrayList<>(activos.values());
        grupos.sort(Comparator.comparingInt(Grupo::id));
        return List.copyOf(grupos);
    }
}
