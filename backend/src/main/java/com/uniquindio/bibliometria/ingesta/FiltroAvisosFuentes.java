package com.uniquindio.bibliometria.ingesta;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/** Oculta solo los avisos conocidos de fuentes; conserva los demas errores y advertencias. */
public final class FiltroAvisosFuentes implements Filter {
    @Override
    public boolean isLoggable(LogRecord record) {
        String nombreRegistro = record.getLoggerName();
        String mensaje = record.getMessage();
        return !(Level.WARNING.equals(record.getLevel())
                && nombreRegistro != null && nombreRegistro.startsWith("org.apache.pdfbox.pdmodel.font.")
                && mensaje != null && mensaje.startsWith("No Unicode mapping for "));
    }
}
