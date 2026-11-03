package com.uniquindio.bibliometria.algoritmos.ordenamiento;

import java.util.ArrayList;
import java.util.List;

import com.uniquindio.bibliometria.modelo.TerminoFrecuencia;

public final class BucketSort {

    private BucketSort() {
    }

    /**
     * Ordena una lista de términos por su frecuencia en orden descendente (mayor a menor),
     * calculando automáticamente el número óptimo de cubetas.
     *
     * @param lista lista de pares (término, frecuencia) a ordenar.
     * @return {@link ResultadoOrdenamiento} con la lista ordenada y métricas empíricas.
     */
    public static ResultadoOrdenamiento ordenarDescendente(List<TerminoFrecuencia> lista) {
        if (lista == null || lista.isEmpty()) {
            return new ResultadoOrdenamiento("Bucket Sort", List.of(), 0.0, 0, 0, 0, 0);
        }
        int numCubetas = calcularNumeroCubetasOptimo(lista.size());
        return ordenar(lista, numCubetas, true);
    }

    /**
     * Ordena una lista de términos por su frecuencia especificando el número de cubetas
     * y la dirección del ordenamiento.
     *
     * @param lista lista de pares (término, frecuencia) a ordenar.
     * @param numCubetas número de cubetas a utilizar (B).
     * @param descendente true para mayor a menor (ranking); false para menor a mayor.
     * @return {@link ResultadoOrdenamiento} con la lista ordenada y métricas empíricas.
     */
    @SuppressWarnings("unchecked")
    public static ResultadoOrdenamiento ordenar(List<TerminoFrecuencia> lista, int numCubetas, boolean descendente) {
        if (lista == null || lista.isEmpty()) {
            return new ResultadoOrdenamiento("Bucket Sort", List.of(), 0.0, 0, numCubetas, 0, 0);
        }

        int n = lista.size();
        if (numCubetas <= 0) {
            numCubetas = calcularNumeroCubetasOptimo(n);
        }

        // Inicio de la medición empírica de tiempo de ejecución
        long tiempoInicioNano = System.nanoTime();

        // Si la lista tiene solo un elemento, ya está ordenada
        if (n == 1) {
            long tiempoFinNano = System.nanoTime();
            double duracionMs = (tiempoFinNano - tiempoInicioNano) / 1_000_000.0;
            int f = lista.get(0).getFrecuencia();
            return new ResultadoOrdenamiento("Bucket Sort", new ArrayList<>(lista), duracionMs, n, numCubetas, f, f);
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

        // Si todas las frecuencias son idénticas, no es necesario distribuir
        if (minFrecuencia == maxFrecuencia) {
            long tiempoFinNano = System.nanoTime();
            double duracionMs = (tiempoFinNano - tiempoInicioNano) / 1_000_000.0;
            return new ResultadoOrdenamiento("Bucket Sort", new ArrayList<>(lista), duracionMs, n, numCubetas, minFrecuencia, maxFrecuencia);
        }

        // Ajustar número de cubetas si el rango de valores es menor que B
        int rango = maxFrecuencia - minFrecuencia + 1;
        if (numCubetas > rango) {
            numCubetas = rango;
        }

        // PASO 2: Inicializar el arreglo de cubetas
        List<TerminoFrecuencia>[] cubetas = new List[numCubetas];
        for (int i = 0; i < numCubetas; i++) {
            cubetas[i] = new ArrayList<>();
        }

        // PASO 3: FASE DE DISPERSIÓN (SCATTER) en O(n)
        // Mapeo lineal de cada frecuencia f al índice de cubeta correspondiente
        for (int i = 0; i < n; i++) {
            TerminoFrecuencia elem = lista.get(i);
            int indiceCubeta = calcularIndiceCubeta(elem.getFrecuencia(), minFrecuencia, maxFrecuencia, numCubetas);
            cubetas[indiceCubeta].add(elem);
        }

        // PASO 4: FASE DE ORDENAMIENTO INTERNO POR CUBETA
        // Implementación explícita de Insertion Sort (sin librerías externas)
        for (int i = 0; i < numCubetas; i++) {
            if (cubetas[i].size() > 1) {
                ordenarPorInsercion(cubetas[i], descendente);
            }
        }

        // PASO 5: FASE DE RECOLECCIÓN (GATHER) en O(n + B)
        List<TerminoFrecuencia> resultado = new ArrayList<>(n);

        if (descendente) {
            // Para orden descendente (mayor a menor frecuencia), recolectamos desde
            // la cubeta de mayor índice hacia la de menor índice
            for (int i = numCubetas - 1; i >= 0; i--) {
                resultado.addAll(cubetas[i]);
            }
        } else {
            // Para orden ascendente, recolectamos desde la cubeta 0 hacia numCubetas - 1
            for (int i = 0; i < numCubetas; i++) {
                resultado.addAll(cubetas[i]);
            }
        }

        // Fin de la medición empírica de tiempo
        long tiempoFinNano = System.nanoTime();
        double duracionMs = (tiempoFinNano - tiempoInicioNano) / 1_000_000.0;

        return new ResultadoOrdenamiento("Bucket Sort", resultado, duracionMs, n, numCubetas, minFrecuencia, maxFrecuencia);
    }

    /**
     * Función matemática de dispersión: calcula a qué cubeta pertenece un elemento
     * dada su frecuencia.
     * Mapea el intervalo cerrado [min, max] al rango discreto de índices [0, numCubetas - 1].
     */
    private static int calcularIndiceCubeta(int frecuencia, int min, int max, int numCubetas) {
        long numerador = (long) (frecuencia - min) * (numCubetas - 1);
        long denominador = max - min;
        int indice = (int) (numerador / denominador);

        // Control de límites por seguridad numérica
        if (indice < 0) indice = 0;
        if (indice >= numCubetas) indice = numCubetas - 1;
        return indice;
    }

    /**
     * Algoritmo explícito de Ordenamiento por Inserción (Insertion Sort).
     * Implementado desde cero utilizando ciclos básicos y desplazamientos manuales
     * para garantizar total transparencia algorítmica sin recurrir a funciones mágicas.
     *
     * @param cubeta lista de elementos dentro de la cubeta a ordenar in-situ.
     * @param descendente true para ordenar de mayor a menor; false para menor a mayor.
     */
    private static void ordenarPorInsercion(List<TerminoFrecuencia> cubeta, boolean descendente) {
        int m = cubeta.size();
        for (int i = 1; i < m; i++) {
            TerminoFrecuencia actual = cubeta.get(i);
            int j = i - 1;

            if (descendente) {
                // En orden descendente, desplazamos a la derecha los elementos con menor frecuencia
                while (j >= 0 && debeDesplazarDescendente(cubeta.get(j), actual)) {
                    cubeta.set(j + 1, cubeta.get(j));
                    j--;
                }
            } else {
                // En orden ascendente, desplazamos a la derecha los elementos con mayor frecuencia
                while (j >= 0 && debeDesplazarAscendente(cubeta.get(j), actual)) {
                    cubeta.set(j + 1, cubeta.get(j));
                    j--;
                }
            }
            cubeta.set(j + 1, actual);
        }
    }

    /**
     * Criterio de comparación para orden descendente:
     * El elemento previo debe desplazarse si su frecuencia es estrictamente menor a la actual,
     * o si tienen igual frecuencia pero alfabéticamente va después (para desempate determinista).
     */
    private static boolean debeDesplazarDescendente(TerminoFrecuencia previo, TerminoFrecuencia actual) {
        if (previo.getFrecuencia() < actual.getFrecuencia()) {
            return true;
        }
        if (previo.getFrecuencia() == actual.getFrecuencia()) {
            return previo.getTermino().compareTo(actual.getTermino()) > 0;
        }
        return false;
    }

    /**
     * Criterio de comparación para orden ascendente.
     */
    private static boolean debeDesplazarAscendente(TerminoFrecuencia previo, TerminoFrecuencia actual) {
        if (previo.getFrecuencia() > actual.getFrecuencia()) {
            return true;
        }
        if (previo.getFrecuencia() == actual.getFrecuencia()) {
            return previo.getTermino().compareTo(actual.getTermino()) > 0;
        }
        return false;
    }

    /**
     * Heurística estándar para definir la cantidad de cubetas:
     * Para N elementos, B = max(10, ⌈√N⌉). Esto equilibra el número de cubetas
     * con la cantidad promedio de elementos por cubeta para acercarse al O(n) ideal.
     */
    private static int calcularNumeroCubetasOptimo(int n) {
        if (n <= 10) return Math.max(1, n);
        int cubetas = (int) Math.ceil(Math.sqrt(n));
        return Math.max(10, cubetas);
    }
}
