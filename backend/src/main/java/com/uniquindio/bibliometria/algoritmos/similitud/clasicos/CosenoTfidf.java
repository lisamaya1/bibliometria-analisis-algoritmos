package com.uniquindio.bibliometria.algoritmos.similitud.clasicos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** TF crudo, IDF = ln(N/DF) y coseno calculados con recorridos explícitos. */
public final class CosenoTfidf {
    public record DocumentoVectorizado(List<Integer> frecuencias, List<Double> pesos) {
        public DocumentoVectorizado {
            frecuencias = List.copyOf(frecuencias);
            pesos = List.copyOf(pesos);
        }
    }

    /** Disponible para inspección matemática; no se devuelve por el endpoint normal. */
    public record Representacion(List<String> vocabulario, List<Integer> df, List<Double> idf,
                                 List<DocumentoVectorizado> documentos) {
        public Representacion {
            vocabulario = List.copyOf(vocabulario);
            df = List.copyOf(df);
            idf = List.copyOf(idf);
            documentos = List.copyOf(documentos);
        }
    }

    public record Resultado(int totalDocumentos, int tamanoVocabulario, double productoPunto,
                            double normaA, double normaB, double similitud) { }

    public Resultado comparar(List<List<String>> corpus, int indiceA, int indiceB) {
        Objects.requireNonNull(corpus, "El corpus no puede ser nulo.");
        if (indiceA < 0 || indiceB < 0 || indiceA >= corpus.size() || indiceB >= corpus.size()) {
            throw new IllegalArgumentException("Los índices deben corresponder a documentos del corpus.");
        }
        Representacion representacion = construirRepresentacion(corpus);
        return compararRepresentacion(representacion, indiceA, indiceB);
    }

    /** Reutiliza los mismos pesos del corpus completo en una comparación múltiple. */
    public Resultado compararRepresentacion(Representacion representacion, int indiceA, int indiceB) {
        Objects.requireNonNull(representacion, "La representación no puede ser nula.");
        int totalDocumentos = representacion.documentos().size();
        if (indiceA < 0 || indiceB < 0 || indiceA >= totalDocumentos || indiceB >= totalDocumentos) {
            throw new IllegalArgumentException("Los índices deben corresponder a documentos del corpus.");
        }
        List<Double> a = representacion.documentos().get(indiceA).pesos();
        List<Double> b = representacion.documentos().get(indiceB).pesos();
        double productoPunto = 0;
        double sumaCuadradosA = 0;
        double sumaCuadradosB = 0;
        for (int i = 0; i < representacion.vocabulario().size(); i++) {
            double pesoA = a.get(i);
            double pesoB = b.get(i);
            productoPunto += pesoA * pesoB;
            sumaCuadradosA += pesoA * pesoA;
            sumaCuadradosB += pesoB * pesoB;
        }
        double normaA = Math.sqrt(sumaCuadradosA);
        double normaB = Math.sqrt(sumaCuadradosB);
        if (normaA == 0 || normaB == 0) {
            throw new IllegalArgumentException("El coseno no está definido: algún artículo tiene norma TF-IDF cero. "
                    + "Puede estar vacío o contener solo términos presentes en todos los documentos.");
        }
        double similitud = productoPunto / (normaA * normaB);
        // Los pesos son no negativos. Acota únicamente el posible redondeo numérico.
        similitud = Math.max(0.0, Math.min(1.0, similitud));
        return new Resultado(totalDocumentos, representacion.vocabulario().size(),
                productoPunto, normaA, normaB, similitud);
    }

    public Representacion construirRepresentacion(List<List<String>> corpus) {
        Objects.requireNonNull(corpus, "El corpus no puede ser nulo.");
        if (corpus.isEmpty()) throw new IllegalArgumentException("El corpus no puede estar vacío.");
        var vocabularioUnico = new LinkedHashSet<String>();
        var conteos = new ArrayList<Map<String, Integer>>();
        for (List<String> documento : corpus) {
            Objects.requireNonNull(documento, "Un documento no puede ser nulo.");
            var frecuencias = new HashMap<String, Integer>();
            for (String termino : documento) {
                Objects.requireNonNull(termino, "Un término no puede ser nulo.");
                if (termino.isBlank()) throw new IllegalArgumentException("No se admiten términos en blanco.");
                vocabularioUnico.add(termino);
                frecuencias.put(termino, frecuencias.getOrDefault(termino, 0) + 1);
            }
            conteos.add(frecuencias);
        }
        var vocabulario = new ArrayList<>(vocabularioUnico);
        var df = new ArrayList<Integer>();
        var idf = new ArrayList<Double>();
        int n = corpus.size();
        for (String termino : vocabulario) {
            int documentosConTermino = 0;
            for (Map<String, Integer> conteo : conteos) {
                if (conteo.containsKey(termino)) documentosConTermino++;
            }
            df.add(documentosConTermino);
            idf.add(Math.log((double) n / documentosConTermino));
        }
        var documentos = new ArrayList<DocumentoVectorizado>();
        for (Map<String, Integer> conteo : conteos) {
            var frecuencias = new ArrayList<Integer>();
            var pesos = new ArrayList<Double>();
            for (int i = 0; i < vocabulario.size(); i++) {
                int tf = conteo.getOrDefault(vocabulario.get(i), 0);
                frecuencias.add(tf);
                pesos.add(tf * idf.get(i));
            }
            documentos.add(new DocumentoVectorizado(frecuencias, pesos));
        }
        return new Representacion(vocabulario, df, idf, documentos);
    }
}
