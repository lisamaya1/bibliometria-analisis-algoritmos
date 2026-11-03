package com.uniquindio.bibliometria.algoritmos.similitud.clasicos;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LevenshteinTest {
    private final Levenshtein algoritmo = new Levenshtein();

    @Test void cadenasIgualesTienenDistanciaCeroYSimilitudUno() {
        var resultado = algoritmo.comparar("artificial intelligence", "artificial intelligence");
        assertEquals(0, resultado.distancia());
        assertEquals(1, resultado.similitud());
    }

    @Test void manejaAmbosVaciosYUnSoloTextoVacio() {
        assertEquals(new Levenshtein.Resultado(0, 0, 0, 1), algoritmo.comparar("", ""));
        assertEquals(new Levenshtein.Resultado(3, 0, 3, 0), algoritmo.comparar("", "abc"));
        assertEquals(new Levenshtein.Resultado(3, 3, 0, 0), algoritmo.comparar("abc", ""));
    }

    @Test void unaInsercion() { assertEquals(1, algoritmo.comparar("ab", "acb").distancia()); }
    @Test void unaEliminacion() { assertEquals(1, algoritmo.comparar("acb", "ab").distancia()); }
    @Test void unaSustitucion() { assertEquals(1, algoritmo.comparar("abc", "axc").distancia()); }

    @Test void casoConocidoKittenSitting() {
        var resultado = algoritmo.comparar("kitten", "sitting");
        assertEquals(3, resultado.distancia());
        assertEquals(1.0 - 3.0 / 7, resultado.similitud(), 1e-12);
    }

    @Test void simetriaYNormalizacionEntreCeroYUno() {
        var muestras = List.of("", "a", "abc", "xyz", "a b.");
        for (String a : muestras) for (String b : muestras) {
            var resultado = algoritmo.comparar(a, b);
            assertEquals(resultado.distancia(), algoritmo.comparar(b, a).distancia());
            assertTrue(resultado.distancia() <= Math.max(resultado.longitudA(), resultado.longitudB()));
            assertTrue(resultado.similitud() >= 0 && resultado.similitud() <= 1);
        }
    }

    @Test void matrizCompletaExponeInicializacionYRecurrenciaParaDemostracion() {
        int[][] matriz = algoritmo.construirMatriz("ab", "acb");
        assertEquals(3, matriz.length);
        assertArrayEquals(new int[]{0, 1, 2, 3}, matriz[0]);
        assertArrayEquals(new int[]{1, 0, 1, 2}, matriz[1]);
        assertArrayEquals(new int[]{2, 1, 1, 1}, matriz[2]);
    }

    @Test void cuentaCaracteresUnicodeFueraDelPlanoBasicoUnaSolaVez() {
        var resultado = algoritmo.comparar("a\uD83D\uDE00", "a");
        assertEquals(2, resultado.longitudA());
        assertEquals(1, resultado.longitudB());
        assertEquals(1, resultado.distancia());
        assertEquals(0.5, resultado.similitud());
    }

    @Test void rechazaTextosNulos() {
        assertThrows(NullPointerException.class, () -> algoritmo.comparar(null, "a"));
        assertThrows(NullPointerException.class, () -> algoritmo.construirMatriz("a", null));
    }

    @Test void conservaEspaciosPuntuacionYMayusculasDeLaEntrada() {
        assertEquals(1, algoritmo.comparar("a b", "ab").distancia());
        assertEquals(1, algoritmo.comparar("ab.", "ab").distancia());
        assertEquals(1, algoritmo.comparar("A", "a").distancia());
    }
}
