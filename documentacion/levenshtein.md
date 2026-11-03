# Distancia de Levenshtein

Compara los abstracts procesados completos de los artículos, carácter por carácter,
sin retokenizar, filtrar ni cambiar el preprocesamiento. Incluye espacios,
puntuación y repeticiones. Las longitudes y las ediciones se cuentan por puntos de
código Unicode, no por unidades UTF-16. No se normalizan caracteres dentro del
algoritmo; se usa exactamente `ArticuloDocumental.abstractProcesado()`.

## Recurrencia implementada

La celda `D[i][j]` contiene la distancia entre los primeros `i` caracteres de A y
los primeros `j` de B. Se inicializan `D[i][0] = i` y `D[0][j] = j`. Cada celda es:

`D[i][j] = min(D[i-1][j] + 1, D[i][j-1] + 1, D[i-1][j-1] + costo)`

Los términos representan eliminación, inserción y sustitución/coincidencia,
respectivamente. `costo` es 0 si los caracteres coinciden y 1 si difieren.
Dos ciclos anidados llenan la matriz completa. El resultado es la celda final.
No se utilizan bibliotecas de distancia de edición ni optimización a dos filas.

`Levenshtein.construirMatriz(textoA, textoB)` permite obtener la matriz completa
para una demostración académica posterior con fragmentos pequeños de los artículos
15 y 19. Las inicializaciones y decisiones son explícitas en el código. No se
redacta todavía el caso matemático completo ni se publica la matriz vía HTTP.

## Normalización y vacíos

`similitud = 1 - distancia / max(longitudA, longitudB)`

La distancia está entre cero y la longitud máxima: basta sustituir los caracteres
de la parte común y agregar/eliminar los restantes para no superar esa longitud.
La similitud, por tanto, está entre 0 y 1. Textos idénticos tienen distancia 0 y
similitud 1. Un texto vacío frente a uno no vacío tiene distancia igual a la
longitud del segundo y similitud 0. Para dos textos vacíos se define explícitamente
distancia 0 y similitud 1, evitando dividir por cero. Textos nulos se rechazan.

## Backend y pantalla

`GET /api/levenshtein?articuloA=15&articuloB=19`

`LevenshteinService` reutiliza el servicio de corpus existente; no modifica sus
reglas ni el comportamiento de regeneración desde PDF cuando falta el JSON.
La respuesta incluye `articuloA`, `articuloB`, `resultado` y `porcentaje`.
`resultado` contiene `distancia`, `longitudA`, `longitudB` y `similitud`.
No incluye textos ni matriz. Identificadores inválidos o inexistentes producen
HTTP 400; un error de lectura produce HTTP 500 con mensaje general.

La opción **Levenshtein** en el menú abre una pantalla propia coherente con Jaccard
y Dice. Carga los selectores mediante `/api/articulos` y ejecuta la comparación
en el backend. Muestra títulos, distancia, longitudes, similitud normalizada y
porcentaje; maneja carga, selección incompleta, cambios de selección y errores.

## Caso real actual: artículos 15 y 19

| Medida | Resultado |
|---|---:|
| Longitud del artículo 15 | 1476 |
| Longitud del artículo 19 | 1425 |
| Distancia | 1123 |
| Similitud | 1 - 1123/1476 = 353/1476 ≈ 0,2391598916 |
| Porcentaje | 23,915989… % ≈ 23,92 % |

Para 15 frente a 15 las dos longitudes son 1476, la distancia es 0 y la similitud
normalizada es 1 (100 %). Los resultados se calculan desde el corpus; no hay
excepciones por identificador.

Este valor describe cercanía textual basada en ediciones, no equivalencia
semántica ni porcentaje de plagio. No se asignan umbrales. A diferencia de Jaccard
y Dice sobre conjuntos, aquí influyen orden, repetición, espacios y puntuación.

La matriz del caso 15–19 tiene 1477 × 1426 celdas, con unos 8,4 MB de enteros
sin contar cabeceras y arreglos auxiliares. La longitud máxima observada del corpus
durante la revisión fue 1817 puntos de código. No se necesitó cambiar la versión
clásica por razones de memoria o tiempo en las verificaciones realizadas.

## Verificación y alcance

Ejecutar `mvn test`. Hay pruebas del algoritmo para identidad, vacíos, inserción,
eliminación, sustitución, caso conocido, simetría, límites, matriz completa, Unicode,
entradas nulas y conservación de espacios/puntuación. Las pruebas de servicio
comprueban el caso real, identidad, corpus intacto, uso del abstract procesado,
carga única e identificadores inválidos. Las de controlador verifican la respuesta
HTTP sin matriz, parámetros y errores de lectura.

No se realiza el análisis formal T(n) en esta fase. No se modifican Jaccard, Dice,
corpus, preprocesamiento u ordenamientos y no se incorporan otros algoritmos.
