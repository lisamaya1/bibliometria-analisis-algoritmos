package com.uniquindio.bibliometria.ingesta;

import com.uniquindio.bibliometria.preprocesamiento.ProcesadorResumen;
import com.uniquindio.bibliometria.preprocesamiento.OpcionesPreprocesamiento;
import com.uniquindio.bibliometria.preprocesamiento.ArticuloProcesado;
import java.io.IOException;
import java.util.Objects;

/** Reune la lectura, extraccion y limpieza de un documento. */
public final class ProcesadorDocumentos {
    private final LectorPdf lector;
    private final ExtractorArticulos extractor;
    private final ProcesadorResumen procesador;

    public ProcesadorDocumentos(LectorPdf lector, ExtractorArticulos extractor,
                            ProcesadorResumen procesador) {
        this.lector = Objects.requireNonNull(lector);
        this.extractor = Objects.requireNonNull(extractor);
        this.procesador = Objects.requireNonNull(procesador);
    }

    public ArticuloExtraido extraer(String archivo, byte[] pdf) throws IOException {
        return extractor.extraer(lector.leer(archivo, pdf));
    }

    /** Tambien permite procesar una extraccion corregida manualmente. */
    public ArticuloProcesado preprocesar(ArticuloExtraido articulo) {
        return procesador.procesar(articulo);
    }

    public ArticuloProcesado preprocesar(ArticuloExtraido articulo, OpcionesPreprocesamiento opciones) {
        return procesador.procesar(articulo, opciones);
    }
}
