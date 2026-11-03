package com.uniquindio.bibliometria.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.service.PreprocesamientoService;

/**
 * Controlador REST que expone la información general del corpus documental
 * y los artículos científicos utilizados en el estudio.
 *
 * Endpoints disponibles:
 *   GET /api/articulos
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Permite llamadas desde cualquier origen (React en localhost:5173)
public class InformacionController {

    private final PreprocesamientoService preprocesamientoService;

    public InformacionController() {
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
     * Retorna la lista de todos los artículos científicos utilizados en el estudio.
     */
    @GetMapping("/articulos")
    public ResponseEntity<List<ArticuloResumenDto>> obtenerArticulos() {
        try {
            Path rutaPdfs = resolverRutaPdfs();
            Path rutaJson = resolverRutaJson();

            List<ArticuloDocumental> corpus = preprocesamientoService.obtenerCorpus(rutaPdfs, rutaJson);
            List<ArticuloResumenDto> dtos = corpus.stream()
                    .map(a -> new ArticuloResumenDto(
                            a.id(),
                            a.archivo(),
                            a.titulo(),
                            a.autores(),
                            a.abstractOriginal(),
                            a.totalTokens()
                    ))
                    .toList();
            return ResponseEntity.ok(dtos);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─── DTO (Data Transfer Object) ─────────────────────────────────────────────

    /**
     * DTO para los artículos del estudio mostrados en la ventana de información.
     */
    public record ArticuloResumenDto(
            int id,
            String archivo,
            String titulo,
            List<String> autores,
            String resumenOriginal,
            int totalPalabras
    ) {}
}
