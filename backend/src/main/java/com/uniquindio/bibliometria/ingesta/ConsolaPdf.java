package com.uniquindio.bibliometria.ingesta;

import com.uniquindio.bibliometria.preprocesamiento.ProcesadorResumen;
import com.uniquindio.bibliometria.preprocesamiento.OpcionesPreprocesamiento;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipFile;

/** Permite inspeccionar los PDF y exportar los datos extraidos a JSON. */
public final class ConsolaPdf {
    private ConsolaPdf() { }

    public static void main(String[] args) {
        try {
            ejecutar(args, System.out);
        } catch (IOException | IllegalArgumentException error) {
            System.err.println("Error: " + error.getMessage());
            System.exit(1);
        }
    }

    public static void ejecutar(String[] args, PrintStream salida) throws IOException {
        if (args.length < 2 || !(args[0].equals("inspeccionar") || args[0].equals("exportar"))) {
            throw new IllegalArgumentException("Uso: inspeccionar <ZIP/carpeta> [numero de articulo] "
                    + "o exportar <ZIP/carpeta> <archivo.json> "
                    + "[--conservar-texto] [--espacios] [--minusculas] [--unicode] [--unir-guiones] [--reconstruir-espacios]");
        }
        boolean exportar = args[0].equals("exportar");
        if (exportar && (args.length < 3 || args[2].startsWith("--"))) {
            throw new IllegalArgumentException("Indique el archivo JSON de salida");
        }
        var predeterminadas = OpcionesPreprocesamiento.limpiezaMinima();
        boolean espacios = predeterminadas.normalizarEspacios(), minusculas = predeterminadas.minusculas();
        boolean unicode = predeterminadas.normalizarUnicode(), guiones = predeterminadas.unirGuiones();
        boolean separacion = predeterminadas.reconstruirEspacios();
        boolean inspeccionarArticulo = !exportar && args.length > 2 && !args[2].startsWith("--");
        for (int i = exportar || inspeccionarArticulo ? 3 : 2; i < args.length; i++) {
            switch (args[i]) {
                case "--conservar-texto" -> {
                    espacios = false;
                    minusculas = false;
                    unicode = false;
                    guiones = false;
                    separacion = false;
                }
                case "--espacios" -> espacios = true;
                case "--minusculas" -> minusculas = true;
                case "--unicode" -> unicode = true;
                case "--unir-guiones" -> guiones = true;
                case "--reconstruir-espacios" -> separacion = true;
                default -> throw new IllegalArgumentException("Opcion desconocida: " + args[i]);
            }
        }
        List<Integer> solicitados = inspeccionarArticulo ? List.of(numeroArticulo(args[2])) : List.of();
        var cargado = cargarSeleccion(Path.of(args[1]), solicitados, new ProcesadorDocumentos(
                new LectorPdfBox(), new ExtractorArticulos(), new ProcesadorResumen()));
        List<ArticuloExtraido> articulos = cargado.articulos();
        var opciones = new OpcionesPreprocesamiento(espacios, minusculas, unicode, guiones, separacion);
        if (exportar) {
            Path destino = Path.of(args[2]);
            new ArchivoArticulosJson().guardar(articulos, destino, opciones);
            salida.println("Articulos guardados: " + articulos.size());
            salida.println("Archivo JSON: " + destino.toAbsolutePath().normalize());
            return;
        }
        salida.println("PDF en el corpus: " + cargado.totalDocumentos() + "; leidos: "
                + cargado.documentosLeidos());
        if (inspeccionarArticulo) mostrarArticulo(articulos.get(0), opciones, salida);
        else for (int i = 0; i < articulos.size(); i++) {
            var a = articulos.get(i);
            salida.printf("%02d | %s | autores=%d | resumen=%d caracteres | %s%n", i + 1,
                    a.titulo().isBlank() ? a.archivo() : a.titulo(), a.autores().size(),
                    a.resumenOriginal().length(), a.tieneCamposCompletos() ? "REVISAR" : "FALTAN CAMPOS");
        }
    }

    public static List<ArticuloExtraido> cargarCorpus(Path ruta) throws IOException {
        var procesadorDocumentos = new ProcesadorDocumentos(new LectorPdfBox(), new ExtractorArticulos(),
                new ProcesadorResumen());
        return cargarSeleccion(ruta, List.of(), procesadorDocumentos).articulos();
    }

    record CorpusCargado(int totalDocumentos, int documentosLeidos, List<ArticuloExtraido> articulos) {
        CorpusCargado { articulos = List.copyOf(articulos); }
    }

