package com.uniquindio.bibliometria.algoritmos.ordenamiento;

import java.util.List;
import java.util.Objects;

import com.uniquindio.bibliometria.modelo.TerminoFrecuencia;

/**
 * Encapsula el resultado de la ejecución de un algoritmo de ordenamiento,
 * incluyendo la lista ordenada, métricas de desempeño empírico (tiempo real en milisegundos)
 * y utilidades para obtener rankings (ej. Top 15).
 */
public record ResultadoOrdenamiento(
        String algoritmo,
        List<TerminoFrecuencia> terminosOrdenados,
        double tiempoMilisegundos,
        int totalElementos,
        int numeroCubetas,
        int frecuenciaMinima,
        int frecuenciaMaxima
) {
    public ResultadoOrdenamiento {
        Objects.requireNonNull(algoritmo, "algoritmo no puede ser nulo");
        terminosOrdenados = terminosOrdenados == null ? List.of() : List.copyOf(terminosOrdenados);
    }

    /** Constructor de compatibilidad sin frecuencia mínima ni máxima explícitas. */
    public ResultadoOrdenamiento(
            String algoritmo,
            List<TerminoFrecuencia> terminosOrdenados,
            double tiempoMilisegundos,
            int totalElementos,
            int numeroCubetas
    ) {
        this(algoritmo, terminosOrdenados, tiempoMilisegundos, totalElementos, numeroCubetas, 0, 0);
    }

    /**
     * Retorna los primeros K términos más frecuentes (Top K).
     */
    public List<TerminoFrecuencia> getTop(int k) {
        int limite = Math.min(k, terminosOrdenados.size());
        return terminosOrdenados.subList(0, limite);
    }

    /**
     * Imprime una tabla formateada en consola con el Top K de términos más frecuentes
     * y los parámetros requeridos del algoritmo.
     */
    public void imprimirTop(int k) {
        List<TerminoFrecuencia> top = getTop(k);
        System.out.println("=========================================================================");
        System.out.printf("  RANKING TOP %d - ALGORITMO: %s%n", top.size(), algoritmo.toUpperCase());
        System.out.println("=========================================================================");
        System.out.printf("  Número de términos distintos (n): %d%n", totalElementos);
        System.out.printf("  Frecuencia mínima / máxima:       %d / %d%n", frecuenciaMinima, frecuenciaMaxima);
        if ("Counting Sort".equalsIgnoreCase(algoritmo)) {
            System.out.printf("  Rango de claves k (max-min+1):    %d%n", numeroCubetas);
        } else if ("Radix Sort".equalsIgnoreCase(algoritmo)) {
            System.out.printf("  Número de dígitos d:              %d%n", numeroCubetas);
        } else {
            System.out.printf("  Número de cubetas utilizado (B):  %d%n", numeroCubetas);
        }
        System.out.printf("  Tiempo real de ejecución:         %.4f ms%n", tiempoMilisegundos);
        System.out.println("-------------------------------------------------------------------------");
        System.out.printf("  %-6s | %-30s | %-12s%n", "PUESTO", "TÉRMINO", "FRECUENCIA");
        System.out.println("-------------------------------------------------------------------------");
        for (int i = 0; i < top.size(); i++) {
            TerminoFrecuencia tf = top.get(i);
            System.out.printf("  #%-5d | %-30s | %-12d%n", i + 1, tf.getTermino(), tf.getFrecuencia());
        }
        System.out.println("=========================================================================");
    }
}
