package com.uniquindio.bibliometria.ingesta;

import com.uniquindio.bibliometria.preprocesamiento.OpcionesPreprocesamiento;
import com.uniquindio.bibliometria.preprocesamiento.ProcesadorResumen;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Guarda la extraccion una vez y permite consultarla sin volver a leer los PDF. */
public final class ArchivoArticulosJson {
    private final JsonMapper convertidor = new JsonMapper();

    public void guardar(List<ArticuloExtraido> articulos, Path destino,
                        OpcionesPreprocesamiento opciones) throws IOException {
        if (!destino.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".json")) {
            throw new IllegalArgumentException("El archivo de salida debe terminar en .json");
        }
        if (articulos.isEmpty()) {
            throw new IllegalArgumentException("No hay articulos para guardar");
        }
        var datos = new ArrayList<ArticuloJson>();
        var procesador = new ProcesadorResumen();
        // Revisa todos los articulos antes de escribir para no guardar una extraccion incompleta.
        for (ArticuloExtraido articulo : articulos) {
            if (!articulo.tieneCamposCompletos()) {
                throw new IllegalArgumentException("Faltan campos en: " + articulo.archivo()
                        + ". Revise el PDF antes de exportar.");
            }
            var procesado = procesador.procesar(articulo, opciones);
            datos.add(new ArticuloJson(articulo.titulo(), articulo.autores(),
                    articulo.resumenOriginal(), procesado.resumenProcesado(),
                    procesado.palabras()));
        }
        String contenido = convertidor.writerWithDefaultPrettyPrinter().writeValueAsString(datos);
        Path ruta = destino.toAbsolutePath().normalize();
        Files.createDirectories(ruta.getParent());
        Files.writeString(ruta, contenido + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    public List<ArticuloJson> leer(Path archivo) throws IOException {
        String contenido = Files.readString(archivo, StandardCharsets.UTF_8);
        ArticuloJson[] articulos = convertidor.readValue(contenido, ArticuloJson[].class);
        if (articulos == null) {
            throw new IllegalArgumentException("El JSON debe contener una lista de articulos");
        }
        return List.of(articulos);
    }
}
