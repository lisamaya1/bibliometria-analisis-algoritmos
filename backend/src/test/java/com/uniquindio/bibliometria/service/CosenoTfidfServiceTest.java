package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.util.JsonUtil;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CosenoTfidfServiceTest {
    @Test void casoRealConTodoElCorpusIdentidadYSimetriaSinModificarJson() throws Exception {
        Path ruta = PreprocesamientoService.RUTA_JSON_DEFECTO;
        byte[] original = Files.readAllBytes(ruta);
        var servicio = new CosenoTfidfService(lector(JsonUtil.cargarArticulosDesdeJson(ruta)));
        var comparacion = servicio.comparar(15, 19);
        var resultado = comparacion.resultado();
        assertEquals(15, comparacion.articuloA().id());
        assertEquals(19, comparacion.articuloB().id());
        assertEquals(20, resultado.totalDocumentos());
        assertEquals(1131, resultado.tamanoVocabulario());
        assertEquals(41.640274232546155, resultado.productoPunto(), 1e-10);
        assertEquals(31.83952479896092, resultado.normaA(), 1e-10);
        assertEquals(33.89094605127457, resultado.normaB(), 1e-10);
        assertEquals(0.03858898108939938, resultado.similitud(), 1e-12);
        assertEquals(3.8588981089399375, comparacion.porcentaje(), 1e-10);
        assertEquals(resultado.similitud(), servicio.comparar(19, 15).resultado().similitud(), 1e-12);
        assertEquals(1, servicio.comparar(15, 15).resultado().similitud(), 1e-12);
        assertArrayEquals(original, Files.readAllBytes(ruta));
    }

    @Test void utilizaTokensYTodosLosDocumentosYCargaUnaSolaVez() throws Exception {
        int[] lecturas = {0};
        var corpus = List.of(articulo(7, List.of("the", "the", "902")),
                articulo(9, List.of("ai", "902")), articulo(12, List.of("the", "ai", "902")));
        var lector = new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path pdfs, Path json) {
                lecturas[0]++;
                return corpus;
            }
        };
        var resultado = new CosenoTfidfService(lector).comparar(7, 12).resultado();
        assertEquals(1, lecturas[0]);
        assertEquals(3, resultado.totalDocumentos());
        assertEquals(3, resultado.tamanoVocabulario());
        assertEquals(2 * Math.log(1.5), resultado.normaA(), 1e-12);
        assertEquals(1 / Math.sqrt(2), resultado.similitud(), 1e-12);
    }

    @Test void rechazaIdentificadoresInvalidosOInexistentes() {
        var servicio = new CosenoTfidfService(lector(List.of(articulo(1, List.of("a")))));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(0, 1));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(1, -1));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(1, 99));
    }

    @Test void rechazaDocumentoSeleccionadoVacio() {
        var servicio = new CosenoTfidfService(lector(List.of(articulo(1, List.of()), articulo(2, List.of("a")))));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(1, 2));
    }

    @Test void rechazaVectorDeNormaCeroNoVacio() {
        var servicio = new CosenoTfidfService(lector(List.of(articulo(1, List.of("comun")), articulo(2, List.of("comun")))));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(1, 1));
    }

    private static PreprocesamientoService lector(List<ArticuloDocumental> corpus) {
        return new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path pdfs, Path json) { return corpus; }
        };
    }

    private static ArticuloDocumental articulo(int id, List<String> tokens) {
        return new ArticuloDocumental(id, "ejemplo.pdf", "Artículo " + id, List.of("Autor"),
                "Original diferente", "Texto distinto de los tokens", tokens, tokens.size());
    }
}
