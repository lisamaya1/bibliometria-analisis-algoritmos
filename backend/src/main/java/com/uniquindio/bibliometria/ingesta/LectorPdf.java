package com.uniquindio.bibliometria.ingesta;

import java.io.IOException;

/** Permite cambiar el lector sin modificar las reglas de extraccion. */
public interface LectorPdf {
    DocumentoPdf leer(String archivo, byte[] pdf) throws IOException;
}
