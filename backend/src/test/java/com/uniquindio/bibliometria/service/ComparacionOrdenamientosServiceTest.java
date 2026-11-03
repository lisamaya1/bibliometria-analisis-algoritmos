package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.modelo.TerminoFrecuencia;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComparacionOrdenamientosServiceTest {
    private final ComparacionOrdenamientosService servicio =
            new ComparacionOrdenamientosService(new PreprocesamientoService());

    @Test void comparaLaMismaEntradaSinModificarlaYMideCincoVecesCadaAlgoritmo() {
        var entrada = new ArrayList<>(List.of(new TerminoFrecuencia("zeta", 2),
                new TerminoFrecuencia("alpha", 2), new TerminoFrecuencia("ai", 15),
                new TerminoFrecuencia("education", 7)));
        var original = List.copyOf(entrada);
        var resultado = servicio.compararFrecuencias(entrada, 2, true, 20);

        assertEquals(original, entrada);
        assertEquals(20, resultado.totalArticulos());
        assertEquals(4, resultado.totalElementos());
        assertEquals(5, resultado.repeticiones());
        assertTrue(resultado.resultadosEquivalentes());
        assertEquals(List.of("Bucket Sort", "Counting Sort", "Radix Sort"),
                resultado.algoritmos().stream().map(a -> a.algoritmo()).toList());
        double minimo = resultado.algoritmos().stream().mapToDouble(a -> a.tiempoPromedioMs()).min().orElseThrow();
        for (var algoritmo : resultado.algoritmos()) {
            assertEquals(5, algoritmo.ejecucionesMs().size());
            assertTrue(algoritmo.ejecucionesMs().stream().allMatch(t -> t >= 0));
            assertEquals(algoritmo.ejecucionesMs().stream().mapToDouble(Double::doubleValue).average().orElseThrow(),
                    algoritmo.tiempoPromedioMs(), 1e-9);
            assertTrue(algoritmo.resultadoCorrecto());
            assertEquals(List.of("ai", "education"), algoritmo.terminos().stream().map(t -> t.termino()).toList());
            assertEquals(algoritmo.tiempoPromedioMs() == minimo, resultado.ganadores().contains(algoritmo.algoritmo()));
        }
        assertFalse(resultado.ganadores().isEmpty());
    }

    @Test void aceptaEmpatesDeFrecuenciaSinMarcarComoIncorrectoSuOrden() {
        var resultado = servicio.compararFrecuencias(List.of(new TerminoFrecuencia("z", 4),
                new TerminoFrecuencia("a", 4), new TerminoFrecuencia("m", 4)), 15, false, 1);
        assertTrue(resultado.resultadosEquivalentes());
        assertTrue(resultado.algoritmos().stream().allMatch(a -> a.terminos().size() == 3));
    }

    @Test void rechazaDatosVaciosYLimitesInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> servicio.compararFrecuencias(List.of(), 15, true, 20));
        var entrada = List.of(new TerminoFrecuencia("ai", 2));
        assertThrows(IllegalArgumentException.class, () -> servicio.compararFrecuencias(entrada, 0, true, 20));
        assertThrows(IllegalArgumentException.class, () -> servicio.compararFrecuencias(entrada, 1001, true, 20));
    }

    @Test void cargaElCorpusUnaSolaVezYComparteLasMismasFrecuencias() throws Exception {
        int[] lecturas = {0};
        var lector = new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path pdfs, Path json) {
                lecturas[0]++;
                return List.of(new ArticuloDocumental(1, "ejemplo.pdf", "Titulo", List.of("Autor"),
                        "Resumen", "resumen", List.of("ai", "ai", "education"), 3));
            }
        };
        var resultado = new ComparacionOrdenamientosService(lector).comparar(15, false);
        assertEquals(1, lecturas[0]);
        assertEquals(1, resultado.totalArticulos());
        assertEquals(2, resultado.totalElementos());
        assertTrue(resultado.resultadosEquivalentes());
    }
}
