package com.uniquindio.bibliometria.ingesta;

import java.util.List;
import java.util.Objects;

public record DocumentoPdf(String archivo, String tituloMetadatos, List<PaginaPdf> paginas) {
    public DocumentoPdf {
        Objects.requireNonNull(archivo, "archivo");
        tituloMetadatos = tituloMetadatos == null ? "" : tituloMetadatos;
        paginas = List.copyOf(paginas);
    }
}
