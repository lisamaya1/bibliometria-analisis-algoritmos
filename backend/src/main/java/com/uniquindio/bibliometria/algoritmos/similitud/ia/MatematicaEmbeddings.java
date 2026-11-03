package com.uniquindio.bibliometria.algoritmos.similitud.ia;

import java.util.List;

/** Promedios, normalización y coseno explícitos, sin funciones externas de similitud. */
public final class MatematicaEmbeddings {
    public record Coseno(double productoPunto, double normaA, double normaB, double similitud) { }

    public record Euclidiana(double sumaCuadrados, double normaA, double normaB, double distancia) { }

    /** Primer estado final (CLS); no usa pooler_output ni el promedio de tokens. */
    public double[] poolingCls(float[][] embeddings, long[] mascara) {
        if (embeddings == null || mascara == null || embeddings.length == 0 ||
                embeddings.length != mascara.length || mascara[0] != 1 || embeddings[0] == null || embeddings[0].length == 0)
            throw new IllegalArgumentException("No hay un token CLS valido.");
        double[] vector = new double[embeddings[0].length];
        for (int i = 0; i < vector.length; i++) {
            if (!Float.isFinite(embeddings[0][i])) throw new IllegalArgumentException("CLS contiene valores no finitos.");
            vector[i] = embeddings[0][i];
        }
        return vector;
    }

    public Euclidiana euclidiana(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length)
            throw new IllegalArgumentException("Los vectores deben tener la misma dimension.");
        double normaA = norma(a);
        double normaB = norma(b);
        double suma = 0;
        for (int i = 0; i < a.length; i++) {
            double diferencia = a[i] - b[i];
            suma += diferencia * diferencia;
        }
        return new Euclidiana(suma, normaA, normaB, Math.sqrt(suma));
    }

    public double[] meanPooling(float[][] embeddings, long[] mascara) {
        if (embeddings == null || mascara == null || embeddings.length == 0 || embeddings.length != mascara.length)
            throw new IllegalArgumentException("Embeddings y máscara deben tener la misma cantidad de posiciones.");
        int dimension = embeddings[0].length;
        if (dimension == 0) throw new IllegalArgumentException("El embedding no puede tener dimensión cero.");
        double[] promedio = new double[dimension];
        int posiciones = 0;
        for (int i = 0; i < embeddings.length; i++) {
            if (embeddings[i].length != dimension || (mascara[i] != 0 && mascara[i] != 1))
                throw new IllegalArgumentException("Dimensiones o máscara inválidas.");
            if (mascara[i] == 1) {
                posiciones++;
                for (int j = 0; j < dimension; j++) promedio[j] += embeddings[i][j];
            }
        }
        if (posiciones == 0) throw new IllegalArgumentException("No hay posiciones válidas para mean pooling.");
        for (int j = 0; j < dimension; j++) promedio[j] /= posiciones;
        return promedio;
    }

    public double norma(double[] vector) {
        if (vector == null || vector.length == 0) throw new IllegalArgumentException("El vector no puede estar vacío.");
        double suma = 0;
        for (double valor : vector) {
            if (!Double.isFinite(valor)) throw new IllegalArgumentException("El vector contiene valores no finitos.");
            suma += valor * valor;
        }
        return Math.sqrt(suma);
    }

    public double[] normalizar(double[] vector) {
        double norma = norma(vector);
        if (norma == 0) throw new IllegalArgumentException("No se puede normalizar un vector de norma cero.");
        double[] resultado = new double[vector.length];
        for (int i = 0; i < vector.length; i++) resultado[i] = vector[i] / norma;
        return resultado;
    }

    public double[] promedioPonderado(List<double[]> vectores, List<Integer> pesos) {
        if (vectores == null || pesos == null || vectores.isEmpty() || vectores.size() != pesos.size())
            throw new IllegalArgumentException("Los fragmentos y sus pesos deben corresponder.");
        double[] resultado = new double[vectores.get(0).length];
        long total = 0;
        for (int i = 0; i < vectores.size(); i++) {
            if (pesos.get(i) <= 0 || vectores.get(i).length != resultado.length)
                throw new IllegalArgumentException("Peso o dimensión de fragmento inválidos.");
            total += pesos.get(i);
            for (int j = 0; j < resultado.length; j++) resultado[j] += vectores.get(i)[j] * pesos.get(i);
        }
        for (int j = 0; j < resultado.length; j++) resultado[j] /= total;
        return resultado;
    }

    public Coseno coseno(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) throw new IllegalArgumentException("Los vectores deben tener la misma dimensión.");
        double normaA = norma(a);
        double normaB = norma(b);
        if (normaA == 0 || normaB == 0) throw new IllegalArgumentException("El coseno no está definido para norma cero.");
        double producto = 0;
        for (int i = 0; i < a.length; i++) producto += a[i] * b[i];
        double similitud = producto / (normaA * normaB);
        // Los embeddings pueden tener coordenadas negativas: se conserva la escala [-1, 1].
        similitud = Math.max(-1, Math.min(1, similitud));
        return new Coseno(producto, normaA, normaB, similitud);
    }
}
