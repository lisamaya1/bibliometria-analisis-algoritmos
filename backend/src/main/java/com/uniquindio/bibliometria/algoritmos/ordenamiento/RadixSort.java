package com.uniquindio.bibliometria.algoritmos.ordenamiento;

import java.util.ArrayList;
import java.util.List;

import com.uniquindio.bibliometria.modelo.TerminoFrecuencia;

public final class RadixSort {

    // Base numérica decimal: los dígitos van de 0 a 9
    private static final int BASE = 10;

    private RadixSort() {
        // Constructor privado para clase utilitaria estática
    }

    /**
     * Ordena una lista de términos por su frecuencia en orden descendente (mayor a menor),
     * correspondiente al ranking bibliométrico solicitado.
     *
     * @param lista lista de pares (término, frecuencia) a ordenar.
     * @return {@link ResultadoOrdenamiento} con la lista ordenada y las métricas de ejecución.
     */
    public static ResultadoOrdenamiento ordenarDescendente(List<TerminoFrecuencia> lista) {
        return ordenar(lista, true);
    }

    /**
     * Ordena una lista de términos por su frecuencia especificando la dirección del ordenamiento.
     *
     * @param lista lista de pares (término, frecuencia) a ordenar.
     * @param descendente true para mayor a menor (ranking); false para menor a mayor.
     * @return {@link ResultadoOrdenamiento} con la lista ordenada y las métricas de ejecución.
     */
    public static ResultadoOrdenamiento ordenar(List<TerminoFrecuencia> lista, boolean descendente) {
        // Caso especial: lista vacía o nula
        if (lista == null || lista.isEmpty()) {
            return new ResultadoOrdenamiento("Radix Sort", List.of(), 0.0, 0, 0, 0, 0);
        }

        int n = lista.size();

        // Inicio de la medición empírica de tiempo real en nanosegundos
        long tiempoInicioNano = System.nanoTime();

        // Caso base: si la lista contiene un solo elemento, ya está ordenada
        if (n == 1) {
            long tiempoFinNano = System.nanoTime();
            double duracionMs = (tiempoFinNano - tiempoInicioNano) / 1_000_000.0;
            int f = lista.get(0).getFrecuencia();
            return new ResultadoOrdenamiento("Radix Sort", new ArrayList<>(lista), duracionMs, 1, 1, f, f);
        }

        // PASO 1: Encontrar la frecuencia máxima para saber cuántos dígitos tiene [O(n)]
        int maxFrecuencia = lista.get(0).getFrecuencia();
        int minFrecuencia = lista.get(0).getFrecuencia();

        for (int i = 1; i < n; i++) {
            int f = lista.get(i).getFrecuencia();
            if (f > maxFrecuencia) {
                maxFrecuencia = f;
            }
            if (f < minFrecuencia) {
                minFrecuencia = f;
            }
        }

        // Si todas las frecuencias son iguales, ya está ordenada
        if (minFrecuencia == maxFrecuencia) {
            long tiempoFinNano = System.nanoTime();
            double duracionMs = (tiempoFinNano - tiempoInicioNano) / 1_000_000.0;
            return new ResultadoOrdenamiento("Radix Sort", new ArrayList<>(lista), duracionMs, n, 0, minFrecuencia, maxFrecuencia);
        }

        // PASO 2: Copiar la lista a un arreglo de trabajo para manipularlo
        TerminoFrecuencia[] arreglo = new TerminoFrecuencia[n];
        for (int i = 0; i < n; i++) {
            arreglo[i] = lista.get(i);
        }

        // PASO 3: Ejecutar Counting Sort por cada posición de dígito (LSD → MSD)
        // El divisor (1, 10, 100, 1000, ...) indica qué dígito se está procesando
        int totalDigitos = 0; // Contador de pasadas (dígitos procesados)

        // Recorremos cada posición de dígito mientras el divisor sea menor o igual al máximo
        // Ejemplo: si maxFrecuencia = 245, procesamos unidades (div=1), decenas (div=10), centenas (div=100)
        for (int divisor = 1; maxFrecuencia / divisor > 0; divisor *= BASE) {
            arreglo = countingSortPorDigito(arreglo, n, divisor);
            totalDigitos++;
        }

        // PASO 4: Construir la lista resultado
        // Radix Sort ordena de menor a mayor. Si se pide descendente, invertimos.
        List<TerminoFrecuencia> resultado = new ArrayList<>(n);

        if (descendente) {
            // Recorrer de atrás hacia adelante para obtener orden descendente
            for (int i = n - 1; i >= 0; i--) {
                resultado.add(arreglo[i]);
            }
        } else {
            // Orden ascendente: copiar tal cual
            for (int i = 0; i < n; i++) {
                resultado.add(arreglo[i]);
            }
        }

        // Fin de la medición empírica de tiempo real
        long tiempoFinNano = System.nanoTime();
        double duracionMs = (tiempoFinNano - tiempoInicioNano) / 1_000_000.0;

        return new ResultadoOrdenamiento("Radix Sort", resultado, duracionMs, n, totalDigitos, minFrecuencia, maxFrecuencia);
    }

