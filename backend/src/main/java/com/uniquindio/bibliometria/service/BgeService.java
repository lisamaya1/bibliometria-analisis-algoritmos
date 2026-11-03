package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.algoritmos.similitud.ia.*;
import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.List;

@Service
public class BgeService {
    private final PreprocesamientoService preprocesamiento;
    private final ModeloBge modelo;
    private final EmbeddingBge embedding;
    private final MatematicaEmbeddings matematica = new MatematicaEmbeddings();

    public BgeService(PreprocesamientoService preprocesamiento, ModeloBge modelo) {
        this.preprocesamiento = preprocesamiento;
        this.modelo = modelo;
        embedding = new EmbeddingBge(modelo);
    }

    public Comparacion comparar(int articuloA, int articuloB) throws IOException {
        if (articuloA < 1 || articuloB < 1) throw new IllegalArgumentException("Los identificadores de artículos deben ser positivos.");
        var corpus = preprocesamiento.obtenerCorpus(PreprocesamientoService.RUTA_PDFS_DEFECTO, PreprocesamientoService.RUTA_JSON_DEFECTO);
        var a = buscar(corpus, articuloA);
        var b = buscar(corpus, articuloB);
        var vectorA = embedding.generar(a.abstractProcesado());
        var vectorB = embedding.generar(b.abstractProcesado());
        var resultado = matematica.euclidiana(vectorA.vector(), vectorB.vector());
        return new Comparacion(resumir(a, vectorA), resumir(b, vectorB), ModeloBgeOnnx.MODELO,
                ModeloBgeOnnx.REVISION, modelo.dimension(), resultado);
    }

    private ArticuloDocumental buscar(List<ArticuloDocumental> corpus, int id) {
        for (var articulo : corpus) if (articulo.id() == id) return articulo;
        throw new IllegalArgumentException("No existe el artículo " + id + " en el corpus.");
    }

    private Articulo resumir(ArticuloDocumental articulo, EmbeddingBge.Resultado vector) {
        return new Articulo(articulo.id(), articulo.titulo(), vector.tokensContenido(), vector.fragmentos(),
                vector.fragmentos() > 1, vector.tokensPorFragmento());
    }

    public record Articulo(int id, String titulo, int tokensContenido, int fragmentos,
                            boolean fragmentado, List<Integer> tokensPorFragmento) { }
    public record Comparacion(Articulo articuloA, Articulo articuloB, String modelo, String revision,
                              int dimension, MatematicaEmbeddings.Euclidiana resultado) { }
}
