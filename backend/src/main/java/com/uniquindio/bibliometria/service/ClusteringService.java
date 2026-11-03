package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.algoritmos.agrupamiento.MatrizDistancias;
import com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace.SingleLinkage;
import com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace.CompleteLinkage;
import com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace.AglomeradorJerarquico;
import com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace.AverageLinkage;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.List;

@Service
public class ClusteringService {
    private final PreprocesamientoService preprocesamiento;
    public ClusteringService(PreprocesamientoService preprocesamiento) { this.preprocesamiento = preprocesamiento; }

    public Comparacion single() throws IOException { return agrupar("single", new SingleLinkage()); }
    public Comparacion complete() throws IOException { return agrupar("complete", new CompleteLinkage()); }
    public Comparacion average() throws IOException { return agrupar("average", new AverageLinkage()); }

    public com.uniquindio.bibliometria.algoritmos.agrupamiento.EvaluadorClustering.Resultado evaluar() throws IOException {
        var corpus = preprocesamiento.obtenerCorpus(PreprocesamientoService.RUTA_PDFS_DEFECTO,
                PreprocesamientoService.RUTA_JSON_DEFECTO);
        var matriz = MatrizDistancias.desdeCorpus(corpus);
        return new com.uniquindio.bibliometria.algoritmos.agrupamiento.EvaluadorClustering().evaluar(matriz);
    }

    private Comparacion agrupar(String criterio, AglomeradorJerarquico algoritmo) throws IOException {
        var corpus = preprocesamiento.obtenerCorpus(PreprocesamientoService.RUTA_PDFS_DEFECTO,
                PreprocesamientoService.RUTA_JSON_DEFECTO);
        var matriz = MatrizDistancias.desdeCorpus(corpus);
        var resultado = algoritmo.agrupar(matriz);
        var articulos = corpus.stream().map(a -> new Articulo(a.id(), a.titulo())).toList();
        return new Comparacion(criterio, corpus.size(), "1 - coseno TF-IDF", articulos,
                resultado.raiz().id(), resultado.fusiones());
    }

    public record Articulo(int id, String titulo) { }
    /** Historial plano: los IDs de hijos apuntan a hojas o fusiones anteriores. */
    public record Comparacion(String criterio, int cantidadArticulos, String distanciaBase,
                              List<Articulo> articulos, int raiz, List<AglomeradorJerarquico.Fusion> fusiones) { }
}