    /**
     * Subrutina de Counting Sort estable que ordena el arreglo según UN SOLO DÍGITO
     * en la posición indicada por el divisor.
     *
     * Ejemplo: si divisor = 1, ordena por las unidades.
     *          si divisor = 10, ordena por las decenas.
     *          si divisor = 100, ordena por las centenas.
     *
     * @param arreglo  arreglo de entrada con los términos a ordenar.
     * @param n        tamaño del arreglo.
     * @param divisor  potencia de 10 que indica la posición del dígito a procesar.
     * @return nuevo arreglo ordenado de forma estable según el dígito en la posición dada.
     */
    private static TerminoFrecuencia[] countingSortPorDigito(TerminoFrecuencia[] arreglo, int n, int divisor) {

        // Arreglo de salida donde se colocarán los elementos ordenados
        TerminoFrecuencia[] salida = new TerminoFrecuencia[n];

        // Arreglo de conteo: un espacio por cada posible dígito (0 a 9)
        int[] conteo = new int[BASE];

        // ---- Fase 1: Contar cuántas veces aparece cada dígito ---- [O(n)]
        for (int i = 0; i < n; i++) {
            int digito = obtenerDigito(arreglo[i].getFrecuencia(), divisor);
            conteo[digito]++;
        }

        // ---- Fase 2: Sumas acumuladas (Prefix Sums) ---- [O(BASE) = O(10) = O(1)]
        // conteo[i] ahora indica la posición final (exclusiva) del último elemento con ese dígito
        for (int i = 1; i < BASE; i++) {
            conteo[i] += conteo[i - 1];
        }

        // ---- Fase 3: Ubicar cada elemento en su posición final ---- [O(n)]
        // Recorremos de DERECHA a IZQUIERDA para mantener la ESTABILIDAD del ordenamiento
        for (int i = n - 1; i >= 0; i--) {
            int digito = obtenerDigito(arreglo[i].getFrecuencia(), divisor);

            // La posición en el arreglo de salida (base 0) es conteo[digito] - 1
            int posicion = conteo[digito] - 1;
            salida[posicion] = arreglo[i];

            // Se decrementa para que el siguiente elemento con el mismo dígito
            // vaya en la posición inmediatamente anterior (preserva estabilidad)
            conteo[digito]--;
        }

        return salida;
    }

    /**
     * Extrae un dígito específico de un número entero positivo.
     *
     * Ejemplo: obtenerDigito(245, 1)   → 5  (unidades)
     *          obtenerDigito(245, 10)  → 4  (decenas)
     *          obtenerDigito(245, 100) → 2  (centenas)
     *
     * @param numero  el número del cual extraer el dígito.
     * @param divisor potencia de 10 que indica la posición del dígito.
     * @return el dígito en la posición indicada (valor entre 0 y 9).
     */
    private static int obtenerDigito(int numero, int divisor) {
        return (numero / divisor) % BASE;
    }
}
