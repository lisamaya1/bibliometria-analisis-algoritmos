package com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace;

import com.uniquindio.bibliometria.algoritmos.agrupamiento.Cluster;
import com.uniquindio.bibliometria.algoritmos.agrupamiento.MatrizDistancias;
import java.util.ArrayList;
import java.util.List;

/** Aglomeración explícita; conserva las distancias originales entre artículos. */
public abstract class AglomeradorJerarquico {
    public record Fusion(int id, int izquierdo, int derecho, List<Integer> articulos,
                         double distancia, int tamano) {
        public Fusion { articulos = List.copyOf(articulos); }
    }
    public record Resultado(Cluster raiz, List<Fusion> fusiones) {
        public Resultado { fusiones = List.copyOf(fusiones); }
    }

    public abstract double distancia(Cluster a, Cluster b, MatrizDistancias matriz);

    public Resultado agrupar(MatrizDistancias matriz) {
        if (matriz == null) throw new IllegalArgumentException("La matriz es obligatoria.");
        var activos = new ArrayList<Cluster>();
        int mayorId = 0;
        for (int id : matriz.articulos()) {
            activos.add(Cluster.hoja(id));
            mayorId = Math.max(mayorId, id);
        }
        if ((long) mayorId + activos.size() - 1 > Integer.MAX_VALUE)
            throw new IllegalArgumentException("No hay IDs suficientes para registrar las fusiones.");
        var fusiones = new ArrayList<Fusion>();
        int siguienteId = mayorId + 1;
        while (activos.size() > 1) {
            int mejorI = -1;
            int mejorJ = -1;
            int menorIzquierdo = Integer.MAX_VALUE;
            int menorDerecho = Integer.MAX_VALUE;
            double minima = Double.POSITIVE_INFINITY;
            for (int i = 0; i < activos.size(); i++) {
                for (int j = i + 1; j < activos.size(); j++) {
                    var a = activos.get(i);
                    var b = activos.get(j);
                    double d = distancia(a, b, matriz);
                    int izquierdo = Math.min(a.id(), b.id());
                    int derecho = Math.max(a.id(), b.id());
                    if (d < minima || (Double.compare(d, minima) == 0 &&
                            (izquierdo < menorIzquierdo || (izquierdo == menorIzquierdo && derecho < menorDerecho)))) {
                        minima = d;
                        mejorI = i;
                        mejorJ = j;
                        menorIzquierdo = izquierdo;
                        menorDerecho = derecho;
                    }
                }
            }
            var a = activos.get(mejorI);
            var b = activos.get(mejorJ);
            var nuevo = Cluster.fusionar(siguienteId++, a.id() < b.id() ? a : b,
                    a.id() < b.id() ? b : a, minima);
            fusiones.add(new Fusion(nuevo.id(), nuevo.izquierdo().id(), nuevo.derecho().id(),
                    nuevo.articulos(), minima, nuevo.tamano()));
            activos.remove(mejorJ);
            activos.remove(mejorI);
            activos.add(nuevo);
        }
        return new Resultado(activos.get(0), fusiones);
    }
}
