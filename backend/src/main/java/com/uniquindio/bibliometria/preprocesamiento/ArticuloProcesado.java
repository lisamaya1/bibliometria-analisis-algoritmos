package com.uniquindio.bibliometria.preprocesamiento;

import com.uniquindio.bibliometria.ingesta.ArticuloExtraido;
import java.util.List;
import java.util.Objects;

public record ArticuloProcesado(ArticuloExtraido articulo, String resumenProcesado,
                               List<String> palabras, OpcionesPreprocesamiento opciones) {
    public ArticuloProcesado {
        Objects.requireNonNull(articulo, "articulo");
        Objects.requireNonNull(resumenProcesado, "resumenProcesado");
        Objects.requireNonNull(opciones, "opciones");
        palabras = List.copyOf(palabras);
    }
}
