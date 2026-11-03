# Comparación interna de búsquedas exactas

Registro de la Fase 4 del Requerimiento 3. No sustituye el documento técnico final ni contiene el conteo formal T(n) del profesor.

## Entrada y ejecución común

El servicio lee una sola vez el JSON vigente, selecciona `abstractProcesado` y entrega exactamente el mismo texto y patrón a KMP, Boyer-Moore (solo Bad Character) y Rabin-Karp. Reutiliza sus métodos `buscar`; no reimplementa ni modifica los algoritmos. Comprueba igualdad de posiciones, ocurrencias, indicador de hallazgo y longitudes. Una diferencia se considera un error.

Las posiciones son puntos de código Unicode, base 0. La búsqueda es exacta y distingue mayúsculas: no modifica el patrón ni vuelve a normalizar el abstract. Las coincidencias superpuestas se conservan.

El endpoint agregador es `GET /api/busqueda/comparar?articulo=19&patron=chatgpt`. Se justifica para garantizar una única lectura y entrada compartida, en lugar de tres solicitudes independientes. Los resultados individuales conservan sus contadores y tiempos diagnósticos. La cantidad de actualizaciones Bad Character se consulta mediante el preparador existente en una ejecución adicional fuera de los intervalos medidos; no se suma a los tiempos de búsqueda.

El endpoint devuelve 400 para artículo inexistente, ID inválido o patrón vacío; 500 para problemas de lectura o discrepancias funcionales. No incorpora frontend en esta fase.

## Casos reales

Todos usan el artículo 19, «What Is the Impact of ChatGPT on Education? A Rapid Review of the Literature», con 1425 puntos de código en su abstract procesado.

| Caso | Patrón | Longitud | Ocurrencias | Posiciones de los tres algoritmos |
| --- | --- | ---: | ---: | --- |
| A, varias | `chatgpt` | 7 | 5 | [42, 243, 644, 825, 1386] |
| B, una | `november 2022` | 13 | 1 | [67] |
| C, ausente | `quantum entanglement` | 20 | 0 | [] |

## Metodología temporal

Medición ejecutada en Java 21.0.8, Windows 11, amd64; la JVM informó 8 procesadores disponibles. Es una **comparación empírica local**, no un benchmark científico universal.

Por cada caso se abrió una JVM nueva, sin servidor ni modelos IA. Dentro de cada JVM los tres algoritmos se ejecutaron secuencialmente, alternando el orden por ronda: KMP/BM/RK, BM/RK/KMP y RK/KMP/BM. Cada algoritmo tuvo 2000 ejecuciones de calentamiento y 5000 muestras posteriores. Todas las ejecuciones usaron los mismos objetos de entrada del caso.

Se registraron tres intervalos separados mediante `System.nanoTime()`:

- **Preparación:** intervalo interno que ya proporciona cada algoritmo (LPS, tabla o hashes).
- **Búsqueda:** intervalo interno del recorrido de búsqueda, con sus contadores existentes.
- **Método completo:** cronómetro externo alrededor de `buscar`, incluyendo conversión a puntos de código, preparación, búsqueda y construcción del resultado.

Lectura de JSON, compilación, arranque, HTTP, serialización, cálculo de estadísticas y comprobación de resultados quedan fuera de esos intervalos. Después de cada muestra se verifica su resultado y se consume mediante un acumulador `volatile`, fuera del cronómetro externo.

El método completo incluye preparación y búsqueda: **no deben sumarse sus estadísticas entre sí**. Tampoco se obtiene el método completo sumando mínimos o medianas de otros intervalos. Las mediciones incluyen la instrumentación existente y el coste de los relojes. No se eliminaron valores atípicos. JIT, GC, planificación del sistema y resolución del reloj pueden influir; el calentamiento no garantiza un estado estacionario completo. Un mínimo observado de 0 ns en preparación refleja resolución del reloj, no trabajo gratuito.

Los tiempos diagnósticos de la primera comparación funcional y del endpoint no se usan como muestras estables. Las tablas siguientes proceden únicamente de las 5000 repeticiones posteriores al calentamiento.

Reproducción desde la raíz del proyecto:

```powershell
.\scripts\comparar-busqueda.ps1 -Articulo 19 -Patron 'chatgpt'
.\scripts\comparar-busqueda.ps1 -Articulo 19 -Patron 'november 2022'
.\scripts\comparar-busqueda.ps1 -Articulo 19 -Patron 'quantum entanglement'
```

Los valores predeterminados son `-Calentamiento 2000 -Repeticiones 5000`. El script imprime un informe JSON con resultados funcionales y estadísticas temporales; no escribe datos nuevos en el corpus. Prepara el classpath en una carpeta temporal propia y la elimina al terminar.

## Interpretación de contadores

No existe un contador de «operaciones totales». Las comparaciones de caracteres, escrituras de tabla y operaciones hash no tienen el mismo costo conceptual.

