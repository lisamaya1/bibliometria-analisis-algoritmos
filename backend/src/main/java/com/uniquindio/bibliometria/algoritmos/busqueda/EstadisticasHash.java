package com.uniquindio.bibliometria.algoritmos.busqueda;

/** Conteos de iteraciones de hash, separados de las comparaciones de caracteres. */
public record EstadisticasHash(long base, long modulo, long hashPatron, Long hashVentanaInicial, long potencia,
                               long ventanasProcesadas, long coincidenciasHash, long colisiones,
                               long actualizacionesRolling, long pasosHashPatron, long pasosHashInicial,
                               long pasosPotencia) { }
