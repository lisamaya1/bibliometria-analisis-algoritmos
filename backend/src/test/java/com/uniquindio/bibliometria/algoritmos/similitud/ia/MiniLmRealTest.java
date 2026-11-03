package com.uniquindio.bibliometria.algoritmos.similitud.ia;

import com.uniquindio.bibliometria.modelo.ArticuloDocumental;
import com.uniquindio.bibliometria.service.MiniLmService;
import com.uniquindio.bibliometria.service.PreprocesamientoService;
import com.uniquindio.bibliometria.util.JsonUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "MINILM_MODEL_DIR", matches = ".+")
class MiniLmRealTest {
    private static ModeloMiniLmOnnx modelo;
    private static MiniLmService servicio;
    @BeforeAll static void cargarUnaVez() throws Exception {
        modelo = new ModeloMiniLmOnnx(System.getenv("MINILM_MODEL_DIR"));
        var corpus = JsonUtil.cargarArticulosDesdeJson(PreprocesamientoService.RUTA_JSON_DEFECTO);
        servicio = new MiniLmService(new PreprocesamientoService() {
            @Override public List<ArticuloDocumental> obtenerCorpus(Path a, Path b) { return corpus; }
        }, modelo);
    }
    @AfterAll static void cerrar() { if (modelo != null) modelo.close(); }

    @Test void tokenizadorOficialSinEspecialesNiTruncamiento() {
        assertArrayEquals(new long[]{7592, 2088}, modelo.tokenizar("hello world"));
        long[] ids = modelo.tokenizar("hello ".repeat(600));
        assertEquals(600, ids.length);
        var resultado = new EmbeddingMiniLm(modelo).generar("hello ".repeat(600));
        assertEquals(3, resultado.fragmentos());
        assertEquals(List.of(254, 254, 92), resultado.tokensPorFragmento());
        assertEquals(384, resultado.vector().length);
        assertEquals(1, new MatematicaEmbeddings().norma(resultado.vector()), 1e-10);
    }

    @Test void casoRealSimetriaIdentidadYConservacionDelCorpus() throws Exception {
        byte[] original = Files.readAllBytes(PreprocesamientoService.RUTA_JSON_DEFECTO);
        long inicio = System.nanoTime();
        var resultado = servicio.comparar(15, 19);
        long duracion = (System.nanoTime() - inicio) / 1_000_000;
        assertEquals(384, resultado.dimension());
        assertTrue(resultado.resultado().similitud() >= -1 && resultado.resultado().similitud() <= 1);
        assertEquals(resultado.resultado().similitud(), servicio.comparar(19, 15).resultado().similitud(), 1e-6);
        assertEquals(1, servicio.comparar(15, 15).resultado().similitud(), 1e-10);
        assertEquals(resultado.articuloA().tokensContenido(), resultado.articuloA().tokensPorFragmento().stream().mapToInt(Integer::intValue).sum());
        assertEquals(resultado.articuloB().tokensContenido(), resultado.articuloB().tokensPorFragmento().stream().mapToInt(Integer::intValue).sum());
        assertThrows(IllegalArgumentException.class, () -> servicio.comparar(15, 99));
        assertArrayEquals(original, Files.readAllBytes(PreprocesamientoService.RUTA_JSON_DEFECTO));
        System.out.println("MINILM_REAL " + new JsonMapper().writeValueAsString(resultado));
        System.out.println("MINILM_COMPARACION_MS " + duracion);
    }
}
