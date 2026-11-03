package com.uniquindio.bibliometria.ingesta;

import com.uniquindio.bibliometria.preprocesamiento.NormalizadorTexto;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Identifica los campos por su posicion en el PDF y avisa si falta informacion. */
public final class ExtractorArticulos {
    private static final Pattern ENCABEZADO_RESUMEN = Pattern.compile(
            "(?i)^\\s*a\\s*b\\s*s\\s*t\\s*r\\s*a\\s*c\\s*t\\s*(?::\\s*(.*))?$");
    private static final Pattern LIMITE_RESUMEN = Pattern.compile(
            "(?i)^(?:k\\s*e\\s*y\\s*w\\s*o\\s*r\\s*d\\s*s?\\b.*|(?:\\d+[.\\s|]+)?introduction\\s*|paper\\s*type\\b.*)$");
    private static final Pattern AFILIACION = Pattern.compile(
            "(?i).*(department|university|institute|faculty|college|school of|correspondence|accepted:|published online:|copyright|author\\(s\\)).*");

    public ArticuloExtraido extraer(DocumentoPdf documento) {
        List<String> notas = new ArrayList<>();
        if (documento.paginas().isEmpty()) return camposAusentes(documento.archivo(), notas);
        PaginaPdf pagina = documento.paginas().get(0);
        List<PaginaPdf.Linea> lineas = pagina.lineas();
        PaginaPdf.Linea encabezado = lineas.stream().filter(l -> ENCABEZADO_RESUMEN.matcher(l.texto()).matches())
                .findFirst().orElse(null);
        if (encabezado == null) {
            notas.add("No se encontro el encabezado del resumen en la primera pagina; revise el PDF.");
        }
        float posicionResumen = encabezado == null ? pagina.alto() : encabezado.y();
        List<PaginaPdf.Linea> lineasTitulo = lineasTitulo(documento.tituloMetadatos(), lineas, posicionResumen);
        String titulo = NormalizadorTexto.unaLinea(unirLineas(lineasTitulo, " "));
        if (titulo.isBlank()) notas.add("No se identifico el titulo.");
        if (!documento.tituloMetadatos().isBlank() && !clave(titulo).equals(clave(documento.tituloMetadatos()))) {
            notas.add("El titulo visible difiere de los metadatos; revise el titulo visible.");
        }
        List<String> autores = autores(lineas, lineasTitulo, posicionResumen);
        if (autores.isEmpty()) notas.add("No se identificaron los autores.");
        String textoResumen = encabezado == null ? "" : textoResumen(lineas, encabezado, notas);
        // Usa la misma region del resumen; solo cambia la reconstruccion de espacios.
        String reconstruido = encabezado == null ? "" : textoResumen(lineas, encabezado, new ArrayList<>(), true);
        if (textoResumen.isBlank()) notas.add("No se identifico el contenido del resumen.");
        if (titulo.indexOf('\ufffd') >= 0 || textoResumen.indexOf('\ufffd') >= 0
                || autores.stream().anyMatch(a -> a.indexOf('\ufffd') >= 0)) {
            notas.add("Hay caracteres sin correspondencia Unicode; revise el PDF visible.");
        }
        return new ArticuloExtraido(documento.archivo(), titulo, autores, textoResumen, notas, reconstruido);
    }

    private ArticuloExtraido camposAusentes(String archivo, List<String> notas) {
        return new ArticuloExtraido(archivo, "", List.of(), "", notas);
    }

    private List<PaginaPdf.Linea> lineasTitulo(String metadatos, List<PaginaPdf.Linea> lineas, float antesDeY) {
        String claveMetadatos = clave(metadatos);
        List<PaginaPdf.Linea> candidatos = lineas.stream().filter(l -> l.y() < antesDeY
                && clave(l.texto()).length() > 15).toList();
        PaginaPdf.Linea inicio = candidatos.stream().filter(l -> {
            String k = clave(l.texto());
            return !claveMetadatos.isEmpty() && claveMetadatos.startsWith(k.substring(0, Math.min(30, k.length())));
        }).max(Comparator.comparing(PaginaPdf.Linea::tamanoFuente)).orElse(null);
        if (inicio == null) {
            inicio = candidatos.stream().filter(l -> !l.texto().contains("http")
                    && !l.texto().toLowerCase(Locale.ROOT).contains("journal homepage"))
                    .max(Comparator.comparing(PaginaPdf.Linea::tamanoFuente)).orElse(null);
        }
        if (inicio == null) return List.of();
        List<PaginaPdf.Linea> resultado = new ArrayList<>();
        for (PaginaPdf.Linea linea : lineas) {
            if (linea.y() < inicio.y() - 1) continue;
            if (linea.y() >= antesDeY) break;
            if (Math.abs(linea.tamanoFuente() - inicio.tamanoFuente()) < .8f
                    && (resultado.isEmpty() || linea.y() - resultado.get(resultado.size()-1).y() <= 35)) {
                resultado.add(linea);
            } else if (!resultado.isEmpty() && linea.y() - resultado.get(resultado.size()-1).y() > 10) {
                break;
            }
        }
        return resultado;
    }