- KMP separa comparaciones de búsqueda y comparaciones durante la construcción LPS.
- Boyer-Moore informa comparaciones de búsqueda y actualizaciones de tabla: una por punto de código del patrón. No se añadieron contadores de desplazamientos que su respuesta normal no exponía.
- Rabin-Karp cuenta comparaciones de caracteres durante la verificación exacta, ventanas, coincidencias de hash, colisiones, actualizaciones rolling y tres tipos de pasos de preparación. Estos últimos permanecen separados.
- `N/A` indica que una métrica no corresponde al algoritmo. Los ceros observados, como colisiones o verificaciones del caso ausente, son valores reales.

## Complejidades preliminares de las variantes existentes

Sean n y m las longitudes en puntos de código, con patrón no vacío y m ≤ n. Son cotas asintóticas bajo el modelo habitual de operaciones sobre palabras de máquina; no fijan costos exactos de bibliotecas ni sustituyen T(n).

| Algoritmo | Preparación | Búsqueda | Alcance y condiciones |
| --- | --- | --- | --- |
| KMP | O(m), construcción LPS | O(n) | Reutiliza prefijos; el método completo es O(n+m). |
| Boyer-Moore Bad Character | O(m) esperado con acceso esperado constante a HashMap | Favorable: O(ceil(n/m)) comparaciones si cada fallo permite desplazar m; peor caso O(nm) | Esta variante no incorpora Good Suffix ni Galil. Los supuestos del mapa no constituyen un costo unitario formal. |
| Rabin-Karp | O(m), hashes iniciales y potencia | O(n+V), donde V es el número de comparaciones exactas de verificación | Cada actualización rolling es O(1). Incluyendo preparación: O(n+m+V). Muchas verificaciones pueden producir O(nm). |

