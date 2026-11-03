# Similitud de Sørensen-Dice

Dice compara conjuntos de términos distintos de dos abstracts. Se calcula como:

`Dice(A, B) = 2 × |A ∩ B| / (|A| + |B|)`

## Funcionamiento e integración

`Dice` construye dos conjuntos `LinkedHashSet` recorriendo los tokens. Después
recorre A y añade a la intersección cada término que también esté en B.
Calcula explícitamente el doble de la cardinalidad de la intersección y lo divide
por la suma de las cardinalidades individuales. No usa bibliotecas de similitud.

`DiceService` reutiliza `PreprocesamientoService.obtenerCorpus` y los tokens
actuales de `ArticuloDocumental`, sin retokenizar, normalizar o filtrar nuevamente.
Los títulos y autores no intervienen en el cálculo. No cambia el corpus ni Jaccard.
El comportamiento existente de regenerar el JSON si falta permanece en el servicio
de preprocesamiento; las pruebas del corpus leen únicamente el archivo existente.

Se conserva la misma política de vacíos de Jaccard: si solo un conjunto está vacío,
el resultado es cero; dos conjuntos vacíos se rechazan porque el cociente sería
0/0. Listas nulas y tokens nulos o blancos se rechazan. Las repeticiones no cambian
el resultado y comparar un artículo no vacío consigo mismo produce 1.

## Endpoint y pantalla

`GET /api/dice?articuloA=15&articuloB=19`

Ambos parámetros son obligatorios. La respuesta contiene `articuloA`, `articuloB`,
`resultado` y `porcentaje`. Dentro de `resultado` se incluyen `conjuntoA`,
`conjuntoB`, `interseccion`, `cardinalidadA`, `cardinalidadB`,
`cardinalidadInterseccion` y `similitud`. Los identificadores se consultan en
`GET /api/articulos`. Los parámetros inválidos, artículos inexistentes y dos
conjuntos vacíos producen HTTP 400; los errores de lectura producen HTTP 500.

La opción **Sørensen-Dice** del menú abre su pantalla propia. Reutiliza los estilos
de Jaccard, carga los artículos desde el backend y consulta `/api/dice` al comparar.
Muestra los títulos, porcentaje, decimal y cardinalidades. Los términos completos
solo aparecen en detalles desplegables. Al cambiar la selección se retira el
resultado anterior; permite comparar un artículo consigo mismo.

## Caso académico: artículos 15 y 19

Con el preprocesamiento vigente:

| Medida | Resultado |
|---|---:|
| Tamaño del conjunto A | 133 |
| Tamaño del conjunto B | 141 |
| Intersección | 33 |
| Sustitución | 2 × 33 / (133 + 141) |
| Resultado exacto | 66/274 = 33/137 |
| Decimal aproximado | 0,2408759124 |
| Porcentaje aproximado | 24,09 % |

Para 15 frente a 15: `2 × 133 / (133 + 133) = 1 = 100 %`.
Los valores del servicio y del frontend se obtienen de los tokens; no se fijan
resultados por identificador.

## Interpretación y diferencia frente a Jaccard

Dice expresa coincidencia léxica, sin tomar en cuenta frecuencia ni orden. No
equivale a porcentaje de plagio o similitud semántica y no se asigna un umbral.

Jaccard divide la intersección por la unión; Dice divide el doble de la intersección
por la suma de tamaños individuales. Esa suma cuenta dos veces los términos
compartidos. Para los mismos conjuntos no vacíos, `Dice = 2J / (1 + J)`:
sus escalas son diferentes, aunque ambas producen 0 para conjuntos disjuntos
y 1 para conjuntos iguales. Que Dice obtenga 24,09 % y Jaccard 13,69 % en esta
pareja no significa que uno detecte mejor el significado de los abstracts.

## Pruebas y alcance

Ejecutar `mvn test`. Las pruebas abarcan igualdad, disjunción, coincidencia parcial,
vacíos, valores inválidos, simetría, límites e inmutabilidad. Las pruebas de servicio
contrastan el caso real con una referencia independiente basada en listas,
comprueban la conservación del JSON y el uso exclusivo de tokens. Las del
controlador verifican la respuesta real del caso de estudio y errores HTTP.

El análisis formal T(n) queda pendiente de la metodología del profesor. Esta fase
no incorpora otros algoritmos de similitud, IA, clustering o búsqueda de patrones.
