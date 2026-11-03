package com.uniquindio.bibliometria.algoritmos.ordenamiento;

import java.util.ArrayList;
import java.util.List;

import com.uniquindio.bibliometria.modelo.TerminoFrecuencia;

public final class CountingSort {

    private CountingSort() {
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
        if (lista == null || lista.isEmpty()) {
            return new ResultadoOrdenamiento("Counting Sort", List.of(), 0.0, 0, 0, 0, 0);
        }

        int n = lista.size();

        // Inicio de la medición empírica de tiempo real en nanosegundos
        long tiempoInicioNano = System.nanoTime();

        // Caso base: si la lista contiene un solo elemento, ya está ordenada
        if (n == 1) {
            long tiempoFinNano = System.nanoTime();
            double duracionMs = (tiempoFinNano - tiempoInicioNano) / 1_000_000.0;
            int f = lista.get(0).getFrecuencia();
            return new ResultadoOrdenamiento("Counting Sort", new ArrayList<>(lista), duracionMs, 1, 1, f, f);
        }

        // PASO 1: Encontrar los valores mínimo y máximo de frecuencia en O(n)
        int minFrecuencia = lista.get(0).getFrecuencia();
        int maxFrecuencia = lista.get(0).getFrecuencia();

        for (int i = 1; i < n; i++) {
            int f = lista.get(i).getFrecuencia();
            if (f < minFrecuencia) {
                minFrecuencia = f;
            }
            if (f > maxFrecuencia) {
                maxFrecuencia = f;
            }
        }

        // Si todas las frecuencias son exactamente iguales, la lista ya se encuentra ordenada
        if (minFrecuencia == maxFrecuencia) {
            long tiempoFinNano = System.nanoTime();
            double duracionMs = (tiempoFinNano - tiempoInicioNano) / 1_000_000.0;
            return new ResultadoOrdenamiento("Counting Sort", new ArrayList<>(lista), duracionMs, n, 1, minFrecuencia, maxFrecuencia);
        }

        // Rango de frecuencias k: define el tamaño del arreglo de conteo
        int k = maxFrecuencia - minFrecuencia + 1;

        // PASO 2: Construir el arreglo de conteo (Histograma de frecuencias) en O(n)
        // Cada índice (frecuencia - minFrecuencia) almacena cuántas veces aparece dicha frecuencia
        int[] conteo = new int[k];
        for (int i = 0; i < n; i++) {
            int frecuencia = lista.get(i).getFrecuencia();
            conteo[frecuencia - minFrecuencia]++;
        }

        // PASO 3: Calcular las sumas acumuladas (Prefix Sums) en O(k)
        // Transforma el arreglo de conteo para que conteo[i] indique la última posición
        // que ocupará un elemento con dicha clave en el arreglo de salida.
        if (descendente) {
            // Para orden descendente (mayor a menor frecuencia), acumulamos desde
            // la frecuencia más alta (k - 1) hacia la más baja (0)
            for (int i = k - 2; i >= 0; i--) {
                conteo[i] += conteo[i + 1];
            }
        } else {
            // Para orden ascendente (menor a mayor), acumulamos de izquierda a derecha
            for (int i = 1; i < k; i++) {
                conteo[i] += conteo[i - 1];
            }
        }

        // PASO 4: Construir el arreglo de salida en O(n) garantizando ESTABILIDAD
        // Recorremos la lista original de derecha a izquierda (desde n - 1 hasta 0).
        TerminoFrecuencia[] salida = new TerminoFrecuencia[n];

        for (int i = n - 1; i >= 0; i--) {
            TerminoFrecuencia actual = lista.get(i);
            int claveIndice = actual.getFrecuencia() - minFrecuencia;

            // La posición en el arreglo ordenado (base 0) es conteo[claveIndice] - 1
            int posicionSalida = conteo[claveIndice] - 1;
            salida[posicionSalida] = actual;

            // Se decrementa el contador para que el siguiente elemento con igual clave
            // ocupe la posición inmediatamente anterior (preserva estabilidad)
            conteo[claveIndice]--;
        }

        // Convertir el arreglo elemental a la lista de resultado
        List<TerminoFrecuencia> resultado = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            resultado.add(salida[i]);
        }

        // Fin de la medición empírica de tiempo real
        long tiempoFinNano = System.nanoTime();
        double duracionMs = (tiempoFinNano - tiempoInicioNano) / 1_000_000.0;

        return new ResultadoOrdenamiento("Counting Sort", resultado, duracionMs, n, k, minFrecuencia, maxFrecuencia);
    }
}
