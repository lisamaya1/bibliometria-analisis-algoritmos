package com.uniquindio.bibliometria.algoritmos.similitud.ia;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MatematicaEmbeddingsTest {
    private final MatematicaEmbeddings matematica = new MatematicaEmbeddings();

    @Test void meanPoolingPromediaTodasLasDimensiones() {
        assertArrayEquals(new double[]{3, 5}, matematica.meanPooling(new float[][]{{1, 3}, {5, 7}}, new long[]{1, 1}), 1e-12);
    }
    @Test void mascaraExcluyeRellenoAunqueSusValoresSeanGrandes() {
        assertArrayEquals(new double[]{2, 4}, matematica.meanPooling(new float[][]{{2, 4}, {100, 200}}, new long[]{1, 0}), 1e-12);
    }
    @Test void rechazaMascaraVaciaInvalidaYDimensionesDistintas() {
        assertThrows(IllegalArgumentException.class, () -> matematica.meanPooling(new float[][]{{1}}, new long[]{0}));
        assertThrows(IllegalArgumentException.class, () -> matematica.meanPooling(new float[][]{{1}}, new long[]{2}));
        assertThrows(IllegalArgumentException.class, () -> matematica.meanPooling(new float[][]{{1}, {2, 3}}, new long[]{1, 1}));
    }
    @Test void normalizacionL2NoModificaElOriginal() {
        double[] vector = {3, 4};
        assertEquals(5, matematica.norma(vector));
        assertArrayEquals(new double[]{0.6, 0.8}, matematica.normalizar(vector), 1e-12);
        assertArrayEquals(new double[]{3, 4}, vector);
        assertEquals(1, matematica.norma(matematica.normalizar(vector)), 1e-12);
    }
    @Test void rechazaNormaCeroYValoresNoFinitos() {
        assertThrows(IllegalArgumentException.class, () -> matematica.normalizar(new double[]{0, 0}));
        assertThrows(IllegalArgumentException.class, () -> matematica.norma(new double[]{Double.NaN}));
        assertThrows(IllegalArgumentException.class, () -> matematica.coseno(new double[]{0}, new double[]{1}));
    }
    @Test void cosenoProductoYNormasConVectoresConocidos() {
        var resultado = matematica.coseno(new double[]{1, 0}, new double[]{1, 1});
        assertEquals(1, resultado.productoPunto());
        assertEquals(1, resultado.normaA());
        assertEquals(Math.sqrt(2), resultado.normaB());
        assertEquals(1 / Math.sqrt(2), resultado.similitud(), 1e-12);
        assertEquals(1, matematica.coseno(new double[]{3, 4}, new double[]{3, 4}).similitud(), 1e-12);
        assertEquals(-1, matematica.coseno(new double[]{1, 0}, new double[]{-1, 0}).similitud());
    }
    @Test void combinacionPonderadaRespetaLongitudDeContenido() {
        assertArrayEquals(new double[]{0.75, 0.25}, matematica.promedioPonderado(
                List.of(new double[]{1, 0}, new double[]{0, 1}), List.of(3, 1)), 1e-12);
    }
    @Test void rechazaPesosInvalidosYDimensionesIncompatibles() {
        assertThrows(IllegalArgumentException.class, () -> matematica.promedioPonderado(List.of(new double[]{1}), List.of(0)));
        assertThrows(IllegalArgumentException.class, () -> matematica.coseno(new double[]{1}, new double[]{1, 2}));
    }
}
