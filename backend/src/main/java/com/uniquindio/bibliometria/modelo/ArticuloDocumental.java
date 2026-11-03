package com.uniquindio.bibliometria.modelo;

import java.util.List;
import java.util.Objects;


public record ArticuloDocumental(
        int id,
        String archivo,
        String titulo,
        List<String> autores,
        String abstractOriginal,
        String abstractProcesado,
        List<String> tokens,
        int totalTokens
) {
    public ArticuloDocumental {
        Objects.requireNonNull(archivo, "archivo no puede ser nulo");
        Objects.requireNonNull(titulo, "titulo no puede ser nulo");
        Objects.requireNonNull(abstractOriginal, "abstractOriginal no puede ser nulo");
        Objects.requireNonNull(abstractProcesado, "abstractProcesado no puede ser nulo");
        autores = autores == null ? List.of() : List.copyOf(autores);
        tokens = tokens == null ? List.of() : List.copyOf(tokens);
    }
}
