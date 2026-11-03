package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.algoritmos.similitud.clasicos.Levenshtein;
import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class LevenshteinService {
    private final PreprocesamientoService preprocesamiento;
    private final Levenshtein levenshtein = new Levenshtein();

    public LevenshteinService(PreprocesamientoService preprocesamiento) {
        this.preprocesamiento = preprocesamiento;
    }

    public Comparacion comparar(int articuloA, int articuloB) throws IOException {
        if (articuloA < 1 || articuloB < 1) {
            throw new IllegalArgumentException("Los identificadores de artículos deben ser positivos.");
        }
        var corpus = preprocesamiento.obtenerCorpus(PreprocesamientoService.RUTA_PDFS_DEFECTO,
                PreprocesamientoService.RUTA_JSON_DEFECTO);
        var a = buscar(corpus, articuloA);
        var b = buscar(corpus, articuloB);
        var resultado = levenshtein.comparar(a.abstractProcesado(), b.abstractProcesado());
        return new Comparacion(new Articulo(a.id(), a.titulo()), new Articulo(b.id(), b.titulo()),
                resultado, resultado.similitud() * 100.0);
    }

    private ArticuloDocumental buscar(List<ArticuloDocumental> corpus, int id) {
        for (var articulo : corpus) {
            if (articulo.id() == id) return articulo;
        }
        throw new IllegalArgumentException("No existe el artículo " + id + " en el corpus.");
    }

    public record Articulo(int id, String titulo) { }
    public record Comparacion(Articulo articuloA, Articulo articuloB,
                              Levenshtein.Resultado resultado, double porcentaje) { }
}
