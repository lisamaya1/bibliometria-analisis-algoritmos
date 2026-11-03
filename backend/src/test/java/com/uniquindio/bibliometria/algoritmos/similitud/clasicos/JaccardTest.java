package com.uniquindio.bibliometria.algoritmos.similitud.clasicos;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JaccardTest {
    private final Jaccard algoritmo = new Jaccard();

    @Test void conjuntosIgualesIgnoranOrdenYRepeticiones() {
        var resultado = algoritmo.comparar(List.of("ai", "learning", "ai"), List.of("learning", "ai"));
        assertEquals(1, resultado.similitud());
        assertEquals(List.of("ai", "learning"), resultado.interseccion());
    }

    @Test void conjuntosDisjuntosTienenSimilitudCero() {
        var resultado = algoritmo.comparar(List.of("a"), List.of("b"));
        assertEquals(0, resultado.similitud());
        assertEquals(List.of("a", "b"), resultado.union());
    }

    @Test void coincidenciaParcialTieneResultadoMatematico() {
        var resultado = algoritmo.comparar(List.of("a", "b", "b"), List.of("b", "c"));
        assertEquals(List.of("b"), resultado.interseccion());
        assertEquals(List.of("a", "b", "c"), resultado.union());
        assertEquals(1.0 / 3, resultado.similitud(), 1e-12);
    }

    @Test void unSoloConjuntoVacioProduceCero() {
        assertEquals(0, algoritmo.comparar(List.of(), List.of("a")).similitud());
        assertEquals(0, algoritmo.comparar(List.of("a"), List.of()).similitud());
    }

    @Test void rechazaUnionVaciaSinAsignarValorAlCocienteCeroSobreCero() {
        assertThrows(IllegalArgumentException.class, () -> algoritmo.comparar(List.of(), List.of()));
    }

    @Test void rechazaListasNulasYPalabrasNulasOEnBlanco() {
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
        for (var lista : List.of(resultado.conjuntoA(), resultado.conjuntoB(), resultado.interseccion(), resultado.union())) {
            assertThrows(UnsupportedOperationException.class, () -> lista.add("nuevo"));
        }
    }
}
