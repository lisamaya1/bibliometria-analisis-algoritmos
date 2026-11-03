# Rabin–Karp: hash rodante y verificación exacta

Tercera fase del Requerimiento 3. Implementa Rabin–Karp desde cero y reutiliza `BusquedaExacta`, `ResultadoBusqueda`, servicio y controlador. No modifica KMP, Boyer–Moore, corpus ni preprocesamiento. No incorpora frontend, comparación formal definitiva ni T(n).

## Entradas

Busca sobre el `abstractProcesado` del JSON existente, sin releer PDF ni regenerar el corpus. Texto y patrón no se transforman. La búsqueda es sensible a mayúsculas, espacios y puntuación; detecta subcadenas, también dentro de palabras. Posiciones y longitudes: puntos de código Unicode, base 0, como en los otros algoritmos.

## Constantes y hash

- Base `d = 1.114.112 = 0x110000`: es el número de posiciones del espacio de puntos de código Unicode; cada código recibido es menor que la base.
- Módulo primo fijo `q = 1.000.000.007`: mantiene los hashes acotados y admite multiplicaciones seguras con esta base en `long`.
- No son parámetros elegidos para generar colisiones. Las pruebas utilizan exactamente estas mismas constantes.

Se recorre el patrón y la primera ventana aplicando:

`H = (d × H + codigo) mod q`

Se calcula manualmente `h = d^(m−1) mod q` mediante multiplicaciones modulares sucesivas. No se usa `Math.pow`, una función de búsqueda ni una biblioteca de hashing que oculte el cálculo.

## Rolling hash y negativos

La fórmula matemática entre ventanas es:

`H_siguiente = (d × (H_actual − saliente × h) + entrante) mod q`

Para evitar desbordamiento y corregir el residuo negativo, el código realiza explícitamente:

1. `retirado = (saliente × h) mod q`.
2. `restante = (H_actual − retirado) mod q`.
3. Si `restante < 0`, sumar q.
4. `H_siguiente = (d × restante + entrante) mod q`.

Reducir antes de multiplicar mantiene cada producto por debajo de aproximadamente `1,12 × 10^15`, dentro del rango de `long`. No se recalcula la ventana entera al desplazarse.

## Verificación y colisiones

Se visita cada alineación, avanzando una posición. Si los hashes difieren no se comparan caracteres. Si coinciden, se verifica el patrón de izquierda a derecha, deteniéndose en la primera diferencia. Solo se registra una ocurrencia si todos los caracteres coinciden.

Una igualdad de hash rechazada por la verificación incrementa `colisiones`. Así, una colisión no produce falsos positivos. Las coincidencias superpuestas se conservan: `aaaa` con `aa` produce [0,1,2].

## Contrato y conteos separados

`ResultadoBusqueda` conserva su constructor anterior. Incorpora el campo opcional `estadisticasHash`, omitido del JSON cuando es nulo. Los endpoints de KMP y Boyer–Moore no exponen campos nuevos nulos ni cambian sus resultados.

- `comparaciones`: cada evaluación explícita `t[s+j] == p[j]` durante la verificación. No incluye igualdad de hashes, índices ni operaciones aritméticas.
- `comparacionesPreparacion`: 0, porque preparar los hashes no compara explícitamente dos caracteres. Esto no significa preparación sin trabajo.
- `pasosHashPatron`: iteraciones que incorporan un código al hash del patrón.
- `pasosHashInicial`: iteraciones que incorporan un código al hash de la primera ventana.
- `pasosPotencia`: multiplicaciones modulares utilizadas para obtener h.
- `ventanasProcesadas`: alineaciones visitadas; cada una incluye una comparación numérica de hashes.
- `coincidenciasHash`: igualdades de hash; cada una inicia una verificación de caracteres.
- `colisiones`: igualdades de hash que no superan esa verificación.
- `actualizacionesRolling`: desplazamientos con actualización de hash.

Los pasos de preparación son conteos de iteraciones específicas, no una equivalencia de costos unitarios ni un T(n). No se suman con las comparaciones de caracteres.

Los tiempos en nanosegundos se miden por separado para preparar y buscar. La conversión a puntos de código, lectura JSON, serialización y transporte HTTP quedan fuera. Los registros detallados de `buscarConTraza` se usan solo para ejemplos pequeños, no como benchmark.

## Ejemplo controlado ejecutado

Texto `ABAAABCD`, patrón `ABC`:

- Hash del patrón: **33.622.052**.
- Hash de la primera ventana `ABA`: **33.622.050**.
- `h`: **245.539.857**.

| Alineación | Ventana | Hash | ¿Coincide hash? | Comparaciones de caracteres | Resultado |
|---:|---|---:|---|---:|---|
| 0 | ABA | 33622050 | No | 0 | Sin ocurrencia |
| 1 | BAA | 278047795 | No | 0 | Sin ocurrencia |
| 2 | AAA | 32507938 | No | 0 | Sin ocurrencia |
| 3 | AAB | 32507939 | No | 0 | Sin ocurrencia |
| 4 | ABC | 33622052 | Sí | 3 | A=A, B=B, C=C; posición 4 |
| 5 | BCD | 280276022 | No | 0 | Sin ocurrencia |

