package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.util.JsonUtil;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LevenshteinServiceTest {
    @Test void casoRealQuinceDiecinueveEIdentidadSinModificarElJson() throws Exception {
        Path ruta = PreprocesamientoService.RUTA_JSON_DEFECTO;
        byte[] original = Files.readAllBytes(ruta);
        var corpus = JsonUtil.cargarArticulosDesdeJson(ruta);
        var servicio = new LevenshteinService(lector(corpus));
        var comparacion = servicio.comparar(15, 19);
        assertEquals(15, comparacion.articuloA().id());
        assertEquals(19, comparacion.articuloB().id());
        assertEquals(1476, comparacion.resultado().longitudA());
        assertEquals(1425, comparacion.resultado().longitudB());
        assertEquals(1123, comparacion.resultado().distancia());
        assertEquals(1.0 - 1123.0 / 1476, comparacion.resultado().similitud(), 1e-12);
        assertEquals(100 * (1.0 - 1123.0 / 1476), comparacion.porcentaje(), 1e-10);
        assertEquals(1123, servicio.comparar(19, 15).resultado().distancia());
        var identidad = servicio.comparar(15, 15);
        assertEquals(0, identidad.resultado().distancia());
        assertEquals(1, identidad.resultado().similitud());
        assertEquals(100, identidad.porcentaje());
        assertArrayEquals(original, Files.readAllBytes(ruta));
    }

    @Test void usaSoloAbstractProcesadoCompletoYCargaUnaVez() throws Exception {
        int[] lecturas = {0};
        var corpus = List.of(articulo(7, "a b."), articulo(9, "ab."));
        var lector = new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path pdfs, Path json) {
                lecturas[0]++;
                return corpus;
            }
        };
        var resultado = new LevenshteinService(lector).comparar(7, 9).resultado();
        assertEquals(1, lecturas[0]);
        assertEquals(4, resultado.longitudA());
        assertEquals(3, resultado.longitudB());
        assertEquals(1, resultado.distancia());
        assertEquals(0.75, resultado.similitud());
    }

    @Test void rechazaIdentificadoresInvalidosOInexistentes() {
        var servicio = new LevenshteinService(lector(List.of(articulo(1, "ai"))));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(0, 1));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(1, -1));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(1, 99));
    }

    @Test void permiteTextosVaciosConLaPoliticaDefinida() throws Exception {
        var servicio = new LevenshteinService(lector(List.of(articulo(1, ""), articulo(2, "abc"))));
        assertEquals(0, servicio.comparar(1, 2).porcentaje());
        assertEquals(100, servicio.comparar(1, 1).porcentaje());
    }

    private static PreprocesamientoService lector(List<ArticuloDocumental> corpus) {
        return new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path pdfs, Path json) { return corpus; }
        };
    }

    private static ArticuloDocumental articulo(int id, String procesado) {
        return new ArticuloDocumental(id, "ejemplo.pdf", "Artículo " + id, List.of("Autor"),
                "Original distinto que no debe usarse", procesado, List.of("tokens", "iguales"), 2);
    }
}
