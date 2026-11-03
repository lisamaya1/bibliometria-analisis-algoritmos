package com.uniquindio.bibliometria.algoritmos.similitud.clasicos;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CosenoTfidfTest {
    @Test void reutilizarRepresentacionDaElMismoResultadoParaTodasLasParejas() {
        var modelo = algoritmo.construirRepresentacion(corpus);
        for (int a = 0; a < corpus.size(); a++) for (int b = 0; b < corpus.size(); b++) {
            assertEquals(algoritmo.comparar(corpus, a, b), algoritmo.compararRepresentacion(modelo, a, b));
        }
    }

    @Test void representacionReutilizadaValidaIndicesYNulos() {
        var modelo = algoritmo.construirRepresentacion(corpus);
        assertThrows(NullPointerException.class, () -> algoritmo.compararRepresentacion(null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> algoritmo.compararRepresentacion(modelo, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> algoritmo.compararRepresentacion(modelo, 0, 3));
    }

    private final CosenoTfidf algoritmo = new CosenoTfidf();
    private final List<List<String>> corpus = List.of(List.of("a", "a", "comun"),
            List.of("b", "comun"), List.of("a", "b", "comun"));

    @Test void vocabularioTfDfIdfPesosYCalculoCoincidenConEjemploManual() {
        double l = Math.log(3.0 / 2);
        var modelo = algoritmo.construirRepresentacion(corpus);
        assertEquals(List.of("a", "comun", "b"), modelo.vocabulario());
        assertEquals(List.of(2, 3, 2), modelo.df());
        assertEquals(List.of(l, 0.0, l), modelo.idf());
        assertEquals(List.of(2, 1, 0), modelo.documentos().get(0).frecuencias());
        assertEquals(List.of(2 * l, 0.0, 0.0), modelo.documentos().get(0).pesos());
        var resultado = algoritmo.comparar(corpus, 0, 2);
        assertEquals(3, resultado.totalDocumentos());
        assertEquals(3, resultado.tamanoVocabulario());
        assertEquals(2 * l * l, resultado.productoPunto(), 1e-12);
        assertEquals(2 * l, resultado.normaA(), 1e-12);
        assertEquals(Math.sqrt(2) * l, resultado.normaB(), 1e-12);
        assertEquals(1 / Math.sqrt(2), resultado.similitud(), 1e-12);
    }

    @Test void vectorNoNuloComparadoConsigoMismoEsUno() {
        assertEquals(1, algoritmo.comparar(corpus, 0, 0).similitud(), 1e-12);
    }

    @Test void documentosDisjuntosProducenCero() {
        assertEquals(0, algoritmo.comparar(List.of(List.of("a"), List.of("b")), 0, 1).similitud());
    }

    @Test void simetriaYLimites() {
        for (int a = 0; a < corpus.size(); a++) for (int b = 0; b < corpus.size(); b++) {
            var ab = algoritmo.comparar(corpus, a, b);
            var ba = algoritmo.comparar(corpus, b, a);
            assertEquals(ab.similitud(), ba.similitud(), 1e-12);
            assertEquals(ab.normaA(), ba.normaB(), 1e-12);
            assertTrue(ab.similitud() >= 0 && ab.similitud() <= 1);
        }
    }

    @Test void rechazaNormaCeroAunqueExistanTerminos() {
        var iguales = List.of(List.of("comun"), List.of("comun", "comun"));
        assertThrows(IllegalArgumentException.class, () -> algoritmo.comparar(iguales, 0, 0));
        assertEquals(0, algoritmo.construirRepresentacion(iguales).idf().get(0));
    }

    @Test void rechazaDocumentosSeleccionadosVaciosYCorpusVacio() {
        assertThrows(IllegalArgumentException.class, () -> algoritmo.construirRepresentacion(List.of()));
        assertThrows(IllegalArgumentException.class, () -> algoritmo.comparar(List.of(List.of(), List.of("a")), 0, 1));
        assertThrows(IllegalArgumentException.class, () -> algoritmo.comparar(List.of(List.of(), List.of()), 0, 1));
    }

    @Test void rechazaTerminosInvalidosSinFiltrarlosSilenciosamente() {
        assertThrows(NullPointerException.class, () -> algoritmo.construirRepresentacion(null));
        assertThrows(NullPointerException.class, () -> algoritmo.construirRepresentacion(Arrays.asList((List<String>) null)));
        assertThrows(NullPointerException.class, () -> algoritmo.construirRepresentacion(List.of(Arrays.asList("a", null))));
        assertThrows(IllegalArgumentException.class, () -> algoritmo.construirRepresentacion(List.of(List.of(" "))));
    }

    @Test void noModificaEntradasYLaRepresentacionEsInmutable() {
        var tokens = new ArrayList<>(List.of("a", "a"));
        var modelo = algoritmo.construirRepresentacion(List.of(tokens, List.of("b")));
        assertEquals(List.of("a", "a"), tokens);
        assertThrows(UnsupportedOperationException.class, () -> modelo.vocabulario().add("c"));
        assertThrows(UnsupportedOperationException.class, () -> modelo.documentos().get(0).pesos().add(2.0));
    }

    @Test void rechazaIndicesFueraDelCorpus() {
        assertThrows(IllegalArgumentException.class, () -> algoritmo.comparar(corpus, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> algoritmo.comparar(corpus, 0, 3));
    }

    @Test void idfIncluyeDocumentosNoSeleccionados() {
        var dos = algoritmo.construirRepresentacion(corpus.subList(0, 2));
        var tres = algoritmo.construirRepresentacion(corpus);
        assertEquals(Math.log(2), dos.idf().get(0), 1e-12);
        assertEquals(Math.log(3.0 / 2), tres.idf().get(0), 1e-12);
    }
}
