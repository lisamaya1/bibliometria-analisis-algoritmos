package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.util.JsonUtil;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ComparacionClasicaServiceTest {
    private final List<ArticuloDocumental> corpus = List.of(articulo(3, "a"), articulo(15, "b"),
            articulo(19, "c"), articulo(20, "d"));
    private final ComparacionClasicaService servicio = new ComparacionClasicaService(lector(corpus));

    @Test void dosArticulosProducenUnaPareja() throws Exception {
        var resultado = servicio.comparar(List.of(15, 19));
        assertEquals(1, resultado.totalParejas());
        assertEquals(List.of("15-19"), parejas(resultado));
        assertEquals(4, resultado.totalDocumentosIdf());
    }

    @Test void tresArticulosProducenTresParejasSinIdentidadNiInversas() throws Exception {
        assertEquals(List.of("3-15", "3-19", "15-19"), parejas(servicio.comparar(List.of(3, 15, 19))));
    }

    @Test void cuatroArticulosProducenSeisParejas() throws Exception {
        var resultado = servicio.comparar(List.of(3, 15, 19, 20));
        assertEquals(6, resultado.totalParejas());
        assertEquals(List.of("3-15", "3-19", "3-20", "15-19", "15-20", "19-20"), parejas(resultado));
    }

    @Test void rechazaMenosDeDosDistintos() {
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(List.of()));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(List.of(15)));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(List.of(15, 15)));
    }

    @Test void idsRepetidosSeCuentanUnaSolaVezSinModificarEntrada() throws Exception {
        var seleccion = List.of(15, 19, 15, 19);
        assertEquals(servicio.comparar(List.of(15, 19)), servicio.comparar(seleccion));
        assertEquals(List.of(15, 19, 15, 19), seleccion);
    }

    @Test void rechazaIdsInvalidosOInexistentes() {
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(List.of(0, 15)));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(List.of(-1, 15)));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(Arrays.asList(15, null)));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(List.of(15, 99)));
    }

    @Test void ordenDeSeleccionNoCambiaParejasNiResultados() throws Exception {
        assertEquals(servicio.comparar(List.of(3, 15, 19)), servicio.comparar(List.of(19, 3, 15)));
    }

    @Test void cargaElCorpusUnaSolaVezParaTodasLasParejas() throws Exception {
        int[] lecturas = {0};
        var lector = new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path pdfs, Path json) {
                lecturas[0]++;
                return corpus;
            }
        };
        var resultado = new ComparacionClasicaService(lector).comparar(List.of(3, 15, 19, 20));
        assertEquals(6, resultado.totalParejas());
        assertEquals(1, lecturas[0]);
    }

    @Test void casoAcademicoRealSinCambiarJson() throws Exception {
        Path ruta = PreprocesamientoService.RUTA_JSON_DEFECTO;
        byte[] antes = Files.readAllBytes(ruta);
        var real = new ComparacionClasicaService(lector(JsonUtil.cargarArticulosDesdeJson(ruta)));
        var resultado = real.comparar(List.of(15, 19));
        var pareja = resultado.parejas().get(0);
        assertEquals(20, resultado.totalDocumentosIdf());
        assertEquals(33.0 / 241, pareja.jaccard().similitud(), 1e-12);
        assertEquals(66.0 / 274, pareja.dice().similitud(), 1e-12);
        assertEquals(1123, pareja.levenshtein().distancia());
        assertEquals(1.0 - 1123.0 / 1476, pareja.levenshtein().similitud(), 1e-12);
        assertEquals(0.03858898108939938, pareja.cosenoTfidf().similitud(), 1e-12);
        assertArrayEquals(antes, Files.readAllBytes(ruta));
    }

    @Test void informaLaParejaCuandoUnAlgoritmoNoEstaDefinido() {
        var iguales = new ComparacionClasicaService(lector(List.of(articulo(1, "comun"), articulo(2, "comun"))));
        var error = assertThrows(IllegalArgumentException.class, () -> iguales.comparar(List.of(1, 2)));
        assertTrue(error.getMessage().contains("1 vs 2"));
        assertTrue(error.getMessage().contains("norma TF-IDF cero"));
    }

    private static List<String> parejas(ComparacionClasicaService.Comparacion resultado) {
        return resultado.parejas().stream().map(p -> p.articuloA().id() + "-" + p.articuloB().id()).toList();
    }

    private static PreprocesamientoService lector(List<ArticuloDocumental> corpus) {
        return new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path pdfs, Path json) { return corpus; }
        };
    }

    private static ArticuloDocumental articulo(int id, String palabra) {
        return new ArticuloDocumental(id, "ejemplo.pdf", "Artículo " + id, List.of("Autor"),
                "Original conservado", palabra, List.of(palabra), 1);
    }
}
