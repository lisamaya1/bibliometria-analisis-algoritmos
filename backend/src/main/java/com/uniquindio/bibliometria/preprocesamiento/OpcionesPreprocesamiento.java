package com.uniquindio.bibliometria.preprocesamiento;

/** Opciones de limpieza que pueden ajustarse sin cambiar la lectura del PDF. */
public record OpcionesPreprocesamiento(boolean normalizarEspacios, boolean minusculas,
                                   boolean normalizarUnicode, boolean unirGuiones,
                                   boolean reconstruirEspacios) {
    /** Permite elegir cuatro reglas sin activar la reconstruccion de espacios. */
    public OpcionesPreprocesamiento(boolean normalizarEspacios, boolean minusculas,
                                 boolean normalizarUnicode, boolean unirGuiones) {
        this(normalizarEspacios, minusculas, normalizarUnicode, unirGuiones, false);
    }

    public static OpcionesPreprocesamiento limpiezaMinima() {
        return new OpcionesPreprocesamiento(true, true, true, true, true);
    }

    public static OpcionesPreprocesamiento conservarTexto() {
        return new OpcionesPreprocesamiento(false, false, false, false, false);
    }
}
