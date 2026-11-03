# Búsqueda exacta: infraestructura y KMP

Esta fase implementa únicamente el contrato común, Knuth–Morris–Pratt desde cero, servicio, endpoint y pruebas. No incorpora Boyer–Moore, Rabin–Karp, comparación empírica definitiva, interfaz final ni T(n).

## Texto y exactitud

Se busca sobre `abstractProcesado` del JSON existente mediante `JsonUtil.cargarArticulosDesdeJson`. No se vuelven a leer los PDF ni se regenera el corpus si el JSON falta. Se conservan el preprocesamiento, los abstracts originales y todos los algoritmos anteriores.

La búsqueda es exacta respecto del texto procesado: no transforma, recorta, convierte a minúsculas ni normaliza el patrón. Por tanto, `ChatGPT` no coincide con `chatgpt`. Los espacios y la puntuación son significativos. Un patrón formado por espacios es válido si no está vacío.

Se buscan subcadenas, no palabras completas: `chatgpt` también coincide dentro de `chatgpt's`. No es búsqueda semántica.

## Posiciones y contrato común

`BusquedaExacta.buscar(texto, patron)` permite incorporar después los otros algoritmos sin cambiar el significado del resultado. `ResultadoBusqueda` contiene algoritmo, patrón, longitudes, posiciones, ocurrencias, encontrado, comparaciones de búsqueda, comparaciones de preparación y tiempos separados en nanosegundos. Las posiciones son inmutables.

La respuesta del servicio añade ID y título del artículo, `fuenteTexto` y `unidadPosiciones`.

Las posiciones y longitudes se expresan en **puntos de código Unicode, base 0**, no offsets UTF-16 de `String`. Por ejemplo, en `😀a😀a`, `a` aparece en [1,3]. Para presentar base 1, sumar uno a cada posición; para resaltar texto posteriormente en JavaScript, debe respetarse esa unidad y no tratar automáticamente las posiciones como índices UTF-16.

## KMP y LPS

KMP utiliza dos índices: `i` en el texto y `j` en el patrón. Construye manualmente LPS, donde `LPS[j]` indica la longitud del mayor prefijo propio de `P[0..j]` que también es sufijo.

En una coincidencia se incrementan ambos índices. En una diferencia, si `j>0`, se actualiza `j=LPS[j-1]` sin retroceder `i`. Si `j=0`, se avanza `i`. Al completar el patrón se registra `i-j` y se continúa con `j=LPS[j-1]`, conservando coincidencias superpuestas.

No se utiliza `String.indexOf`, regex, librerías de búsqueda ni otra función que sustituya KMP.

`buscarConTraza` ejecuta el mismo núcleo con registro de pasos para ejemplos pequeños. La traza no se envía por el endpoint normal y su tiempo no debe confundirse con una ejecución sin registro.

## Comparaciones y tiempos

Una comparación de búsqueda es una evaluación real de `t[i] == p[j]`, donde cada elemento es un punto de código. Se incrementa el contador una vez por evaluación, también cuando el mismo carácter del texto se contrasta con otra posición del patrón tras un retroceso LPS. No se cuentan condiciones de bucles, índices, asignaciones ni saltos.

La preparación utiliza su propio contador: evaluaciones de `p[i] == p[longitud]` al construir LPS. **No se suman a `comparaciones`**, que corresponde exclusivamente a la búsqueda.

`tiempoPreparacionNs` mide la construcción de LPS. `tiempoBusquedaNs` mide el bucle de búsqueda, incluidos el registro de posiciones y la comprobación de coincidencias. La conversión a puntos de código, lectura del JSON, respuesta HTTP y serialización no se incluyen en esos tiempos. Las mediciones son diagnósticas, no un benchmark definitivo; la JVM, calentamiento y planificación influyen.

## Casos límite

| Caso | Comportamiento |
|---|---|
| Patrón nulo o vacío | Error; HTTP 400 en el endpoint |
| Texto nulo | Error en el algoritmo |
| Texto vacío y patrón no vacío | Cero ocurrencias y cero comparaciones de búsqueda |
| Patrón más largo que el texto | Cero ocurrencias y cero comparaciones de búsqueda; LPS sí se prepara |
| Patrón inexistente | Posiciones vacías, ocurrencias 0 y encontrado=false |
| Patrón igual al texto | Una coincidencia en 0 |
| Coincidencias múltiples | Se devuelven todas |
| `aaaa` con `aa` | Posiciones [0,1,2], 4 comparaciones de búsqueda y 1 de preparación |
| Artículo inexistente o ID no positivo | HTTP 400 |
| JSON ausente o ilegible | HTTP 500; no se regenera desde PDF |

## Ejemplo controlado

