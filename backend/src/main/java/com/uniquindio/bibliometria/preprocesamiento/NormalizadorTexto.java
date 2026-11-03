package com.uniquindio.bibliometria.preprocesamiento;

import java.text.Normalizer;

/** Convierte caracteres de presentacion del PDF sin quitar acentos ni simbolos tecnicos. */
public final class NormalizadorTexto {
    private NormalizadorTexto() { }

    public static String caracteres(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFC)
                .replaceAll("\\p{Zs}", " ")
                .replace('\u2010', '-').replace('\u2011', '-')
                // Las rayas separan frases; no deben unir dos palabras en un solo token.
                .replace("\u2013", " - ").replace("\u2014", " - ")
                .replace('\u2018', '\'').replace('\u2019', '\'')
                .replace('\u201c', '"').replace('\u201d', '"')
                .replace("\ufb00", "ff").replace("\ufb01", "fi").replace("\ufb02", "fl")
                .replace("\ufb03", "ffi").replace("\ufb04", "ffl")
                .replace("\ufb05", "st").replace("\ufb06", "st")
                .replace("\u00ad", "").replace("\ufeff", "").replace("\u2060", "")
                .replace('\u200b', ' ');
    }

    public static String unaLinea(String texto) {
        return caracteres(texto).replaceAll("[\\s\\p{Z}]+", " ").strip();
    }
}
