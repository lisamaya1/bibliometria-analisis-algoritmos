# Boyer–Moore con Bad Character

Segunda fase del Requerimiento 3. Reutiliza `BusquedaExacta`, `ResultadoBusqueda`, servicio, controlador y manejo de errores. No implementa Good Suffix, Rabin–Karp, frontend, comparación final ni T(n).

## Entradas y contrato

Busca todas las subcadenas exactas sobre `abstractProcesado` del JSON existente. No modifica mayúsculas, espacios, puntuación ni patrón. No lee PDF ni regenera el corpus. Las posiciones y longitudes son puntos de código Unicode en base 0, igual que KMP. Para presentación base 1 se suma uno; los offsets no deben interpretarse automáticamente como índices UTF-16.

Se mantiene el mismo contrato de respuesta y el algoritmo se identifica como `boyer-moore`. `BusquedaService` comparte la lectura del JSON y las validaciones entre ambos algoritmos mediante un método privado; KMP y su implementación no se modifican.

## Tabla de última aparición

El patrón se recorre de izquierda a derecha y se registra manualmente `tabla[caracter] = posicion`. Sobrescribir una entrada conserva su última aparición. Se usa un mapa de puntos de código a índices, compatible con Unicode. Si el carácter no aparece, su última aparición es -1.

Para el ejemplo implementado, patrón `ABC`:

| Carácter | Última aparición |
|---|---:|
| A | 0 |
| B | 1 |
| C | 2 |
| Cualquier otro | -1 |

Una prueba adicional usa `ABCA` para verificar la sobrescritura: A=3, B=1, C=2.

## Procedimiento

En cada alineación `s`, se inicia `j=m-1`. Se comparan los caracteres del patrón de derecha a izquierda. En una diferencia, se consulta la última aparición del carácter `texto[s+j]` y se calcula explícitamente:

`desplazamiento = j - ultimaAparicion(texto[s+j])`

Si el resultado es menor que 1 se usa 1; equivale a `max(1, j-last(...))`. No se utilizan funciones externas de búsqueda.

Cuando coincide el patrón completo se registra `s` y se desplaza **una posición**. Esta continuación conservadora permite detectar superposiciones: `aaaa` con `aa` devuelve [0,1,2]. No se utiliza Good Suffix ni otra heurística de desplazamiento posterior a la coincidencia.

## Conteo y preparación

Cada evaluación explícita `t[s+j] == p[j]` cuenta como una comparación de búsqueda. No se cuentan condiciones de bucles, índices, consultas de tabla ni cálculos de desplazamiento. La comparación se evalúa una sola vez antes de decidir si continúa hacia la izquierda.

La preparación del mapa no compara explícitamente dos caracteres. Por eso `comparacionesPreparacion=0`. Esto **no significa preparación gratuita**: `TablaUltimaAparicion.actualizaciones` registra una inserción/sobrescritura por posición del patrón, y `tiempoPreparacionNs` mide su construcción. Las actualizaciones se pueden inspeccionar con `preparar` o `buscarConTraza`; no se mezclan con el contador de comparaciones ni se cambia el contrato común.

Las comparaciones que una implementación de mapa pueda efectuar internamente no se instrumentan ni se les atribuye un costo unitario. Esta regla contabiliza comparaciones explícitas del algoritmo. KMP conserva su contador separado para las igualdades explícitas entre caracteres al construir LPS.

Los tiempos se miden con `System.nanoTime()`. Preparación y búsqueda se miden por separado, después de convertir las entradas a puntos de código. No incluyen lectura del JSON, transporte HTTP ni serialización. Son datos diagnósticos; no constituyen un benchmark formal.

## Ejemplo controlado ejecutado

- Texto: `ABAAABCD`.
- Patrón: `ABC`.
- Tabla: A=0, B=1, C=2.

