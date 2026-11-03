package com.uniquindio.bibliometria.service;

import com.uniquindio.bibliometria.algoritmos.similitud.ia.ModeloMiniLm;
import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class MiniLmServiceTest {
    @Test void usaAbstractProcesadoYCargaCorpusUnaVezSinReutilizarTokensClasicos() throws Exception {
        int[] lecturas = {0};
        var textos = new ArrayList<String>();
        var modelo = modelo(textos);
        var lector = new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path a, Path b) { lecturas[0]++; return corpus(); }
        };
        var resultado = new MiniLmService(lector, modelo).comparar(15, 19);
        assertEquals(1, lecturas[0]);
        assertEquals(List.of("procesado A", "procesado B"), textos);
        assertEquals(2, resultado.dimension());
        assertEquals(0, resultado.resultado().similitud());
        assertEquals(1, resultado.articuloA().tokensContenido());
        assertEquals(1, resultado.articuloB().fragmentos());
    }
    @Test void simetriaEIdentidadConModeloControlado() throws Exception {
        var servicio = servicio(modelo(new ArrayList<>()));
        assertEquals(servicio.comparar(15, 19).resultado().similitud(), servicio.comparar(19, 15).resultado().similitud(), 1e-12);
        assertEquals(1, servicio.comparar(15, 15).resultado().similitud(), 1e-12);
    }
    @Test void idsInvalidosOInexistentesSeRechazanAntesDeInferencia() {
        var servicio = servicio(modelo(new ArrayList<>()));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(0, 19));
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(15, 99));
    }
    @Test void modeloNoDisponibleSeInformaSinInventarResultados() {
        var modelo = new ModeloMiniLm() {
            public long[] tokenizar(String texto) { throw new IllegalStateException("Configura MINILM_MODEL_DIR."); }
            public Salida ejecutar(long[] ids) { throw new AssertionError(); }
            public int dimension() { return 384; }
            public int limiteEntrada() { return 256; }
        };
        assertThrows(IllegalStateException.class, () -> servicio(modelo).comparar(15, 19));
    }
    private static MiniLmService servicio(ModeloMiniLm modelo) {
        return new MiniLmService(new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path a, Path b) { return corpus(); }
        }, modelo);
    }
    public static ModeloMiniLm modelo(List<String> textos) {
        return new ModeloMiniLm() {
            public long[] tokenizar(String texto) { textos.add(texto); return new long[]{texto.equals("procesado A") ? 1 : 2}; }
            public Salida ejecutar(long[] ids) { return new Salida(new float[][]{ids[0] == 1 ? new float[]{1, 0} : new float[]{0, 1}}, new long[]{1}); }
            public int dimension() { return 2; }
            public int limiteEntrada() { return 256; }
        };
    }
    public static List<ArticuloDocumental> corpus() {
        return List.of(new ArticuloDocumental(15, "a.pdf", "Artículo A", List.of(), "Original A", "procesado A", List.of("tokens", "distintos"), 2),
                new ArticuloDocumental(19, "b.pdf", "Artículo B", List.of(), "Original B", "procesado B", List.of("tokens", "distintos"), 2));
    }
}
