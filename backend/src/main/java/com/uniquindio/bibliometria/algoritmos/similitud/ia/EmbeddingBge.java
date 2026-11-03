package com.uniquindio.bibliometria.algoritmos.similitud.ia;

import java.util.ArrayList;
import java.util.List;

public final class EmbeddingBge {
    private final ModeloBge modelo;
    private final MatematicaEmbeddings matematica = new MatematicaEmbeddings();

    public EmbeddingBge(ModeloBge modelo) { this.modelo = modelo; }

    public record Resultado(double[] vector, int tokensContenido, int fragmentos, List<Integer> tokensPorFragmento) { }

    /** Dos posiciones quedan reservadas para [CLS] y [SEP]. Sin solapamiento. */
    public List<long[]> fragmentar(long[] tokens, int limiteEntrada) {
        return FragmentadorTokens.fragmentar(tokens, limiteEntrada);
    }

    public Resultado generar(String texto) {
        if (texto == null || texto.isBlank()) throw new IllegalArgumentException("El abstract procesado está vacío.");
        long[] tokens = modelo.tokenizar(texto);
        List<long[]> fragmentos = fragmentar(tokens, modelo.limiteEntrada());
        var vectores = new ArrayList<double[]>();
        var pesos = new ArrayList<Integer>();
        for (long[] fragmento : fragmentos) {
            var salida = modelo.ejecutar(fragmento);
            double[] promedio = matematica.poolingCls(salida.embeddings(), salida.mascara());
            if (promedio.length != modelo.dimension()) throw new IllegalStateException("El modelo devolvió una dimensión inesperada.");
            vectores.add(matematica.normalizar(promedio));
            pesos.add(fragmento.length);
        }
        double[] combinado = matematica.promedioPonderado(vectores, pesos);
        return new Resultado(matematica.normalizar(combinado), tokens.length, fragmentos.size(), List.copyOf(pesos));
    }
}
