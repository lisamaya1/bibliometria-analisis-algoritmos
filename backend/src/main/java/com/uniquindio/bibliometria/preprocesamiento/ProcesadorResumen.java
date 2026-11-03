package com.uniquindio.bibliometria.preprocesamiento;

import com.uniquindio.bibliometria.ingesta.ArticuloExtraido;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ProcesadorResumen {
    public ArticuloProcesado procesar(ArticuloExtraido articulo) {
        return procesar(articulo, OpcionesPreprocesamiento.limpiezaMinima());
    }

    public ArticuloProcesado procesar(ArticuloExtraido articulo, OpcionesPreprocesamiento opciones) {
        Objects.requireNonNull(articulo, "articulo");
        Objects.requireNonNull(opciones, "opciones");
        if (!articulo.tieneCamposCompletos()) {
            throw new IllegalArgumentException("Revise los campos faltantes antes de procesar el articulo");
        }
        String texto = opciones.reconstruirEspacios()
                ? combinarEspacios(articulo.resumenOriginal(), articulo.resumenConEspacios())
                : articulo.resumenOriginal();
        // Une palabras partidas antes de normalizar los saltos de linea.
        if (opciones.unirGuiones()) {
            texto = texto.replaceAll("(?<=[\\p{L}\\p{M}])[-\\u00ad\\u2010][\\t\\p{Zs}]*\\R[\\t\\p{Zs}]*(?=\\p{Ll})", "");
        }
        if (opciones.normalizarUnicode()) texto = NormalizadorTexto.caracteres(texto);
        if (opciones.normalizarEspacios()) {
            // Los saltos normales se convierten en un espacio.
            // Esto se aplica despues de unir las palabras cortadas por guion.
            texto = texto.replaceAll("\\R", " ")
                    .replaceAll("[\\s\\p{Z}]+", " ").strip();
        }
        if (opciones.minusculas()) texto = texto.toLowerCase(Locale.ROOT);
        return new ArticuloProcesado(articulo, texto, separarPalabras(texto), opciones);
    }

    /** Conserva los espacios de ambas lecturas sin cambiar las letras ni la puntuacion. */
    private String combinarEspacios(String original, String reconstruido) {
        StringBuilder resultado = new StringBuilder();
        int indiceOriginal = 0, indiceReconstruido = 0;
        while (indiceOriginal < original.length() || indiceReconstruido < reconstruido.length()) {
            int espacioOriginal = indiceOriginal, espacioReconstruido = indiceReconstruido;
            while (indiceOriginal < original.length() && esEspacio(original.codePointAt(indiceOriginal))) {
                indiceOriginal += Character.charCount(original.codePointAt(indiceOriginal));
            }
            while (indiceReconstruido < reconstruido.length()
                    && esEspacio(reconstruido.codePointAt(indiceReconstruido))) {
                indiceReconstruido += Character.charCount(reconstruido.codePointAt(indiceReconstruido));
            }
            // Conserva los saltos originales hasta resolver los guiones de fin de linea.
            if (indiceOriginal > espacioOriginal) resultado.append(original, espacioOriginal, indiceOriginal);
            else if (indiceReconstruido > espacioReconstruido) {
                resultado.append(reconstruido, espacioReconstruido, indiceReconstruido);
            }
            boolean finOriginal = indiceOriginal == original.length();
            boolean finReconstruido = indiceReconstruido == reconstruido.length();
            if (finOriginal || finReconstruido) {
                return finOriginal && finReconstruido ? resultado.toString() : original;
            }
            int codigo = original.codePointAt(indiceOriginal);
            if (codigo != reconstruido.codePointAt(indiceReconstruido)) {
                // Si cambian las letras, descarta la reconstruccion y conserva el original.
                return original;
            }
            resultado.appendCodePoint(codigo);
            indiceOriginal += Character.charCount(codigo);
            indiceReconstruido += Character.charCount(codigo);
        }
        return resultado.toString();
    }

    private boolean esEspacio(int codigo) {
        return Character.isWhitespace(codigo) || Character.isSpaceChar(codigo) || codigo == 0x85;
    }

    /** Recorre letras, numeros y marcas Unicode; conserva guiones y apostrofes internos. */
    public List<String> separarPalabras(String texto) {
        Objects.requireNonNull(texto, "texto");
        List<String> resultado = new ArrayList<>();
        StringBuilder palabra = new StringBuilder();
        for (int i = 0; i < texto.length();) {
            int codigo = texto.codePointAt(i);
            int siguienteIndice = i + Character.charCount(codigo);
            int tipo = Character.getType(codigo);
            boolean esPalabra = Character.isLetterOrDigit(codigo) || (palabra.length() > 0 &&
                    (tipo == Character.NON_SPACING_MARK || tipo == Character.COMBINING_SPACING_MARK));
            boolean esConector = (codigo == '-' || codigo == '\u2011' || codigo == '\'' || codigo == '\u2019')
                    && palabra.length() > 0 && siguienteIndice < texto.length()
                    && Character.isLetterOrDigit(texto.codePointAt(siguienteIndice));
            if (esPalabra || esConector) palabra.appendCodePoint(codigo);
            else if (palabra.length() > 0) {
                resultado.add(palabra.toString());
                palabra.setLength(0);
            }
            i = siguienteIndice;
        }
        if (palabra.length() > 0) resultado.add(palabra.toString());
        return List.copyOf(resultado);
    }
}