- Texto: `ABABDABACDABABCABAB` (19 puntos de código).
- Patrón: `ABABCABAB` (9 puntos de código).
- LPS obtenida por el código: `[0,0,1,2,0,1,2,3,4]`.
- Comparaciones al preparar LPS: **9**.
- Comparaciones de búsqueda: **23**.
- Posición encontrada: **10**, base 0 (11 en base 1).
- Después del hallazgo, `j` continúa en 4 por LPS, no se reinicia en cero.

La tabla de pasos al final se generó ejecutando `Kmp.buscarConTraza`, no transcribiendo un resultado supuesto.

## Endpoint

`GET /api/busqueda/kmp?articulo=19&patron=chatgpt`

Desde la carpeta `backend`, iniciar el backend con `mvn spring-boot:run`. En otra terminal:

```powershell
Invoke-RestMethod 'http://localhost:8080/api/busqueda/kmp?articulo=19&patron=chatgpt' | ConvertTo-Json -Depth 6
```

Para patrones con espacios u otros caracteres, codificar el parámetro de URL. No existen patrones ni posiciones fijados por ID dentro del algoritmo o servicio.

## Caso provisional real

Artículo 19: **What Is the Impact of ChatGPT on Education? A Rapid Review of the Literature**. Este ejemplo no fija todavía el caso académico definitivo.

- Patrón: `chatgpt`.
- Longitud del abstract procesado: **1425** puntos de código.
- Longitud del patrón: **7**.
- Posiciones: **[42,243,644,825,1386]**, base 0.
- Ocurrencias: **5**.
- Comparaciones de búsqueda: **1471**.
- Comparaciones de preparación: **6**.
- Tres tiempos de búsqueda observados en solicitudes sucesivas: **48.800 ns**, **42.900 ns**, **63.299 ns**.
- Preparación en esas solicitudes: **803.000 ns**, **1.401 ns**, **1.400 ns**.

Estos tiempos corresponden a una ejecución local y solo son diagnósticos; no permiten concluir rendimiento relativo frente a algoritmos aún no implementados.

## Pruebas

Se añadieron 19 pruebas: 12 de KMP, 4 de servicio y 3 HTTP. Cubren inicio, medio, final, múltiples coincidencias, superposiciones, inexistencia, longitudes límite, texto vacío, patrón vacío, Unicode, exactitud de mayúsculas/espacios, LPS conocida, traza, conteo, lectura del procesado, JSON intacto y errores. Una prueba contrasta exhaustivamente con un barrido independiente los 127 textos binarios de longitud 0–6 frente a los 30 patrones binarios de longitud 1–4 (3810 casos).

`mvn test` con ambos modelos IA configurados: **198 aprobadas, 0 fallos, 0 errores y 0 omitidas**. `npm run build` del frontend: correcto, sin cambios al frontend.

## Traza completa del ejemplo controlado

Cada fila representa una única comparación de caracteres. `i` y `j` son los índices antes de comparar; las columnas siguientes muestran sus valores después de procesar igualdad, diferencia o coincidencia completa.

| Comparación | i antes | j antes | Texto / patrón | Resultado | i después | j después | Hallazgo |
|---:|---:|---:|---|---|---:|---:|---:|
| 1 | 0 | 0 | A / A | igual | 1 | 1 | - |
| 2 | 1 | 1 | B / B | igual | 2 | 2 | - |
| 3 | 2 | 2 | A / A | igual | 3 | 3 | - |
| 4 | 3 | 3 | B / B | igual | 4 | 4 | - |
| 5 | 4 | 4 | D / C | distinto | 4 | 2 | - |
| 6 | 4 | 2 | D / A | distinto | 4 | 0 | - |
| 7 | 4 | 0 | D / A | distinto | 5 | 0 | - |
| 8 | 5 | 0 | A / A | igual | 6 | 1 | - |
| 9 | 6 | 1 | B / B | igual | 7 | 2 | - |
| 10 | 7 | 2 | A / A | igual | 8 | 3 | - |
| 11 | 8 | 3 | C / B | distinto | 8 | 1 | - |
| 12 | 8 | 1 | C / B | distinto | 8 | 0 | - |
| 13 | 8 | 0 | C / A | distinto | 9 | 0 | - |
| 14 | 9 | 0 | D / A | distinto | 10 | 0 | - |
| 15 | 10 | 0 | A / A | igual | 11 | 1 | - |
| 16 | 11 | 1 | B / B | igual | 12 | 2 | - |
| 17 | 12 | 2 | A / A | igual | 13 | 3 | - |
| 18 | 13 | 3 | B / B | igual | 14 | 4 | - |
| 19 | 14 | 4 | C / C | igual | 15 | 5 | - |
| 20 | 15 | 5 | A / A | igual | 16 | 6 | - |
| 21 | 16 | 6 | B / B | igual | 17 | 7 | - |
| 22 | 17 | 7 | A / A | igual | 18 | 8 | - |
| 23 | 18 | 8 | B / B | igual | 19 | 4 | 10 |
