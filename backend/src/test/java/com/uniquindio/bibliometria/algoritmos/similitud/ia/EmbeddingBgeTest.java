package com.uniquindio.bibliometria.algoritmos.similitud.ia;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EmbeddingBgeTest {
    private ModeloBge modelo(List<Integer> recibidos) {
        return new ModeloBge() {
            public long[] tokenizar(String texto) {
                int cantidad = texto.equals("corto") ? 1 : 1021;
                long[] ids = new long[cantidad];
                for (int i = 0; i < cantidad; i++) ids[i] = i + 1;
                return ids;
            }
            public Salida ejecutar(long[] ids) {
                recibidos.add(ids.length);
                // Solo CLS debe influir, el otro estado tiene valores diferentes.
                float[] cls = ids[0] == 1 ? new float[]{3, 0} : new float[]{0, 4};
                return new Salida(new float[][]{cls, {99, 99}}, new long[]{1, 1});
            }
            public int dimension() { return 2; }
            public int limiteEntrada() { return 512; }
        };
    }
    @Test void textoCortoUsaClsYNormaliza() {
        var recibidos = new ArrayList<Integer>();
        var resultado = new EmbeddingBge(modelo(recibidos)).generar("corto");
        assertArrayEquals(new double[]{1, 0}, resultado.vector(), 1e-12);
        assertEquals(1, resultado.tokensContenido());
        assertEquals(List.of(1), recibidos);
    }
    @Test void textoLargoConservaTodosLosTokensYPonderaNormalizados() {
        var recibidos = new ArrayList<Integer>();
        var resultado = new EmbeddingBge(modelo(recibidos)).generar("largo");
        assertEquals(1021, resultado.tokensContenido());
        assertEquals(List.of(510, 510, 1), recibidos);
        assertEquals(recibidos, resultado.tokensPorFragmento());
        var esperado = new MatematicaEmbeddings().normalizar(new double[]{510, 511});
        assertArrayEquals(esperado, resultado.vector(), 1e-12);
    }
    @Test void fragmentacionRespetaLimitesYOrdenSinSolapar() {
        long[] tokens = new long[1021];
        for (int i = 0; i < tokens.length; i++) tokens[i] = i;
        var partes = FragmentadorTokens.fragmentar(tokens, 512);
        assertEquals(3, partes.size());
        assertArrayEquals(tokens, partes.stream().flatMapToLong(Arrays::stream).toArray());
        assertEquals(1, FragmentadorTokens.fragmentar(new long[510], 512).size());
        assertEquals(2, FragmentadorTokens.fragmentar(new long[511], 512).size());
    }
    @Test void textoVacioYLimiteInvalidoSeRechazan() {
        var embedding = new EmbeddingBge(modelo(new ArrayList<>()));
        assertThrows(IllegalArgumentException.class, () -> embedding.generar(" "));
        assertThrows(IllegalArgumentException.class, () -> embedding.generar(null));
        assertThrows(IllegalArgumentException.class, () -> FragmentadorTokens.fragmentar(new long[]{1}, 2));
    }
}
