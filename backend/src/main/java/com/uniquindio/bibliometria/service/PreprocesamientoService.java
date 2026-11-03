package com.uniquindio.bibliometria.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.uniquindio.bibliometria.ingesta.ArticuloExtraido;
import com.uniquindio.bibliometria.ingesta.DocumentoPdf;
import com.uniquindio.bibliometria.ingesta.ExtractorArticulos;
import com.uniquindio.bibliometria.ingesta.LectorPdfBox;
import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.modelo.TerminoFrecuencia;
import com.uniquindio.bibliometria.preprocesamiento.ArticuloProcesado;
import com.uniquindio.bibliometria.preprocesamiento.OpcionesPreprocesamiento;
import com.uniquindio.bibliometria.preprocesamiento.ProcesadorResumen;
import com.uniquindio.bibliometria.util.JsonUtil;

/**
 * Servicio encargado del preprocesamiento del corpus documental:
 * 1. Extracción de metadatos (título, autores, abstract original) desde los PDFs.
 * 2. Normalización, limpieza y tokenización del abstract.
 * 3. Persistencia y carga en formato JSON centralizado.
 * 4. Extracción de frecuencias globales de términos del corpus.
 */
@Service
public class PreprocesamientoService {

    public static final Path RUTA_PDFS_DEFECTO = Path.of("datos", "pdfs_originales");
    public static final Path RUTA_JSON_DEFECTO = Path.of("datos", "procesados", "corpus_procesado.json");

    /**
      palabras funcionales que no aportan significado temático.
     */
    public static final Set<String> STOPWORDS_INGLES = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "also", "am", "an", "and", "any", "are",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by",
            "can", "could", "did", "do", "does", "doing", "down", "during", "each", "e", "g", "few", "for", "from",
            "further", "had", "has", "have", "having", "he", "her", "here", "hers", "herself", "him", "himself",
            "his", "how", "i", "if", "in", "into", "is", "it", "its", "itself", "just", "me", "more", "most",
            "my", "myself", "no", "nor", "not", "now", "of", "off", "on", "once", "only", "or", "other",
            "our", "ours", "ourselves", "out", "over", "own", "s", "same", "she", "should", "so", "some",
            "such", "t", "than", "that", "the", "their", "theirs", "them", "themselves", "then", "there",
            "these", "they", "this", "those", "through", "to", "too", "under", "until", "up", "very", "was",
            "we", "were", "what", "when", "where", "which", "while", "who", "whom", "why", "will", "with",
            "would", "you", "your", "yours", "yourself", "yourselves"
    );

    private final LectorPdfBox reader;
    private final ExtractorArticulos extractor;
    private final ProcesadorResumen preprocessor;

    public PreprocesamientoService() {
        this(new LectorPdfBox(), new ExtractorArticulos(), new ProcesadorResumen());
    }

    public PreprocesamientoService(LectorPdfBox reader, ExtractorArticulos extractor, ProcesadorResumen preprocessor) {
        this.reader = reader;
        this.extractor = extractor;
        this.preprocessor = preprocessor;
    }

    /**
     * Procesa los archivos PDF del directorio, construye los objetos {@link ArticuloDocumental}
     * y los guarda en el archivo JSON especificado.
     */
    public List<ArticuloDocumental> procesarYGuardarCorpus(Path rutaPdfs, Path rutaJson) throws IOException {
        if (!Files.isDirectory(rutaPdfs)) {
            throw new IllegalArgumentException("La ruta especificada no es un directorio: " + rutaPdfs);
        }

        List<Path> archivosPdf;
        try (var stream = Files.list(rutaPdfs)) {
            archivosPdf = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                    .sorted(Comparator.comparingInt(p -> ordenPorNumero(p.getFileName().toString())))
                    .toList();
        }

        if (archivosPdf.isEmpty()) {
            throw new IOException("No se encontraron archivos PDF en: " + rutaPdfs);
        }

        List<ArticuloDocumental> articulos = new ArrayList<>();
        OpcionesPreprocesamiento opciones = OpcionesPreprocesamiento.limpiezaMinima();

        for (int i = 0; i < archivosPdf.size(); i++) {
            Path pdfPath = archivosPdf.get(i);
            String nombreArchivo = pdfPath.getFileName().toString();
            byte[] bytes = Files.readAllBytes(pdfPath);

            DocumentoPdf doc = reader.leer(nombreArchivo, bytes);
            ArticuloExtraido extraido = extractor.extraer(doc);
            ArticuloProcesado procesado = preprocessor.procesar(extraido, opciones);

            ArticuloDocumental articulo = new ArticuloDocumental(
                    i + 1,
                    nombreArchivo,
                    extraido.titulo(),
                    extraido.autores(),
                    extraido.resumenOriginal(),
                    procesado.resumenProcesado(),
                    procesado.palabras(),
                    procesado.palabras().size()
            );

            articulos.add(articulo);
        }

        // Guardar en el archivo JSON centralizado
        JsonUtil.guardarArticulosEnJson(articulos, rutaJson);
        return articulos;
    }

    /**
     * Obtiene el corpus: si el JSON ya existe, lo carga directamente desde allí;
     * si no existe, procesa los PDFs y genera el JSON.
     */
    public List<ArticuloDocumental> obtenerCorpus(Path rutaPdfs, Path rutaJson) throws IOException {
        if (Files.exists(rutaJson)) {
            return JsonUtil.cargarArticulosDesdeJson(rutaJson);
        }
        return procesarYGuardarCorpus(rutaPdfs, rutaJson);
    }

    /**
     * Calcula la lista de frecuencias globales de todos los términos a través del corpus.
     *
     * @param articulos lista de artículos del corpus.
     * @param filtrarStopwords si es true, descarta palabras vacías comunes y términos de longitud &lt; 2.
     * @return lista no ordenada de {@link TerminoFrecuencia}.
     */
    public List<TerminoFrecuencia> extraerFrecuenciasGlobales(List<ArticuloDocumental> articulos, boolean filtrarStopwords) {
        Map<String, Integer> conteo = new HashMap<>();

        for (ArticuloDocumental art : articulos) {
            for (String token : art.tokens()) {
                String termino = token.trim().toLowerCase(Locale.ROOT);
                if (termino.isEmpty()) continue;

                if (filtrarStopwords) {
                    if (STOPWORDS_INGLES.contains(termino)) continue;
                    if (termino.length() <= 1) continue;
                    // Opcional: ignorar números puros en el análisis temático
                    if (termino.chars().allMatch(Character::isDigit)) continue;
                }

                conteo.put(termino, conteo.getOrDefault(termino, 0) + 1);
            }
        }

        List<TerminoFrecuencia> resultado = new ArrayList<>(conteo.size());
        for (Map.Entry<String, Integer> entrada : conteo.entrySet()) {
            resultado.add(new TerminoFrecuencia(entrada.getKey(), entrada.getValue()));
        }
        return resultado;
    }

    private static int ordenPorNumero(String nombre) {
        int fin = 0;
        while (fin < nombre.length() && Character.isDigit(nombre.charAt(fin))) {
            fin++;
        }
        try {
            return fin == 0 ? Integer.MAX_VALUE : Integer.parseInt(nombre.substring(0, fin));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