La primera actualización tiene una resta `−926468550`. Al corregirla módulo q queda `73531457`; la actualización produce `278047795`, igual al hash recalculado independientemente para BAA. Otras actualizaciones también presentan restas negativas y están cubiertas por pruebas.

Total: 6 ventanas, 5 actualizaciones rolling, una coincidencia de hash, cero colisiones y 3 comparaciones de caracteres. Preparación: 3 pasos del patrón, 3 de ventana inicial y 2 de potencia.

## Colisión controlada con constantes de producción

El patrón `AA` tiene códigos [65,65]. La ventana [962,641608] tiene el mismo hash **72417345**, porque:

`962 = 65 + q / d` y `641608 = 65 + q % d`, usando división entera.

Su polinomio difiere del de AA exactamente en q. En el texto de puntos de código [962,641608,65,65], el código rechaza la ventana inicial al comparar 962 con 65 y solo acepta AA en la posición 2.

Resultado: dos coincidencias de hash, una colisión real, tres comparaciones de caracteres y posiciones [2]. No se alteraron base ni módulo para esta prueba.

## Endpoint

`GET /api/busqueda/rabin-karp?articulo=19&patron=chatgpt`

Con el backend iniciado desde la raíz del proyecto:

```powershell
Invoke-RestMethod 'http://localhost:8080/api/busqueda/rabin-karp?articulo=19&patron=chatgpt' | ConvertTo-Json -Depth 7
```

Se conserva el manejo de errores: patrón vacío/nulo, ID inválido o artículo inexistente producen 400; JSON ausente/ilegible produce 500 sin regeneración desde PDF.

## Caso real común

Artículo 19: **What Is the Impact of ChatGPT on Education? A Rapid Review of the Literature**.

- Patrón `chatgpt`; longitudes texto/patrón: 1425/7.
- Posiciones [42,243,644,825,1386], 5 ocurrencias.
- Hash del patrón: 688447368; hash inicial: 430330438; h: 631020977.
- Ventanas: 1419; actualizaciones rolling: 1418.
- Coincidencias de hash: 5; colisiones: 0.
- Comparaciones de caracteres: 35.
- Preparación: 7 pasos de hash del patrón, 7 de ventana inicial y 6 de potencia; 0 comparaciones explícitas de caracteres.

Tiempos diagnósticos de tres solicitudes locales sucesivas:

| Solicitud | Preparación (ns) | Búsqueda (ns) |
|---:|---:|---:|
| 1 | 1900 | 1286100 |
| 2 | 1500 | 280800 |
| 3 | 1400 | 256400 |

## Comparación provisional de conteos

| Algoritmo | Ocurrencias | Comparaciones de caracteres en búsqueda | Preparación registrada |
|---|---:|---:|---|
| KMP | 5 | 1471 | 6 comparaciones explícitas al construir LPS |
| Boyer–Moore | 5 | 275 | 7 actualizaciones de tabla; 0 comparaciones explícitas de caracteres |
| Rabin–Karp | 5 | 35 | 7 pasos de patrón, 7 de ventana y 6 de potencia; 0 comparaciones explícitas de caracteres |

Los tres devuelven las mismas posiciones. Las 1419 comparaciones numéricas de hashes y las 1418 actualizaciones rolling de Rabin–Karp no están incluidas en sus 35 comparaciones de caracteres. Comparar solo ese último contador puede favorecerlo artificialmente al ocultar el trabajo de hashing. Esta tabla no determina superioridad ni velocidad general.

## Casos límite y limitaciones

Patrón vacío/nulo y texto nulo se rechazan. Texto vacío o patrón más largo producen cero ventanas y cero comparaciones de búsqueda; se preparan hash del patrón y potencia, pero no hash inicial (null). Se admiten patrón igual al texto, múltiples coincidencias, superposiciones y Unicode sin transformaciones.

El hash no es criptográfico ni garantiza ausencia de colisiones. La verificación mantiene la exactitud incluso con colisiones; muchas igualdades de hash pueden aumentar las verificaciones. Los tiempos actuales son diagnósticos y dependen de la ejecución de la JVM. No se realiza aún análisis formal ni comparación empírica definitiva.

## Pruebas

Se añadieron **21 pruebas**: 12 del algoritmo, 5 de servicio y 4 HTTP. Incluyen hash inicial, potencia, rolling de cada ventana contrastado con BigInteger, módulo negativo, colisión real, superposiciones, Unicode de códigos altos, casos límite, caso real, JSON intacto, contrato retrocompatible y regresión de KMP/Boyer–Moore. Se reutiliza la referencia independiente existente para 3810 casos binarios pequeños.

`mvn test`, con ambos modelos IA configurados: **239 aprobadas, 0 fallos, 0 errores y 0 omitidas**. `npm run build`: correcto. No se modificaron el frontend ni los algoritmos de los Requerimientos 1 y 2.
