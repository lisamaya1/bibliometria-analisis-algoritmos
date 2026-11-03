package com.uniquindio.bibliometria.ingesta;

import java.util.List;

/** Campos que los demas modulos pueden leer desde el archivo JSON. */
public record ArticuloJson(String titulo, List<String> autores,
                           String resumenOriginal, String resumenProcesado,
                           List<String> palabras) {
    public ArticuloJson {
        autores = List.copyOf(autores);
        palabras = List.copyOf(palabras);
    }
}
