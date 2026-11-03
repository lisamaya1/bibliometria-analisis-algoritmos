package com.uniquindio.bibliometria.algoritmos.similitud.ia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Fragmentos consecutivos sin truncamiento ni solapamiento; reserva CLS y SEP. */
public final class FragmentadorTokens {
    private FragmentadorTokens() { }
    public static List<long[]> fragmentar(long[] tokens, int limiteEntrada) {
        if (tokens == null || tokens.length == 0) throw new IllegalArgumentException("El abstract no contiene tokens utilizables por el modelo.");
        int capacidad = limiteEntrada - 2;
        if (capacidad < 1) throw new IllegalArgumentException("El límite debe permitir contenido y tokens especiales.");
        var fragmentos = new ArrayList<long[]>();
        for (int inicio = 0; inicio < tokens.length; inicio += capacidad) {
            int fin = Math.min(tokens.length, inicio + capacidad);
            fragmentos.add(Arrays.copyOfRange(tokens, inicio, fin));
        }
        return fragmentos;
    }
}
