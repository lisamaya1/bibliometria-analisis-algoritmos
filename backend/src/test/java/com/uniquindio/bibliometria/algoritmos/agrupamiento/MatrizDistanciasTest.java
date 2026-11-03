package com.uniquindio.bibliometria.algoritmos.agrupamiento;

import com.uniquindio.bibliometria.algoritmos.similitud.clasicos.CosenoTfidf;
import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.service.PreprocesamientoService;
import com.uniquindio.bibliometria.util.JsonUtil;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MatrizDistanciasTest {
    @Test void corpusRealVeintePorVeinteDiagonalSimetriaYRango() throws Exception {
        var corpus = JsonUtil.cargarArticulosDesdeJson(PreprocesamientoService.RUTA_JSON_DEFECTO);
        var m = MatrizDistancias.desdeCorpus(corpus);
        assertEquals(20, m.tamano());
        var tokens = corpus.stream().map(ArticuloDocumental::tokens).toList();
        var coseno = new CosenoTfidf();
        var r = coseno.construirRepresentacion(tokens);
        for (int i = 0; i < 20; i++) {
            int a = corpus.get(i).id();
            assertEquals(0, m.distancia(a, a));
            for (int j = 0; j < 20; j++) {
                int b = corpus.get(j).id();
                double d = m.distancia(a, b);
                assertTrue(Double.isFinite(d) && d >= 0 && d <= 1);
                assertEquals(d, m.distancia(b, a));
                assertEquals(1 - coseno.compararRepresentacion(r, i, j).similitud(), d, 1e-12);
            }
        }
        assertEquals(0.9614110189106007, m.distancia(15, 19), 1e-12);
    }
    @Test void copiaDefensivaYBusquedaPorIdNoPorPosicion() {
        double[][] datos = {{0, 0.2}, {0.2, 0}};
        var m = new MatrizDistancias(List.of(15, 19), datos);
        datos[0][1] = 0.8;
        assertEquals(0.2, m.distancia(15, 19));
        assertThrows(UnsupportedOperationException.class, () -> m.articulos().add(20));
        assertThrows(IllegalArgumentException.class, () -> m.distancia(1, 19));
    }
    @Test void rechazaFormasIdsYAsimetriaInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new MatrizDistancias(List.of(1), new double[][]{{0}}));
        assertThrows(IllegalArgumentException.class, () -> new MatrizDistancias(List.of(1, 1), new double[][]{{0, 0}, {0, 0}}));
        assertThrows(IllegalArgumentException.class, () -> new MatrizDistancias(Arrays.asList(1, null), new double[][]{{0, 0}, {0, 0}}));
        assertThrows(IllegalArgumentException.class, () -> new MatrizDistancias(List.of(1, 2), new double[][]{{0, 0.2}, {0.3, 0}}));
        assertThrows(IllegalArgumentException.class, () -> new MatrizDistancias(List.of(1, 2), new double[][]{{0}, {0, 0}}));
    }
    @Test void rechazaDiagonalYRangoNoFinitos() {
        for (double valor : new double[]{-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> new MatrizDistancias(List.of(1, 2), new double[][]{{0, valor}, {valor, 0}}));
        assertThrows(IllegalArgumentException.class, () -> new MatrizDistancias(List.of(1, 2), new double[][]{{0.1, 0}, {0, 0}}));
    }
    @Test void rechazaCorpusInsuficienteOVectorTfidfNulo() {
        assertThrows(IllegalArgumentException.class, () -> MatrizDistancias.desdeCorpus(List.of()));
        var a = new ArticuloDocumental(1, "a", "a", List.of(), "a", "a", List.of("comun"), 1);
        var b = new ArticuloDocumental(2, "b", "b", List.of(), "b", "b", List.of("comun"), 1);
        assertThrows(IllegalArgumentException.class, () -> MatrizDistancias.desdeCorpus(List.of(a, b)));
    }
}
