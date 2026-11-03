package com.uniquindio.bibliometria.algoritmos.similitud.clasicos;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DiceTest {
    private final Dice algoritmo = new Dice();

    @Test void conjuntosIgualesIgnoranOrdenYRepeticiones() {
        var resultado = algoritmo.comparar(List.of("ai", "learning", "ai"), List.of("learning", "ai"));
        assertEquals(1, resultado.similitud());
        assertEquals(List.of("ai", "learning"), resultado.interseccion());
        assertEquals(2, resultado.cardinalidadA());
        assertEquals(2, resultado.cardinalidadB());
        assertEquals(2, resultado.cardinalidadInterseccion());
    }

    @Test void conjuntosDisjuntosTienenSimilitudCero() {
        var resultado = algoritmo.comparar(List.of("a"), List.of("b"));
        assertEquals(0, resultado.similitud());
        assertEquals(List.of(), resultado.interseccion());
    }

    @Test void coincidenciaParcialUsaDosVecesLaInterseccionSobreSumaDeTamanos() {
        var resultado = algoritmo.comparar(List.of("a", "b", "b"), List.of("b", "c", "d"));
        assertEquals(List.of("b"), resultado.interseccion());
        assertEquals(2, resultado.cardinalidadA());
        assertEquals(3, resultado.cardinalidadB());
        assertEquals(2.0 / 5, resultado.similitud(), 1e-12);
    }

    @Test void unSoloConjuntoVacioProduceCero() {
        assertEquals(0, algoritmo.comparar(List.of(), List.of("a")).similitud());
        assertEquals(0, algoritmo.comparar(List.of("a"), List.of()).similitud());
    }

    @Test void rechazaDosConjuntosVacios() {
        assertThrows(IllegalArgumentException.class, () -> algoritmo.comparar(List.of(), List.of()));
    }

    @Test void rechazaEntradasNulasYPalabrasNulasOEnBlanco() {
        assertThrows(NullPointerException.class, () -> algoritmo.comparar(null, List.of()));
        assertThrows(NullPointerException.class, () -> algoritmo.comparar(List.of(), null));
        assertThrows(NullPointerException.class, () -> algoritmo.comparar(Arrays.asList("a", null), List.of()));
        assertThrows(IllegalArgumentException.class, () -> algoritmo.comparar(List.of(" "), List.of()));
    }

    @Test void mantieneSimetriaYLimites() {
        var muestras = List.of(List.<String>of(), List.of("a"), List.of("a", "b"), List.of("b", "c", "c"));
        for (var a : muestras) for (var b : muestras) {
            if (a.isEmpty() && b.isEmpty()) continue;
            double valor = algoritmo.comparar(a, b).similitud();
            assertEquals(valor, algoritmo.comparar(b, a).similitud());
            assertTrue(valor >= 0 && valor <= 1);
        }
    }

    @Test void noModificaEntradasNiPermiteModificarResultados() {
        var a = new ArrayList<>(List.of("a", "b", "a"));
        var b = new ArrayList<>(List.of("b"));
        var resultado = algoritmo.comparar(a, b);
        assertEquals(List.of("a", "b", "a"), a);
        assertEquals(List.of("b"), b);
        for (var lista : List.of(resultado.conjuntoA(), resultado.conjuntoB(), resultado.interseccion())) {
            assertThrows(UnsupportedOperationException.class, () -> lista.add("nuevo"));
        }
    }
}
