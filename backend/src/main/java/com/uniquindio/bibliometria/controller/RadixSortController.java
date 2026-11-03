package com.uniquindio.bibliometria.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uniquindio.bibliometria.algoritmos.ordenamiento.RadixSort;
import com.uniquindio.bibliometria.algoritmos.ordenamiento.ResultadoOrdenamiento;
import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.modelo.TerminoFrecuencia;
import com.uniquindio.bibliometria.service.PreprocesamientoService;

/**
 * Controlador REST que expone el algoritmo RADIX SORT a través de HTTP,
 * incluyendo las 5 ejecuciones empíricas consecutivas y su promedio.
 *
 * Endpoint disponible:
 *   GET /api/radix-sort?topK=15&filtrarStopwords=true
 *
 * @author John
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Permite llamadas desde cualquier origen (React en localhost:5173)
public class RadixSortController {

    private final PreprocesamientoService preprocesamientoService;

    public RadixSortController() {
        this.preprocesamientoService = new PreprocesamientoService();
    }

    /**
     * Resuelve de forma robusta la ruta al JSON preprocesado del corpus.
     */
    private Path resolverRutaJson() {
        Path p1 = Path.of("datos", "procesados", "corpus_procesado.json");
        if (Files.exists(p1)) return p1;
        Path p2 = Path.of("..", "datos", "procesados", "corpus_procesado.json");
        if (Files.exists(p2)) return p2;
        return p1;
    }

    /**
     * Resuelve de forma robusta la ruta a los PDFs originales.
     */
    private Path resolverRutaPdfs() {
        Path p1 = Path.of("datos", "pdfs_originales");
        if (Files.exists(p1)) return p1;
        Path p2 = Path.of("..", "datos", "pdfs_originales");
        if (Files.exists(p2)) return p2;
        return p1;
    }

    /**
     * Ejecuta el algoritmo Radix Sort sobre el corpus documental (5 veces para medición empírica)
     * y devuelve el ranking de términos junto con los tiempos de cada ejecución y el promedio.
     *
     * @param topK             Cantidad de términos a incluir en el ranking (por defecto 15).
     * @param filtrarStopwords Si es true, excluye palabras vacías funcionales (por defecto true).
     * @return JSON con el resultado del ordenamiento, métricas y las 5 ejecuciones con promedio.
     */
    @GetMapping("/radix-sort")
    public ResponseEntity<RespuestaRadixSort> ejecutarRadixSort(
            @RequestParam(defaultValue = "15") int topK,
            @RequestParam(defaultValue = "true") boolean filtrarStopwords) {

        try {
            Path rutaPdfs = resolverRutaPdfs();
            Path rutaJson = resolverRutaJson();

            // 1. Obtener o cargar el corpus documental
            List<ArticuloDocumental> corpus = preprocesamientoService.obtenerCorpus(rutaPdfs, rutaJson);

            // 2. Extraer las frecuencias globales de términos
            List<TerminoFrecuencia> frecuencias = preprocesamientoService.extraerFrecuenciasGlobales(corpus, filtrarStopwords);

            // 3. Ejecutar Radix Sort 5 veces consecutivas para medición empírica real
            List<Double> ejecucionesMs = new ArrayList<>(5);
            double sumaTiempos = 0;
            ResultadoOrdenamiento resultado = null;

            for (int i = 0; i < 5; i++) {
                resultado = RadixSort.ordenarDescendente(frecuencias);
                double tMs = resultado.tiempoMilisegundos();
                ejecucionesMs.add(tMs);
                sumaTiempos += tMs;
            }
            double tiempoPromedioMs = sumaTiempos / 5.0;

            // 4. Mapear al Top K solicitado
            List<TerminoFrecuenciaDto> topTerminos = resultado.getTop(topK).stream()
                    .map(tf -> new TerminoFrecuenciaDto(tf.getTermino(), tf.getFrecuencia()))
                    .toList();

            // 5. Construir y retornar el DTO de respuesta con las 5 ejecuciones y promedio
            RespuestaRadixSort respuesta = new RespuestaRadixSort(
                    resultado.algoritmo(),
                    topTerminos,
                    resultado.tiempoMilisegundos(),
                    resultado.totalElementos(),
                    resultado.numeroCubetas(), // En Radix Sort: número de dígitos (d)
                    resultado.frecuenciaMinima(),
                    resultado.frecuenciaMaxima(),
                    topK,
                    filtrarStopwords,
                    corpus.size(),
                    ejecucionesMs,
                    tiempoPromedioMs
            );

            return ResponseEntity.ok(respuesta);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─── DTOs (Data Transfer Objects) ───────────────────────────────────────────

    public record TerminoFrecuenciaDto(String termino, int frecuencia) {}

    public record RespuestaRadixSort(
            String algoritmo,
            List<TerminoFrecuenciaDto> terminos,
            double tiempoMs,
            int totalElementos,
            int numDigitos,
            int frecuenciaMinima,
            int frecuenciaMaxima,
            int topK,
            boolean stopwordsFiltradas,
            int totalArticulos,
            List<Double> ejecucionesMs,
            double tiempoPromedioMs
    ) {}
}
