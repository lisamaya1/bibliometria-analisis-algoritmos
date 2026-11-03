package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.algoritmos.busqueda.*;
import com.uniquindio.bibliometria.util.JsonUtil;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Path;

@Service
public class BusquedaService {
    private final Path rutaCorpus;
    private final BusquedaExacta kmp = new Kmp();
    private final BusquedaExacta boyerMoore = new BoyerMoore();
    private final BusquedaExacta rabinKarp = new RabinKarp();

    public BusquedaService() { this(PreprocesamientoService.RUTA_JSON_DEFECTO); }
    public BusquedaService(Path rutaCorpus) { this.rutaCorpus = rutaCorpus; }

    public Respuesta buscarKmp(int articuloId, String patron) throws IOException {
        return buscar(articuloId, patron, kmp);
    }

    public Respuesta buscarBoyerMoore(int articuloId, String patron) throws IOException {
        return buscar(articuloId, patron, boyerMoore);
    }

    public Respuesta buscarRabinKarp(int articuloId, String patron) throws IOException {
        return buscar(articuloId, patron, rabinKarp);
    }

    private Respuesta buscar(int articuloId, String patron, BusquedaExacta algoritmo) throws IOException {
        if (articuloId < 1) throw new IllegalArgumentException("El identificador del artículo debe ser positivo.");
        if (patron == null || patron.isEmpty()) throw new IllegalArgumentException("El patrón no puede ser nulo ni vacío.");
        // Solo lectura del JSON: no usa la regeneración desde PDF de obtenerCorpus.
        var corpus = JsonUtil.cargarArticulosDesdeJson(rutaCorpus);
        for (var articulo : corpus) if (articulo.id() == articuloId) {
            var resultado = algoritmo.buscar(articulo.abstractProcesado(), patron);
            return new Respuesta(new Articulo(articulo.id(), articulo.titulo()), "abstractProcesado",
                    "puntos de código Unicode, base 0", resultado);
        }
        throw new IllegalArgumentException("No existe el artículo " + articuloId + " en el corpus.");
    }

    public record Articulo(int id, String titulo) { }
    public record Respuesta(Articulo articulo, String fuenteTexto, String unidadPosiciones, ResultadoBusqueda resultado) { }
}