    /** Ordena los archivos y lee solo los documentos solicitados. */
    static CorpusCargado cargarSeleccion(Path ruta, List<Integer> solicitados, ProcesadorDocumentos procesadorDocumentos)
            throws IOException {
        List<ArticuloExtraido> resultado = new ArrayList<>();
        var leidos = new HashMap<Integer, ArticuloExtraido>();
        int total;
        if (Files.isDirectory(ruta)) {
            try (var archivos = Files.list(ruta)) {
                var entradas = archivos.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString(), ORDEN_ARCHIVOS)).toList();
                total = entradas.size();
                for (int indice : numerosSeleccionados(total, solicitados, ruta)) {
                    if (!leidos.containsKey(indice)) {
                        Path pdf = entradas.get(indice - 1);
                        leidos.put(indice, extraerConRevision(procesadorDocumentos, pdf.toString(), Files.readAllBytes(pdf)));
                    }
                    resultado.add(leidos.get(indice));
                }
            }
        } else {
            try (var zip = new ZipFile(ruta.toFile())) {
                var entradas = zip.stream().filter(e -> !e.isDirectory()
                        && e.getName().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                        .sorted(Comparator.comparing(e -> e.getName(), ORDEN_ARCHIVOS)).toList();
                total = entradas.size();
                for (int indice : numerosSeleccionados(total, solicitados, ruta)) {
                    if (!leidos.containsKey(indice)) {
                        var entrada = entradas.get(indice - 1);
                        try (var stream = zip.getInputStream(entrada)) {
                            leidos.put(indice, extraerConRevision(procesadorDocumentos, entrada.getName(), stream.readAllBytes()));
                        }
                    }
                    resultado.add(leidos.get(indice));
                }
            }
        }
        return new CorpusCargado(total, leidos.size(), resultado);
    }

    private static List<Integer> numerosSeleccionados(int total, List<Integer> solicitados, Path ruta)
            throws IOException {
        if (total == 0) throw new IOException("No se encontraron PDF en " + ruta);
        if (solicitados.isEmpty()) return java.util.stream.IntStream.rangeClosed(1, total).boxed().toList();
        for (int indice : solicitados) {
            if (indice < 1 || indice > total) throw new IllegalArgumentException("Numero de articulo fuera de rango: " + indice);
        }
        return solicitados;
    }

    private static ArticuloExtraido extraerConRevision(ProcesadorDocumentos procesadorDocumentos, String archivo, byte[] pdf) {
        try {
            return procesadorDocumentos.extraer(archivo, pdf);
        } catch (IOException error) {
            return new ArticuloExtraido(archivo, "", List.of(), "",
                    List.of("No se pudo leer el PDF: " + error.getMessage()));
        }
    }

    private static int numeroArticulo(String numero) {
        int indice;
        try { indice = Integer.parseInt(numero); }
        catch (NumberFormatException error) { throw new IllegalArgumentException("Numero de articulo invalido: " + numero); }
        if (indice < 1) throw new IllegalArgumentException("Numero de articulo fuera de rango: " + indice);
        return indice;
    }

    private static void mostrarArticulo(ArticuloExtraido articulo, OpcionesPreprocesamiento opciones, PrintStream salida) {
        salida.println("Archivo: " + articulo.archivo());
        salida.println("Titulo: " + articulo.titulo());
        salida.println("Autores: " + articulo.autores());
        salida.println("Resumen original:\n" + articulo.resumenOriginal());
        if (articulo.tieneCamposCompletos()) {
            var procesado = new ProcesadorResumen().procesar(articulo, opciones);
            salida.println("Opciones de limpieza: " + opciones);
            salida.println("Resumen procesado:\n" + procesado.resumenProcesado());
            salida.println("Cantidad de palabras: " + procesado.palabras().size());
            salida.println("Palabras: " + mostrarPalabras(procesado.palabras()));
        } else salida.println("No se puede procesar el resumen: faltan campos.");
        if (!articulo.observaciones().isEmpty()) {
            salida.println("Observaciones: " + articulo.observaciones());
        }
    }

    static String mostrarPalabras(List<String> palabras) {
        return palabras.stream().map(palabra -> "\"" + palabra.replace("\\", "\\\\")
                        .replace("\"", "\\\"") + "\"")
                .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
    }

    private static final Comparator<String> ORDEN_ARCHIVOS = Comparator
            .comparingInt(ConsolaPdf::numeroArchivo).thenComparing(Comparator.naturalOrder());

    private static int numeroArchivo(String archivo) {
        String nombre = archivo.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1);
        int fin = 0;
        while (fin < nombre.length() && Character.isDigit(nombre.charAt(fin))) fin++;
        try { return fin == 0 ? Integer.MAX_VALUE : Integer.parseInt(nombre.substring(0, fin)); }
        catch (NumberFormatException error) { return Integer.MAX_VALUE; }
    }
}
