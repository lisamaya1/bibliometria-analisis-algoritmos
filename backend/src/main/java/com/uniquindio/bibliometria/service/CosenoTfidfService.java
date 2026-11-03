package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.algoritmos.similitud.clasicos.CosenoTfidf;
import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class CosenoTfidfService {
    private final PreprocesamientoService preprocesamiento;
    private final CosenoTfidf cosenoTfidf = new CosenoTfidf();

    public CosenoTfidfService(PreprocesamientoService preprocesamiento) {
        this.preprocesamiento = preprocesamiento;
    }

    public Comparacion comparar(int articuloA, int articuloB) throws IOException {
        if (articuloA < 1 || articuloB < 1) {
            throw new IllegalArgumentException("Los identificadores de artículos deben ser positivos.");
        }
        var corpus = preprocesamiento.obtenerCorpus(PreprocesamientoService.RUTA_PDFS_DEFECTO,
                PreprocesamientoService.RUTA_JSON_DEFECTO);
        int indiceA = buscarIndice(corpus, articuloA);
        int indiceB = buscarIndice(corpus, articuloB);
        var tokens = new java.util.ArrayList<List<String>>();
        for (var articulo : corpus) tokens.add(articulo.tokens());
        var resultado = cosenoTfidf.comparar(tokens, indiceA, indiceB);
        var a = corpus.get(indiceA);
        var b = corpus.get(indiceB);
        return new Comparacion(new Articulo(a.id(), a.titulo()), new Articulo(b.id(), b.titulo()),
                resultado, resultado.similitud() * 100.0);
    }

    private int buscarIndice(List<ArticuloDocumental> corpus, int id) {
        for (int i = 0; i < corpus.size(); i++) {
            if (corpus.get(i).id() == id) return i;
        }
        throw new IllegalArgumentException("No existe el artículo " + id + " en el corpus.");
    }

    public record Articulo(int id, String titulo) { }
    public record Comparacion(Articulo articuloA, Articulo articuloB,
                              CosenoTfidf.Resultado resultado, double porcentaje) { }
}
