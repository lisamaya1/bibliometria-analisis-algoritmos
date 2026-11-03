package com.uniquindio.bibliometria.algoritmos.similitud.ia;

/** Interfaz pequeña para separar inferencia real y pruebas matemáticas controladas. */
public interface ModeloMiniLm {
    long[] tokenizar(String texto);
    Salida ejecutar(long[] tokensContenido);
    int dimension();
    int limiteEntrada();
    record Salida(float[][] embeddings, long[] mascara) { }
}
