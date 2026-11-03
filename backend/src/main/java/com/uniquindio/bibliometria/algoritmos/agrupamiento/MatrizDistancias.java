package com.uniquindio.bibliometria.algoritmos.agrupamiento;

import com.uniquindio.bibliometria.algoritmos.similitud.clasicos.CosenoTfidf;
import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Distancias por ID de artículo; copia defensiva para preservar la matriz base. */
public final class MatrizDistancias {
    private final List<Integer> articulos;
    private final Map<Integer, Integer> indices = new HashMap<>();
    private final double[][] valores;

    public MatrizDistancias(List<Integer> articulos, double[][] valores) {
        if (articulos == null || articulos.size() < 2 || valores == null || valores.length != articulos.size())
            throw new IllegalArgumentException("La matriz debe corresponder al menos a dos artículos.");
        int n = articulos.size();
        this.valores = new double[n][n];
        for (int i = 0; i < n; i++) {
            Integer id = articulos.get(i);
            if (id == null || id < 1 || indices.put(id, i) != null)
                throw new IllegalArgumentException("Los IDs de artículos deben ser positivos y distintos.");
            if (valores[i] == null || valores[i].length != n)
                throw new IllegalArgumentException("La matriz debe ser cuadrada.");
            for (int j = 0; j < n; j++) {
                double d = valores[i][j];
                if (!Double.isFinite(d) || d < 0 || d > 1 || (i == j && d != 0))
                    throw new IllegalArgumentException("Las distancias deben ser finitas entre 0 y 1 y la diagonal cero.");
                this.valores[i][j] = d;
            }
        }
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++)
                if (Double.compare(this.valores[i][j], this.valores[j][i]) != 0)
                    throw new IllegalArgumentException("La matriz debe ser simétrica.");
        this.articulos = List.copyOf(articulos);
    }

    /** Una representación TF-IDF global; cada pareja no ordenada se calcula una vez. */
    public static MatrizDistancias desdeCorpus(List<ArticuloDocumental> corpus) {
        if (corpus == null || corpus.size() < 2)
            throw new IllegalArgumentException("Se necesitan al menos dos artículos para agrupar.");
        var ids = new ArrayList<Integer>();
        var tokens = new ArrayList<List<String>>();
        for (var articulo : corpus) {
            ids.add(articulo.id());
            tokens.add(articulo.tokens());
        }
        var coseno = new CosenoTfidf();
        var representacion = coseno.construirRepresentacion(tokens);
        double[][] distancias = new double[corpus.size()][corpus.size()];
        for (int i = 0; i < corpus.size(); i++) {
            // También rechaza vectores nulos: su coseno no está definido.
            coseno.compararRepresentacion(representacion, i, i);
            for (int j = i + 1; j < corpus.size(); j++) {
                double d = 1 - coseno.compararRepresentacion(representacion, i, j).similitud();
                distancias[i][j] = d;
                distancias[j][i] = d;
            }
        }
        return new MatrizDistancias(ids, distancias);
    }

    public List<Integer> articulos() { return articulos; }
    public int tamano() { return articulos.size(); }
    public double distancia(int articuloA, int articuloB) {
        Integer a = indices.get(articuloA);
        Integer b = indices.get(articuloB);
        if (a == null || b == null) throw new IllegalArgumentException("Artículo inexistente en la matriz.");
        return valores[a][b];
    }
}
