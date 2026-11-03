package com.uniquindio.bibliometria.algoritmos.similitud.ia;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EmbeddingMiniLmTest {
    @Test void fragmentosConsecutivosSinPerdidaNiSolapamiento() {
        var algoritmo = new EmbeddingMiniLm(null);
        var partes = algoritmo.fragmentar(new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, 6);
        assertEquals(3, partes.size());
        assertArrayEquals(new long[]{1, 2, 3, 4}, partes.get(0));
        assertArrayEquals(new long[]{5, 6, 7, 8}, partes.get(1));
        assertArrayEquals(new long[]{9}, partes.get(2));
    }
    @Test void limite256ReservaDosTokensEspeciales() {
        var algoritmo = new EmbeddingMiniLm(null);
        assertEquals(1, algoritmo.fragmentar(new long[254], 256).size());
        assertEquals(2, algoritmo.fragmentar(new long[255], 256).size());
    }
    @Test void textoCortoSeNormalizaSinFragmentacion() {
        var modelo = controlado(2);
        var resultado = new EmbeddingMiniLm(modelo).generar("texto corto");
        assertEquals(2, resultado.tokensContenido());
        assertEquals(1, resultado.fragmentos());
        assertArrayEquals(new double[]{1, 0}, resultado.vector(), 1e-12);
    }
    @Test void textoLargoCombinaEmbeddingsNormalizadosConPesosDeContenido() {
        var resultado = new EmbeddingMiniLm(controlado(5)).generar("texto largo");
        assertEquals(5, resultado.tokensContenido());
        assertEquals(3, resultado.fragmentos());
        assertEquals(List.of(2, 2, 1), resultado.tokensPorFragmento());
        double norma = Math.sqrt(0.8 * 0.8 + 0.2 * 0.2);
        assertArrayEquals(new double[]{0.8 / norma, 0.2 / norma}, resultado.vector(), 1e-12);
    }
    @Test void rechazaTextoVacioYSinTokensUtilizables() {
        var algoritmo = new EmbeddingMiniLm(controlado(0));
        assertThrows(IllegalArgumentException.class, () -> algoritmo.generar(" "));
        assertThrows(IllegalArgumentException.class, () -> algoritmo.generar("sin tokens"));
        assertThrows(IllegalArgumentException.class, () -> algoritmo.fragmentar(new long[]{1}, 2));
    }

    private static ModeloMiniLm controlado(int cantidad) {
        return new ModeloMiniLm() {
            public int dimension() { return 2; }
            public int limiteEntrada() { return 4; }
            public long[] tokenizar(String texto) {
                long[] tokens = new long[cantidad];
                for (int i = 0; i < cantidad; i++) tokens[i] = i + 1;
                return tokens;
            }
            public Salida ejecutar(long[] tokens) {
                float[] valor = tokens[0] < 5 ? new float[]{3, 0} : new float[]{0, 7};
                return new Salida(new float[][]{valor, valor}, new long[]{1, 1});
            }
        };
    }
}
