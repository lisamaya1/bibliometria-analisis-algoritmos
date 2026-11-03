package com.uniquindio.bibliometria.ingesta;

import java.util.List;
import java.util.Objects;

/** Guarda los datos extraidos y sus observaciones; no certifica una revision manual. */
public record ArticuloExtraido(String archivo, String titulo, List<String> autores,
                               String resumenOriginal, List<String> observaciones,
                               String resumenConEspacios) {
    public ArticuloExtraido(String archivo, String titulo, List<String> autores,
                            String resumenOriginal, List<String> observaciones) {
        this(archivo, titulo, autores, resumenOriginal, observaciones, resumenOriginal);
    }

    public ArticuloExtraido {
        Objects.requireNonNull(archivo, "archivo");
        Objects.requireNonNull(titulo, "titulo");
        Objects.requireNonNull(resumenOriginal, "resumenOriginal");
        Objects.requireNonNull(resumenConEspacios, "resumenConEspacios");
        autores = List.copyOf(autores);
        observaciones = List.copyOf(observaciones);
    }

    public boolean tieneCamposCompletos() {
        return !titulo.isBlank() && !autores.isEmpty() && !resumenOriginal.isBlank();
    }
}
