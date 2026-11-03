# Complete Linkage

Segunda fase del Requerimiento 2. Usa el mismo corpus de 20 artículos, tokens vigentes, representación TF-IDF y construcción de matriz de Single Linkage. No cambia corpus, preprocesamiento ni algoritmos de similitud.

## Distancia y aglomeración

La matriz base sigue siendo `d(x,y) = 1 - cosenoTFIDF(x,y)`. `MatrizDistancias` construye una representación TF-IDF global por ejecución y cada pareja de artículos se evalúa una vez. No se recalcula TF-IDF para cada pareja de clusters.

`CompleteLinkage.distancia` recorre explícitamente los artículos de dos clusters y conserva la mayor distancia observada:

`D_complete(C1,C2) = max { d(x,y) : x pertenece a C1, y pertenece a C2 }`.

Se inicia con una hoja por artículo. En cada iteración se calculan las distancias Complete entre las parejas activas y se elige la pareja con menor distancia Complete. Se fusionan sus nodos, se registra la fusión y se repite hasta obtener una raíz. El máximo define la distancia entre dos clusters; el mínimo selecciona qué pareja fusionar.

## Reutilización y estabilidad de Single

Se extrajo el procedimiento ya existente, sin cambiar sus decisiones, a `AglomeradorJerarquico`: inicialización, búsqueda de pareja, desempates, creación de IDs, registro de fusiones, validaciones comunes y resultado. Sus registros `Fusion` y `Resultado` son compartidos. `SingleLinkage` conserva su recorrido por mínimo y hereda ese procedimiento; Complete aporta únicamente el recorrido por máximo. No se incorporan bibliotecas ni otros criterios.

La regla de empate sigue siendo distancia exactamente igual, menor primer ID y después menor segundo ID. Cada pareja se ordena por ID; el hijo izquierdo tiene el ID menor. Las fusiones comienzan en `max(ID de artículo) + 1`.

`ClusteringService` reutiliza el mismo método privado para cargar corpus, construir matriz y formar la respuesta; cada endpoint ejecuta el criterio solicitado. No hay caché compartida entre solicitudes: la matriz se reconstruye con exactamente los mismos datos y reglas. Las pruebas ejecutan ambos criterios sobre una misma instancia de matriz y verifican que permanece intacta.

Una prueba de regresión contrasta las 19 parejas y distancias anteriores de Single. Además, la respuesta HTTP completa de Single se comparó con el JSON guardado antes de la extracción: coincide íntegramente, incluidos miembros, alturas, IDs y metadatos.

## Prueba controlada

Se utiliza la misma matriz de cuatro elementos que en Single:

| Elemento | 1 | 2 | 3 | 4 |
|---|---:|---:|---:|---:|
| 1 | 0 | 0,1 | 0,6 | 0,8 |
| 2 | 0,1 | 0 | 0,3 | 0,7 |
| 3 | 0,6 | 0,3 | 0 | 0,4 |
| 4 | 0,8 | 0,7 | 0,4 | 0 |

1. Fusión 5: `{1}` con `{2}`, a distancia 0,1.
2. `D_complete({1,2},{3}) = max(0,6; 0,3) = 0,6`; `D_complete({1,2},{4}) = max(0,8; 0,7) = 0,8`. Como `d(3,4) = 0,4`, la segunda fusión une `{3}` y `{4}` creando el nodo 6 a 0,4.
3. `D_complete({1,2},{3,4}) = max(0,6; 0,8; 0,3; 0,7) = 0,8`. Se fusionan 5 y 6 creando la raíz 7.

Resultado: tres fusiones a alturas 0,1; 0,4; 0,8. Single, sobre esta misma matriz, produce 0,1; 0,3; 0,4, con otra segunda fusión.

## Endpoint y dendrograma

`GET /api/clustering/complete`

