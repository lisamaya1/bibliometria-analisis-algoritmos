package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.algoritmos.busqueda.*;
import com.uniquindio.bibliometria.util.JsonUtil;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
public class ComparacionBusquedaService {
    private final Path rutaCorpus;
    public ComparacionBusquedaService() { this(PreprocesamientoService.RUTA_JSON_DEFECTO); }
    public ComparacionBusquedaService(Path rutaCorpus) { this.rutaCorpus=rutaCorpus; }
    public record Entrada(BusquedaService.Articulo articulo,String texto,String patron) { }
    public record Respuesta(BusquedaService.Articulo articulo,String fuenteTexto,String unidadPosiciones,
                             List<ResultadoBusqueda> resultados,long actualizacionesTablaBadCharacter) {
        public Respuesta { resultados=List.copyOf(resultados); }
    }

    public Entrada cargarEntrada(int id,String patron) throws IOException {
        if (id<1) throw new IllegalArgumentException("El identificador del artículo debe ser positivo.");
        if (patron==null || patron.isEmpty()) throw new IllegalArgumentException("El patrón no puede ser nulo ni vacío.");
        // Una única lectura del JSON. No hay regeneración desde PDF.
        var corpus=JsonUtil.cargarArticulosDesdeJson(rutaCorpus);
        for (var articulo:corpus) if (articulo.id()==id)
            return new Entrada(new BusquedaService.Articulo(id,articulo.titulo()),articulo.abstractProcesado(),patron);
        throw new IllegalArgumentException("No existe el artículo "+id+" en el corpus.");
    }

    public Respuesta comparar(int id,String patron) throws IOException { return comparar(cargarEntrada(id,patron)); }

    public Respuesta comparar(Entrada entrada) {
        var resultados=new ComparadorBusqueda().comparar(entrada.texto(),entrada.patron());
        // Diagnóstico adicional fuera de las mediciones de búsqueda: mismo preparador, sin copiar su lógica.
        long actualizaciones=new BoyerMoore().preparar(entrada.patron()).actualizaciones();
        return new Respuesta(entrada.articulo(),"abstractProcesado","puntos de código Unicode, base 0",resultados,actualizaciones);
    }
}
