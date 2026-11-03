package com.uniquindio.bibliometria.ingesta;

import java.util.List;

/** Conserva el texto y la posicion de las lineas de una pagina. Las medidas estan en puntos. */
public record PaginaPdf(int numero, float ancho, float alto, String texto, List<Linea> lineas) {
    public PaginaPdf {
        lineas = List.copyOf(lineas);
    }

    public record Linea(String texto, float x, float y, float finX, float tamanoFuente,
                       String textoReconstruido) {
        public Linea(String texto, float x, float y, float finX, float tamanoFuente) {
            this(texto, x, y, finX, tamanoFuente, texto);
        }
    }
}