Conserva la estructura de `/api/clustering/single`: `criterio`, `cantidadArticulos`, `distanciaBase`, `articulos`, `raiz` y `fusiones`. El criterio es `complete`. El corpus real produce 19 fusiones y raíz 39, con 20 artículos. No se envía la matriz. Los errores de datos se informan con HTTP 400 y los de lectura con HTTP 500.

El historial conserva ID del nodo, hijos, miembros, altura y tamaño. Las hojas son los IDs de artículos a altura cero; cada fila permite dibujar una unión a su distancia y la última representa la raíz. No se ha implementado el frontend del dendrograma.

## Comparación preliminar objetiva con Single

- Las primeras tres fusiones coinciden en miembros y distancia: `{2,6}`, `{1,11}` y `{12,20}`.
- La cuarta une `{5}` con `{2,6}` en ambos. Single registra `0.8603436430117323`; Complete registra `0.8794346648997046`. Esta es la primera diferencia del historial.
- En la quinta cambia la composición: Single añade 9 al grupo `{5,2,6}`, mientras Complete crea `{8,10}`.
- En la última fusión, Single incorpora el artículo 3 al resto a `0.9327793907732033`; Complete incorpora el artículo 14 al resto a `0.9911273471622725`.
- Los IDs de clusters nuevos son locales a cada historial: después de la divergencia, un mismo ID no implica los mismos miembros.

Estas diferencias describen el comportamiento observado; no permiten concluir cuál agrupamiento es más coherente. No se implementó Silhouette ni se seleccionó un criterio mejor.

## Pruebas

Se añadieron 10 pruebas: 5 del algoritmo, 2 de servicio/regresión y 3 HTTP. Cubren máximo explícito, ejemplo completo, empates, orden de entrada, distancias cero, IDs, matriz sin modificación, las 19 fusiones reales, raíz con 20 artículos, máximos de cada fusión real, miembros sin duplicación, historial reproducible, preservación del JSON y errores HTTP.

`mvn test`, con ambos modelos IA configurados: **153 aprobadas, 0 fallos, 0 errores y 0 omitidas**. `npm run build` del frontend: correcto. No se modificó el frontend.

## Historial real

Las filas siguientes se registraron desde el endpoint ejecutado sobre el corpus actual. Los IDs 1–20 representan artículos y 21–39 representan clusters. El endpoint devuelve también los miembros explícitos; pueden reconstruirse mediante los hijos.

| Paso | Nuevo cluster | Hijo izquierdo | Hijo derecho | Distancia | Tamano |
|---:|---:|---:|---:|---:|---:|
| 1 | 21 | 2 | 6 | 0.8324093325358022 | 2 |
| 2 | 22 | 1 | 11 | 0.8476840653718773 | 2 |
| 3 | 23 | 12 | 20 | 0.8578908916091218 | 2 |
| 4 | 24 | 5 | 21 | 0.8794346648997046 | 3 |
| 5 | 25 | 8 | 10 | 0.8841211362227599 | 2 |
| 6 | 26 | 17 | 19 | 0.9015671227962578 | 2 |
| 7 | 27 | 9 | 24 | 0.9091724685488292 | 4 |
| 8 | 28 | 15 | 22 | 0.9295071353001251 | 3 |
| 9 | 29 | 7 | 27 | 0.9427044974686034 | 5 |
| 10 | 30 | 13 | 18 | 0.9459186223740660 | 2 |
| 11 | 31 | 3 | 28 | 0.9520039400153802 | 4 |
| 12 | 32 | 4 | 23 | 0.9538426966107149 | 3 |
| 13 | 33 | 25 | 32 | 0.9672751266318560 | 5 |
| 14 | 34 | 16 | 26 | 0.9687014425623537 | 3 |
| 15 | 35 | 31 | 33 | 0.9727685541402682 | 9 |
| 16 | 36 | 30 | 34 | 0.9741095725813127 | 5 |
| 17 | 37 | 29 | 35 | 0.9840599393474511 | 14 |
| 18 | 38 | 36 | 37 | 0.9886913702425133 | 19 |
| 19 | 39 | 14 | 38 | 0.9911273471622725 | 20 |
