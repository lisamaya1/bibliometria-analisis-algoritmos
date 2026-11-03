package com.uniquindio.bibliometria.algoritmos.similitud.clasicos;

import java.util.Objects;

/** Distancia de edición por puntos de código Unicode con matriz completa. */
public final class Levenshtein {
    public record Resultado(int distancia, int longitudA, int longitudB, double similitud) { }

    /** Compara los textos recibidos sin limpiarlos ni retokenizarlos. */
    public Resultado comparar(String textoA, String textoB) {
        int[][] matriz = construirMatriz(textoA, textoB);
        int longitudA = matriz.length - 1;
        int longitudB = matriz[0].length - 1;
        int distancia = matriz[longitudA][longitudB];
        int longitudMaxima = Math.max(longitudA, longitudB);
        double similitud = longitudMaxima == 0 ? 1.0 : 1.0 - (double) distancia / longitudMaxima;
        return new Resultado(distancia, longitudA, longitudB, similitud);
    }

    /** Disponible para reproducir ejemplos académicos; no se expone en el endpoint. */
    public int[][] construirMatriz(String textoA, String textoB) {
        Objects.requireNonNull(textoA, "El texto A no puede ser nulo.");
        Objects.requireNonNull(textoB, "El texto B no puede ser nulo.");
        int[] a = textoA.codePoints().toArray();
        int[] b = textoB.codePoints().toArray();
        int[][] matriz = new int[a.length + 1][b.length + 1];
        // Transformar un prefijo en vacío requiere eliminaciones; el inverso, inserciones.
        for (int i = 0; i <= a.length; i++) matriz[i][0] = i;
        for (int j = 0; j <= b.length; j++) matriz[0][j] = j;
        for (int i = 1; i <= a.length; i++) {
            for (int j = 1; j <= b.length; j++) {
                int costoSustitucion = a[i - 1] == b[j - 1] ? 0 : 1;
                int eliminacion = matriz[i - 1][j] + 1;
                int insercion = matriz[i][j - 1] + 1;
                int sustitucionOCoincidencia = matriz[i - 1][j - 1] + costoSustitucion;
                matriz[i][j] = Math.min(Math.min(eliminacion, insercion), sustitucionOCoincidencia);
            }
        }
        return matriz;
    }
}
