package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.algoritmos.similitud.clasicos.CosenoTfidf;
import com.uniquindio.bibliometria.algoritmos.similitud.clasicos.Dice;
import com.uniquindio.bibliometria.algoritmos.similitud.clasicos.Jaccard;
import com.uniquindio.bibliometria.algoritmos.similitud.clasicos.Levenshtein;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
public class ComparacionClasicaService {
    private final PreprocesamientoService preprocesamiento;
    private final Jaccard jaccard = new Jaccard();
    private final Dice dice = new Dice();
    private final Levenshtein levenshtein = new Levenshtein();
    private final CosenoTfidf coseno = new CosenoTfidf();

    public ComparacionClasicaService(PreprocesamientoService preprocesamiento) {
        this.preprocesamiento = preprocesamiento;
    }

    public Comparacion comparar(List<Integer> articulos) throws IOException {
        if (articulos == null) throw new IllegalArgumentException("Selecciona al menos dos artículos distintos.");
        var ids = new HashSet<Integer>();
        for (Integer id : articulos) {
            if (id == null || id < 1) throw new IllegalArgumentException("Los identificadores deben ser positivos.");
            ids.add(id);
        }
        if (ids.size() < 2) throw new IllegalArgumentException("Selecciona al menos dos artículos distintos.");
        var corpus = preprocesamiento.obtenerCorpus(PreprocesamientoService.RUTA_PDFS_DEFECTO,
                PreprocesamientoService.RUTA_JSON_DEFECTO);
        var indices = new ArrayList<Integer>();
        var seleccionados = new ArrayList<Articulo>();
        var encontrados = new HashSet<Integer>();
        var tokens = new ArrayList<List<String>>();
        // El orden del corpus hace independiente el resultado del orden de selección.
        for (int i = 0; i < corpus.size(); i++) {
            var articulo = corpus.get(i);
            tokens.add(articulo.tokens());
            if (ids.contains(articulo.id())) {
                indices.add(i);
                seleccionados.add(new Articulo(articulo.id(), articulo.titulo()));
                encontrados.add(articulo.id());
            }
        }
        for (Integer id : ids) {
            if (!encontrados.contains(id)) throw new IllegalArgumentException("No existe el artículo " + id + " en el corpus.");
        }
        var representacion = coseno.construirRepresentacion(tokens);
        var parejas = new ArrayList<Pareja>();
        for (int i = 0; i < indices.size(); i++) {
            for (int j = i + 1; j < indices.size(); j++) {
                int indiceA = indices.get(i);
                int indiceB = indices.get(j);
                var a = corpus.get(indiceA);
                var b = corpus.get(indiceB);
                try {
                    double valorJaccard = jaccard.comparar(a.tokens(), b.tokens()).similitud();
                    double valorDice = dice.comparar(a.tokens(), b.tokens()).similitud();
                    var edicion = levenshtein.comparar(a.abstractProcesado(), b.abstractProcesado());
                    double valorCoseno = coseno.compararRepresentacion(representacion, indiceA, indiceB).similitud();
                    parejas.add(new Pareja(seleccionados.get(i), seleccionados.get(j),
                            resumir(valorJaccard), resumir(valorDice),
                            new Edicion(edicion.distancia(), edicion.similitud(), edicion.similitud() * 100),
                            resumir(valorCoseno)));
                } catch (IllegalArgumentException error) {
                    throw new IllegalArgumentException("No se pudo comparar " + a.id() + " vs " + b.id() + ": " + error.getMessage(), error);
                }
            }
        }
        return new Comparacion(List.copyOf(seleccionados), parejas.size(), corpus.size(), List.copyOf(parejas));
    }

    private Similitud resumir(double valor) { return new Similitud(valor, valor * 100); }

    public record Articulo(int id, String titulo) { }
    public record Similitud(double similitud, double porcentaje) { }
    public record Edicion(int distancia, double similitud, double porcentaje) { }
    public record Pareja(Articulo articuloA, Articulo articuloB, Similitud jaccard,
                         Similitud dice, Edicion levenshtein, Similitud cosenoTfidf) { }
    public record Comparacion(List<Articulo> articulosSeleccionados, int totalParejas,
                               int totalDocumentosIdf, List<Pareja> parejas) { }
}
