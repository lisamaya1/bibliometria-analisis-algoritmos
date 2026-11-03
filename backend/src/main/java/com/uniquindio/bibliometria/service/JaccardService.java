package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.algoritmos.similitud.clasicos.Jaccard;
import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class JaccardService {
    private final PreprocesamientoService preprocesamiento;
    private final Jaccard jaccard = new Jaccard();

    public JaccardService(PreprocesamientoService preprocesamiento) {
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
        var resultado = jaccard.comparar(a.tokens(), b.tokens());
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
                              Jaccard.Resultado resultado, double porcentaje) { }
}
