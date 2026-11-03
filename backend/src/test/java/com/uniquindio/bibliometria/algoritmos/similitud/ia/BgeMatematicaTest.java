package com.uniquindio.bibliometria.algoritmos.similitud.ia;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BgeMatematicaTest {
    private final MatematicaEmbeddings m = new MatematicaEmbeddings();
    @Test void distanciaTresCuatroCincoYSimetria() {
        var resultado = m.euclidiana(new double[]{0, 0}, new double[]{3, 4});
        assertEquals(25, resultado.sumaCuadrados());
        assertEquals(5, resultado.distancia());
        assertEquals(5, resultado.normaB());
        assertEquals(5, m.euclidiana(new double[]{3, 4}, new double[]{0, 0}).distancia());
        assertEquals(0, m.euclidiana(new double[]{3, 4}, new double[]{3, 4}).distancia());
    }
    @Test void distanciaValidaEntradas() {
        assertThrows(IllegalArgumentException.class, () -> m.euclidiana(null, new double[]{1}));
        assertThrows(IllegalArgumentException.class, () -> m.euclidiana(new double[]{1}, new double[]{1, 2}));
        assertThrows(IllegalArgumentException.class, () -> m.euclidiana(new double[]{}, new double[]{}));
        assertThrows(IllegalArgumentException.class, () -> m.euclidiana(new double[]{Double.NaN}, new double[]{1}));
    }
    @Test void clsSeleccionaSoloPrimerEstadoSinPromediarNiUsarPooler() {
        assertArrayEquals(new double[]{3, 4}, m.poolingCls(new float[][]{{3, 4}, {100, 200}}, new long[]{1, 1}));
        assertThrows(IllegalArgumentException.class, () -> m.poolingCls(new float[][]{{1}}, new long[]{0}));
        assertThrows(IllegalArgumentException.class, () -> m.poolingCls(new float[][]{}, new long[]{}));
    }
    @Test void normalizacionYPonderacionConDatosControlados() {
        assertArrayEquals(new double[]{0.6, 0.8}, m.normalizar(new double[]{3, 4}), 1e-12);
        assertArrayEquals(new double[]{0.75, 0.25}, m.promedioPonderado(List.of(new double[]{1, 0}, new double[]{0, 1}), List.of(3, 1)), 1e-12);
        assertThrows(IllegalArgumentException.class, () -> m.normalizar(new double[]{0, 0}));
    }
    @Test void relacionEntreDistanciaYCosenoNormalizados() {
        double[] a = m.normalizar(new double[]{3, 4});
        double[] b = m.normalizar(new double[]{-2, 1});
        assertEquals(2 - 2 * m.coseno(a, b).similitud(), m.euclidiana(a, b).sumaCuadrados(), 1e-12);
    }
}
