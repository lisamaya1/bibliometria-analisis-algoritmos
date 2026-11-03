package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.algoritmos.ordenamiento.BucketSort;
import com.uniquindio.bibliometria.algoritmos.ordenamiento.CountingSort;
import com.uniquindio.bibliometria.algoritmos.ordenamiento.RadixSort;
import com.uniquindio.bibliometria.algoritmos.ordenamiento.ResultadoOrdenamiento;
import com.uniquindio.bibliometria.modelo.TerminoFrecuencia;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class ComparacionOrdenamientosService {
    private static final int REPETICIONES = 5;
    private final PreprocesamientoService preprocesamiento;
    private final List<Algoritmo> algoritmos = List.of(
            new Algoritmo("Bucket Sort", BucketSort::ordenarDescendente),
            new Algoritmo("Counting Sort", CountingSort::ordenarDescendente),
            new Algoritmo("Radix Sort", RadixSort::ordenarDescendente));

    public ComparacionOrdenamientosService(PreprocesamientoService preprocesamiento) {
        this.preprocesamiento = preprocesamiento;
    }

    public Comparacion comparar(int topK, boolean filtrarStopwords) throws IOException {
        Path datos = Files.exists(Path.of("datos")) ? Path.of("datos") : Path.of("..", "datos");
        var corpus = preprocesamiento.obtenerCorpus(datos.resolve("pdfs_originales"),
                datos.resolve("procesados/corpus_procesado.json"));
        var frecuencias = preprocesamiento.extraerFrecuenciasGlobales(corpus, filtrarStopwords);
        return compararFrecuencias(frecuencias, topK, filtrarStopwords, corpus.size());
    }

    public Comparacion compararFrecuencias(List<TerminoFrecuencia> frecuencias, int topK,
                                            boolean filtrarStopwords, int totalArticulos) {
        if (topK < 1 || topK > 1000) {
            throw new IllegalArgumentException("La cantidad de resultados debe estar entre 1 y 1000.");
        }
        if (frecuencias.isEmpty()) {
            throw new IllegalArgumentException("No hay términos para comparar con el filtro seleccionado.");
        }
        List<TerminoFrecuencia> entrada = List.copyOf(frecuencias);
        Map<TerminoFrecuencia, Integer> elementosEsperados = contarElementos(entrada);
        var mediciones = new ArrayList<List<Long>>();
        var resultados = new ArrayList<ResultadoOrdenamiento>();
        boolean[] correctos = new boolean[algoritmos.size()];
        for (Algoritmo algoritmo : algoritmos) {
            // Una ejecución previa por algoritmo carga las clases fuera de la medición.
            algoritmo.ordenar().apply(new ArrayList<>(entrada));
            mediciones.add(new ArrayList<>());
            resultados.add(null);
        }
        java.util.Arrays.fill(correctos, true);

        for (int ronda = 0; ronda < REPETICIONES; ronda++) {
            // Alterna quién se ejecuta primero. Todos reciben una copia de la misma lista.
            for (int turno = 0; turno < algoritmos.size(); turno++) {
                int indice = (ronda + turno) % algoritmos.size();
                var copia = new ArrayList<>(entrada);
                long inicio = System.nanoTime();
                ResultadoOrdenamiento resultado = algoritmos.get(indice).ordenar().apply(copia);
                long duracion = System.nanoTime() - inicio;
                mediciones.get(indice).add(duracion);
                resultados.set(indice, resultado);
                correctos[indice] &= esOrdenamientoCorrecto(resultado.terminosOrdenados(), elementosEsperados);
            }
        }

        var comparados = new ArrayList<AlgoritmoComparado>();
        long menorSuma = Long.MAX_VALUE;
        boolean todosCorrectos = true;
        for (int i = 0; i < algoritmos.size(); i++) {
            long suma = mediciones.get(i).stream().mapToLong(Long::longValue).sum();
            menorSuma = Math.min(menorSuma, suma);
            todosCorrectos &= correctos[i];
            var terminos = resultados.get(i).getTop(topK).stream()
                    .map(t -> new Termino(t.getTermino(), t.getFrecuencia())).toList();
            comparados.add(new AlgoritmoComparado(algoritmos.get(i).nombre(), terminos,
                    mediciones.get(i).stream().map(n -> n / 1_000_000.0).toList(),
                    suma / (REPETICIONES * 1_000_000.0), correctos[i]));
        }
        var ganadores = new ArrayList<String>();
        if (todosCorrectos) {
            for (int i = 0; i < algoritmos.size(); i++) {
                long suma = mediciones.get(i).stream().mapToLong(Long::longValue).sum();
                if (suma == menorSuma) ganadores.add(algoritmos.get(i).nombre());
            }
        }
        String resumen;
        if (!todosCorrectos) resumen = "Se detectaron resultados incorrectos. No se declara un ganador.";
        else if (ganadores.size() > 1) resumen = "Empate en el menor tiempo promedio: " + String.join(", ", ganadores) + ".";
        else resumen = ganadores.get(0) + " obtuvo el menor tiempo promedio en esta comparación.";
        return new Comparacion(totalArticulos, entrada.size(), topK, filtrarStopwords, REPETICIONES,
                List.copyOf(comparados), List.copyOf(ganadores), todosCorrectos, resumen);
    }

    private static Map<TerminoFrecuencia, Integer> contarElementos(List<TerminoFrecuencia> elementos) {
        var conteo = new HashMap<TerminoFrecuencia, Integer>();
        for (var elemento : elementos) conteo.merge(elemento, 1, Integer::sum);
        return conteo;
    }

    private static boolean esOrdenamientoCorrecto(List<TerminoFrecuencia> ordenados,
                                                   Map<TerminoFrecuencia, Integer> esperados) {
        if (!contarElementos(ordenados).equals(esperados)) return false;
        for (int i = 1; i < ordenados.size(); i++) {
            if (ordenados.get(i - 1).getFrecuencia() < ordenados.get(i).getFrecuencia()) return false;
        }
        return true;
    }

    private record Algoritmo(String nombre, Function<List<TerminoFrecuencia>, ResultadoOrdenamiento> ordenar) { }
    public record Termino(String termino, int frecuencia) { }
    public record AlgoritmoComparado(String algoritmo, List<Termino> terminos,
                                     List<Double> ejecucionesMs, double tiempoPromedioMs,
                                     boolean resultadoCorrecto) { }
    public record Comparacion(int totalArticulos, int totalElementos, int topK, boolean stopwordsFiltradas,
                               int repeticiones, List<AlgoritmoComparado> algoritmos, List<String> ganadores,
                               boolean resultadosEquivalentes, String resumen) { }
}
