package com.uniquindio.bibliometria.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uniquindio.bibliometria.algoritmos.ordenamiento.BucketSort;
import com.uniquindio.bibliometria.algoritmos.ordenamiento.ResultadoOrdenamiento;
import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.modelo.TerminoFrecuencia;
import com.uniquindio.bibliometria.service.PreprocesamientoService;

/**
 * Controlador REST que expone el algoritmo Bucket Sort a través de HTTP.
 *
 * Endpoints disponibles:
 *   GET /api/bucket-sort?topK=15&filtrarStopwords=true
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")   // Permite llamadas desde cualquier origen (React en localhost:5173)
public class BucketSortController {

    private final PreprocesamientoService preprocesamientoService;

    public BucketSortController() {
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
     * Ejecuta el algoritmo Bucket Sort sobre el corpus y devuelve el ranking de términos
     * junto con los parámetros analíticos (n, B, min, max, tiempo).
     *
     * @param topK            Número de términos a retornar en el ranking (por defecto 15).
     * @param filtrarStopwords Si es true, excluye palabras vacías como "the", "and", etc. (por defecto true).
     * @return JSON con los resultados del ordenamiento: lista de términos, tiempo, cubetas, etc.
     */
    @GetMapping("/bucket-sort")
    public ResponseEntity<RespuestaBucketSort> ejecutarBucketSort(
            @RequestParam(defaultValue = "15") int topK,
            @RequestParam(defaultValue = "true") boolean filtrarStopwords) {

        try {
            Path rutaPdfs = resolverRutaPdfs();
            Path rutaJson = resolverRutaJson();

            // 1. Cargar el corpus desde el JSON ya generado (o procesarlo si no existe)
            List<ArticuloDocumental> corpus = preprocesamientoService.obtenerCorpus(rutaPdfs, rutaJson);

            // 2. Extraer frecuencias globales de los tokens del corpus
            List<TerminoFrecuencia> frecuencias = preprocesamientoService.extraerFrecuenciasGlobales(corpus, filtrarStopwords);

            // 3. Ordenar con Bucket Sort (implementación explícita, sin Collections.sort)
            ResultadoOrdenamiento resultado = BucketSort.ordenarDescendente(frecuencias);

            // 4. Construir la respuesta que enviará Spring Boot al frontend como JSON
            List<TerminoFrecuenciaDto> topTerminos = resultado.getTop(topK).stream()
                    .map(tf -> new TerminoFrecuenciaDto(tf.getTermino(), tf.getFrecuencia()))
                    .toList();

            RespuestaBucketSort respuesta = new RespuestaBucketSort(
                    resultado.algoritmo(),
                    topTerminos,
                    resultado.tiempoMilisegundos(),
                    resultado.totalElementos(),
                    resultado.numeroCubetas(),
                    resultado.frecuenciaMinima(),
                    resultado.frecuenciaMaxima(),
                    topK,
                    filtrarStopwords,
                    corpus.size()
            );

            return ResponseEntity.ok(respuesta);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─── DTOs (Data Transfer Objects) ───────────────────────────────────────────

    public record TerminoFrecuenciaDto(String termino, int frecuencia) {}

    public record RespuestaBucketSort(
            String algoritmo,
            List<TerminoFrecuenciaDto> terminos,
            double tiempoMs,
            int totalElementos,
            int numeroCubetas,
            int frecuenciaMinima,
            int frecuenciaMaxima,
            int topK,
            boolean stopwordsFiltradas,
            int totalArticulos
    ) {}
}
