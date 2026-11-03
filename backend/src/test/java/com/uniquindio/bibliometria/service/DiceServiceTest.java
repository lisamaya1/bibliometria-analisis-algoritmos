package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DiceServiceTest {
    @Test void casoRealQuinceContraDiecinueveUsaElJsonVigenteSinModificarlo() throws Exception {
        Path ruta = PreprocesamientoService.RUTA_JSON_DEFECTO;
        byte[] original = Files.readAllBytes(ruta);
        var corpus = JsonUtil.cargarArticulosDesdeJson(ruta);
        assertEquals(20, corpus.size());
        // Lee el JSON existente y evita que una ausencia del archivo genere nuevos datos durante la prueba.
        var servicio = new DiceService(lector(corpus));
        var comparacion = servicio.comparar(15, 19);
        var resultado = comparacion.resultado();
        assertEquals(15, comparacion.articuloA().id());
        assertEquals(19, comparacion.articuloB().id());
        assertEquals(133, resultado.conjuntoA().size());
        assertEquals(141, resultado.conjuntoB().size());
        assertEquals(33, resultado.interseccion().size());
        assertEquals(66.0 / 274, resultado.similitud(), 1e-12);
        assertEquals(100 * 66.0 / 274, comparacion.porcentaje(), 1e-10);
        assertTrue(resultado.interseccion().containsAll(List.of("artificial", "findings")));

        // Referencia independiente por listas, sin estructuras hash ni el algoritmo probado.
        var a = distintos(corpus.get(14).tokens());
        var b = distintos(corpus.get(18).tokens());
        var interseccion = new ArrayList<String>();
        for (String palabra : a) if (b.contains(palabra)) interseccion.add(palabra);
        assertEquals(a, resultado.conjuntoA());
        assertEquals(b, resultado.conjuntoB());
        assertEquals(interseccion, resultado.interseccion());
        assertEquals(a.size(), resultado.cardinalidadA());
        assertEquals(b.size(), resultado.cardinalidadB());
        assertEquals(interseccion.size(), resultado.cardinalidadInterseccion());
        assertEquals(resultado.similitud(), servicio.comparar(19, 15).resultado().similitud());
        assertEquals(1, servicio.comparar(15, 15).resultado().similitud());
        assertArrayEquals(original, Files.readAllBytes(ruta));
    }

    @Test void usaSoloTokensSinRetokenizarNiFiltrarYCargaUnaVez() throws Exception {
        int[] lecturas = {0};
        var corpus = List.of(articulo(7, List.of("the", "902", "ai", "ai")),
                articulo(9, List.of("the", "902", "education")));
        var preprocesamiento = new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path pdfs, Path json) {
                lecturas[0]++;
                return corpus;
            }
        };
        var resultado = new DiceService(preprocesamiento).comparar(7, 9).resultado();
        assertEquals(1, lecturas[0]);
        assertEquals(List.of("the", "902"), resultado.interseccion());
        assertEquals(2.0 / 3, resultado.similitud(), 1e-12);
        assertEquals(1, new DiceService(lector(corpus)).comparar(7, 7).resultado().similitud());
    }

    @Test void rechazaIdentificadoresInvalidosOInexistentes() {
        var servicio = new DiceService(lector(List.of(articulo(1, List.of("ai")))));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(0, 1));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(1, -1));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(1, 21));
    }

    @Test void rechazaDosArticulosSinTokens() {
        var servicio = new DiceService(lector(List.of(articulo(1, List.of()), articulo(2, List.of()))));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(1, 2));
    }

    private static PreprocesamientoService lector(List<ArticuloDocumental> corpus) {
        return new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path pdfs, Path json) { return corpus; }
        };
    }

    private static ArticuloDocumental articulo(int id, List<String> tokens) {
        return new ArticuloDocumental(id, "ejemplo.pdf", "Artículo " + id, List.of("Autor"),
                "Original que no debe modificarse", "Texto diferente de los tokens", tokens, tokens.size());
    }

    private static List<String> distintos(List<String> palabras) {
        var resultado = new ArrayList<String>();
        for (String palabra : palabras) if (!resultado.contains(palabra)) resultado.add(palabra);
        return resultado;
    }
}