| Alineación s | Comparaciones desde la derecha | Fallo y j | last | Desplazamiento | Siguiente s | Hallazgo |
|---:|---|---|---:|---:|---:|---:|
| 0 | T[2]=A frente a P[2]=C | A, j=2 | 0 | 2−0=2 | 2 | — |
| 2 | T[4]=A frente a P[2]=C | A, j=2 | 0 | 2−0=2 | 4 | — |
| 4 | C=C, B=B, A=A; j=2,1,0 | Sin fallo | — | 1 | 5 | 4 |
| 5 | T[7]=D frente a P[2]=C | D, j=2 | -1 | 2−(-1)=3 | 8 | — |

El código produjo posición [4], una ocurrencia y **6 comparaciones de búsqueda**. La preparación hizo **3 actualizaciones** y **0 comparaciones explícitas de caracteres**. `buscarConTraza` utiliza el mismo núcleo con registro opcional y permite inspeccionar cada comparación y salto; no se envía la traza por el endpoint normal.

## Endpoint

`GET /api/busqueda/boyer-moore?articulo=19&patron=chatgpt`

Con el backend iniciado desde la raíz del proyecto:

```powershell
Invoke-RestMethod 'http://localhost:8080/api/busqueda/boyer-moore?articulo=19&patron=chatgpt' | ConvertTo-Json -Depth 6
```

Para otros patrones se utiliza el parámetro recibido dinámicamente, codificado en la URL cuando corresponda. La respuesta contiene artículo, fuente del texto, unidad de posiciones y el `ResultadoBusqueda` común.

## Caso real provisional común con KMP

Artículo 19: **What Is the Impact of ChatGPT on Education? A Rapid Review of the Literature**.

- Patrón: `chatgpt`; no se fija dentro del código.
- Longitud del texto: 1425 puntos de código; patrón: 7.
- Posiciones: **[42,243,644,825,1386]**.
- Ocurrencias: **5**, exactamente las mismas que KMP.
- Comparaciones de búsqueda Boyer–Moore: **275**.
- Comparaciones de preparación Boyer–Moore: **0**; actualizaciones de tabla: **7**.
- KMP conserva **1471** comparaciones de búsqueda y **6** de preparación LPS.

El patrón también aparece dentro de `chatgpt's`: se buscan subcadenas, sin restricción de palabra completa.

Tiempos diagnósticos de tres solicitudes locales sucesivas:

| Solicitud | Preparación (ns) | Búsqueda (ns) |
|---:|---:|---:|
| 1 | 915401 | 79899 |
| 2 | 14400 | 56701 |
| 3 | 21800 | 58399 |

Estos conteos y tiempos describen este caso. No permiten concluir superioridad ni rendimiento general; la comparación empírica formal se realizará cuando estén los tres algoritmos.

## Casos límite

Patrón vacío o nulo: error. Texto nulo: error. Patrón más largo que el texto o texto vacío: cero ocurrencias y cero comparaciones de búsqueda, aunque la tabla se prepara. Patrón inexistente: encontrado=false. Patrón igual al texto: posición 0. Se admiten múltiples coincidencias, superposiciones, espacios, puntuación y Unicode sin transformaciones.

Se mantienen HTTP 400 para patrón vacío, ID inválido o artículo inexistente y HTTP 500 si el JSON no se puede leer, sin regeneración desde PDF.

## Pruebas

Se añadieron **20 pruebas**: 12 del algoritmo, 5 del servicio y 3 HTTP. Cubren tabla, repetición de caracteres, saltos, caracteres ausentes, coincidencias al inicio/medio/final, múltiples y superpuestas, entradas límite, Unicode, traza, caso real, JSON intacto y regresión de KMP. Se reutiliza el barrido independiente de `KmpTest` para contrastar **3810 combinaciones pequeñas**.

Ejecución final de `mvn test`, con ambos modelos IA configurados: **218 aprobadas, 0 fallos, 0 errores y 0 omitidas**. `npm run build`: correcto. No se modificó el frontend ni los Requerimientos 1 y 2.