    private List<String> autores(List<PaginaPdf.Linea> lineas, List<PaginaPdf.Linea> titulo, float posicionResumen) {
        if (titulo.isEmpty()) return List.of();
        float limiteInferior = titulo.get(titulo.size()-1).y();
        List<PaginaPdf.Linea> candidatos = lineas.stream().filter(l -> l.y() > limiteInferior + 3
                && l.y() < Math.min(posicionResumen, limiteInferior + 65)
                && clave(l.texto()).length() > 8 && !AFILIACION.matcher(l.texto()).matches()).toList();
        PaginaPdf.Linea primero = candidatos.stream().max(Comparator.comparing(PaginaPdf.Linea::tamanoFuente))
                .orElse(null);
        if (primero == null) return List.of();
        List<PaginaPdf.Linea> bloque = new ArrayList<>();
        for (PaginaPdf.Linea linea : candidatos) {
            if (linea.y() >= primero.y() - 2 && linea.y() <= primero.y() + 22
                    && Math.abs(linea.tamanoFuente() - primero.tamanoFuente()) < .8f) bloque.add(linea);
        }
        String texto = NormalizadorTexto.unaLinea(unirLineas(bloque, " ")).replaceAll("[\\d*]+", "")
                .replaceAll("(?<=\\s)[a-f](?=\\s*[,;]|$)", "")
                .replaceAll("[\\s\\p{Z}]+", " ");
        List<String> resultado = new ArrayList<>();
        for (String autor : texto.split("\\s*(?:[,;|\\u00b7]|\\band\\b)\\s*")) {
            autor = autor.strip();
            if (autor.codePoints().filter(Character::isLetter).count() >= 4) resultado.add(autor);
        }
        return resultado;
    }

    private String textoResumen(List<PaginaPdf.Linea> lineas, PaginaPdf.Linea encabezado, List<String> notas) {
        return textoResumen(lineas, encabezado, notas, false);
    }

    private String textoResumen(List<PaginaPdf.Linea> lineas, PaginaPdf.Linea encabezado, List<String> notas,
                                boolean reconstruirSeparacion) {
        List<String> cuerpo = new ArrayList<>();
        var coincidencia = ENCABEZADO_RESUMEN.matcher(encabezado.texto());
        coincidencia.matches();
        if (coincidencia.group(1) != null && !coincidencia.group(1).isBlank()) {
            var encabezadoReconstruido = ENCABEZADO_RESUMEN.matcher(encabezado.textoReconstruido());
            String primeraLinea = reconstruirSeparacion && encabezadoReconstruido.matches()
                    ? encabezadoReconstruido.group(1) : coincidencia.group(1);
            cuerpo.add(primeraLinea == null ? coincidencia.group(1) : primeraLinea);
        }
        float ultimaY = encabezado.y();
        float interlineado = 0;
        boolean delimitado = false;
        for (PaginaPdf.Linea linea : lineas) {
            if (linea.y() <= encabezado.y() + 2) continue;
            boolean mismaColumna = Math.abs(linea.x() - encabezado.x()) <= 15;
            if (LIMITE_RESUMEN.matcher(linea.texto()).matches()
                    && (mismaColumna || (!cuerpo.isEmpty() && linea.y() - ultimaY > 15))) {
                delimitado = true;
                break;
            }
            if (!mismaColumna) continue;
            if (!cuerpo.isEmpty() && linea.y() - ultimaY > Math.max(22, interlineado * 1.9f)) {
                notas.add("El final del resumen se dedujo por un espacio vertical; revise el limite.");
                delimitado = true;
                break;
            }
            if (!cuerpo.isEmpty()) interlineado = linea.y() - ultimaY;
            cuerpo.add(reconstruirSeparacion ? linea.textoReconstruido() : linea.texto());
            ultimaY = linea.y();
        }
        if (!delimitado) notas.add("No se encontro un final explicito del resumen; revise que este completo.");
        // Conserva las lineas originales; la limpieza se aplica despues.
        return String.join("\n", cuerpo);
    }

    private String unirLineas(List<PaginaPdf.Linea> lineas, String separador) {
        return String.join(separador, lineas.stream().map(PaginaPdf.Linea::texto).toList());
    }

    private String clave(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