KMP y la heurística Bad Character se contrastaron con las notas de [Cornell, CS312: String search](https://www.cs.cornell.edu/courses/cs312/2002fa/lectures/lec26.htm). La cota favorable de Boyer-Moore se refiere al recorrido, no a toda la implementación Java: convertir el texto completo a puntos de código ya cuesta O(n+m), incluso cuando hay pocos alineamientos. Las condiciones de HashMap y la adaptación a todas las coincidencias se deducen del código del proyecto.

La actualización incremental y la necesidad de verificar hashes coincidentes se contrastaron con [Waterloo, CS240E: String Matching](https://student.cs.uwaterloo.ca/~cs240e/w26/modules/module09.pdf). El caso promedio frecuentemente citado como O(n+m) requiere condiciones favorables sobre hashes y un número limitado de verificaciones. Nuestro código usa base y módulo fijos; no corresponde atribuirle la garantía probabilística de una selección aleatoria del módulo. Además, muchas coincidencias verdaderas superpuestas también pueden ocasionar O(nm), aun sin colisiones.

## Validación y alcance

Se agregaron 12 pruebas en cuatro clases: comparador funcional, medición y estadísticas, servicio y controlador. Cubren igualdad entre algoritmos, los tres casos reales, coincidencias superpuestas, Unicode, entradas inválidas, ausencia de nuevas lecturas tras cargar la entrada, respuestas HTTP y coherencia con los métodos individuales. Las pruebas anteriores se ejecutaron como regresión.

`mvn test`: **251 aprobadas; 0 fallos, 0 errores y 0 omitidas**, con las rutas locales de MiniLM y BGE configuradas para incluir sus pruebas reales. `npm run build`: compilación correcta. No se modificaron los algoritmos, corpus, preprocesamiento ni frontend. No se añadieron dependencias.

En los tres casos medidos KMP presentó el menor promedio de búsqueda. Boyer-Moore comparó menos caracteres que KMP, pero eso no produjo un menor promedio en esta ejecución. Rabin-Karp no verificó caracteres para el patrón ausente, aunque procesó todas sus ventanas y hashes. Estas observaciones se limitan a estos datos, implementación y equipo: no establecen un algoritmo universalmente más rápido.

## Resultados medidos

Todos los tiempos siguientes están expresados en **nanosegundos**. Cada fila temporal resume 5000 muestras. Las tablas son un registro de esta ejecución, no constantes utilizadas por el código.

### Caso A: `chatgpt`

| Metrica | KMP | Boyer-Moore | Rabin-Karp |
| --- | ---: | ---: | ---: |
| Longitud texto | 1425 | 1425 | 1425 |
| Longitud patron | 7 | 7 | 7 |
| Ocurrencias | 5 | 5 | 5 |
| Posiciones | [42, 243, 644, 825, 1386] | [42, 243, 644, 825, 1386] | [42, 243, 644, 825, 1386] |
| Comparaciones caracteres (busqueda) | 1471 | 275 | 35 |
| Comparaciones LPS | 6 | N/A | N/A |
| Actualizaciones Bad Character | N/A | 7 | N/A |
| Ventanas procesadas | N/A | N/A | 1419 |
| Coincidencias hash | N/A | N/A | 5 |
| Colisiones | N/A | N/A | 0 |
| Actualizaciones rolling | N/A | N/A | 1418 |
| Pasos hash patron | N/A | N/A | 7 |
| Pasos hash inicial | N/A | N/A | 7 |
| Pasos potencia | N/A | N/A | 6 |

| Algoritmo | Intervalo (ns) | Minimo | Promedio | Mediana | Maximo |
| --- | --- | ---: | ---: | ---: | ---: |
| kmp | Preparacion | 0 | 80.20 | 100.00 | 5600 |
| kmp | Busqueda | 5200 | 5904.94 | 5500.00 | 79600 |
| kmp | Metodo completo | 6900 | 8577.74 | 7600.00 | 85900 |
| boyer-moore | Preparacion | 400 | 1262.50 | 600.00 | 75800 |
| boyer-moore | Busqueda | 3700 | 12365.12 | 15800.00 | 467100 |
| boyer-moore | Metodo completo | 5600 | 17172.68 | 19200.00 | 2206000 |
| rabin-karp | Preparacion | 100 | 166.98 | 100.00 | 3900 |
| rabin-karp | Busqueda | 21400 | 35001.26 | 22500.00 | 2159100 |
| rabin-karp | Metodo completo | 23000 | 37690.38 | 24600.00 | 2163800 |

### Caso B: `november 2022`

| Metrica | KMP | Boyer-Moore | Rabin-Karp |
| --- | ---: | ---: | ---: |
| Longitud texto | 1425 | 1425 | 1425 |
| Longitud patron | 13 | 13 | 13 |
| Ocurrencias | 1 | 1 | 1 |
| Posiciones | [67] | [67] | [67] |
| Comparaciones caracteres (busqueda) | 1515 | 156 | 13 |
| Comparaciones LPS | 12 | N/A | N/A |
| Actualizaciones Bad Character | N/A | 13 | N/A |
| Ventanas procesadas | N/A | N/A | 1413 |
| Coincidencias hash | N/A | N/A | 1 |
| Colisiones | N/A | N/A | 0 |
| Actualizaciones rolling | N/A | N/A | 1412 |
| Pasos hash patron | N/A | N/A | 13 |
| Pasos hash inicial | N/A | N/A | 13 |
| Pasos potencia | N/A | N/A | 12 |

| Algoritmo | Intervalo (ns) | Minimo | Promedio | Mediana | Maximo |
| --- | --- | ---: | ---: | ---: | ---: |
| kmp | Preparacion | 0 | 75.40 | 100.00 | 14300 |
| kmp | Busqueda | 5100 | 5729.30 | 5500.00 | 104900 |
| kmp | Metodo completo | 6900 | 8391.90 | 7600.00 | 107500 |
| boyer-moore | Preparacion | 600 | 1260.40 | 900.00 | 76000 |
| boyer-moore | Busqueda | 2200 | 9113.32 | 9800.00 | 445700 |
| boyer-moore | Metodo completo | 4300 | 13590.74 | 13600.00 | 456900 |
| rabin-karp | Preparacion | 100 | 222.64 | 200.00 | 14800 |
| rabin-karp | Busqueda | 22300 | 26674.10 | 23500.00 | 2223900 |
| rabin-karp | Metodo completo | 24100 | 29797.08 | 25600.00 | 2410200 |

### Caso C: `quantum entanglement`

| Metrica | KMP | Boyer-Moore | Rabin-Karp |
| --- | ---: | ---: | ---: |
| Longitud texto | 1425 | 1425 | 1425 |
| Longitud patron | 20 | 20 | 20 |
| Ocurrencias | 0 | 0 | 0 |
| Posiciones | [] | [] | [] |
| Comparaciones caracteres (busqueda) | 1426 | 116 | 0 |
| Comparaciones LPS | 19 | N/A | N/A |
| Actualizaciones Bad Character | N/A | 20 | N/A |
| Ventanas procesadas | N/A | N/A | 1406 |
| Coincidencias hash | N/A | N/A | 0 |
| Colisiones | N/A | N/A | 0 |
| Actualizaciones rolling | N/A | N/A | 1405 |
| Pasos hash patron | N/A | N/A | 20 |
| Pasos hash inicial | N/A | N/A | 20 |
| Pasos potencia | N/A | N/A | 19 |

| Algoritmo | Intervalo (ns) | Minimo | Promedio | Mediana | Maximo |
| --- | --- | ---: | ---: | ---: | ---: |
| kmp | Preparacion | 0 | 67.16 | 100.00 | 4600 |
| kmp | Busqueda | 500 | 719.64 | 600.00 | 23900 |
| kmp | Metodo completo | 3200 | 4586.48 | 3900.00 | 58200 |
| boyer-moore | Preparacion | 600 | 1950.56 | 900.00 | 94000 |
| boyer-moore | Busqueda | 6500 | 9074.64 | 7000.00 | 292600 |
| boyer-moore | Metodo completo | 9900 | 15107.14 | 11500.00 | 304400 |
| rabin-karp | Preparacion | 300 | 337.88 | 300.00 | 11800 |
| rabin-karp | Busqueda | 21400 | 24205.52 | 22400.00 | 2233300 |
| rabin-karp | Metodo completo | 23200 | 27205.44 | 24500.00 | 2238900 |
