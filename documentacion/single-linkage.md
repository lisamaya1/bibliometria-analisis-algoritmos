# Clustering jerárquico: infraestructura y Single Linkage

Esta fase utiliza los 20 abstracts del corpus vigente, sin modificar su texto ni su preprocesamiento. Solo implementa infraestructura común y Single Linkage. Complete Linkage, Average Linkage, Silhouette y la visualización frontend quedan pendientes.

## Matriz común

`MatrizDistancias.desdeCorpus` toma los tokens ya cargados por `PreprocesamientoService` y construye una sola representación con `CosenoTfidf.construirRepresentacion`. Se mantienen TF absoluto e IDF `ln(N/DF)`, con todos los documentos del corpus. No se vuelve a vectorizar para cada pareja.

La disimilitud utilizada es:

`d(i,j) = 1 - cosenoTFIDF(i,j)`

Se calcula cada pareja `i < j` una vez y se copia su valor a ambas posiciones; la diagonal se fija en cero. Se valida que todos los vectores tengan coseno definido, incluso para la diagonal. La matriz rechaza formas incorrectas, IDs duplicados o inválidos, asimetría, diagonal no nula, NaN, infinito y valores fuera de [0,1]. Conserva una copia defensiva y permite consultar por ID de artículo, sin asumir que el ID corresponde a una posición.

Para el corpus real es una matriz 20 × 20. Como control, `d(15,19) = 0.9614110189106007`. El complemento del coseno se utiliza aquí como disimilitud; no se necesita asumir desigualdad triangular para Single Linkage.

## Nodos e historial

`Cluster` es un nodo inmutable con ID, artículos, hijo izquierdo, hijo derecho, distancia y tamaño. Las hojas usan el ID del artículo y distancia cero. Cada fusión conserva los nodos hijos y una copia de sus artículos, sin duplicaciones.

Los IDs nuevos comienzan en `max(ID de artículo) + 1`: en el corpus actual son 21 a 39. El historial usa registros compactos `Fusion(id, izquierdo, derecho, articulos, distancia, tamano)`; los hijos se referencian por ID, evitando serializar repetidamente subárboles completos.

## Algoritmo explícito y empates

1. Crear un cluster por artículo.
2. Recorrer todas las parejas diferentes de clusters activos.
3. Para cada pareja, recorrer los pares de artículos de ambos clusters y tomar el mínimo de la matriz original:

   `D_single(C1,C2) = min { d(x,y) : x en C1, y en C2 }`.

4. Elegir la pareja de menor distancia, fusionarla, registrar el resultado y retirar los dos hijos del conjunto activo.
5. Repetir hasta conservar una sola raíz.

En un empate exacto de distancia (`Double.compare`), se elige el menor ID del primer cluster y después el menor ID del segundo. Cada pareja se ordena primero por sus IDs; el hijo izquierdo siempre tiene el ID menor. No se utiliza tolerancia para convertir distancias diferentes en empates. Las decisiones no dependen del orden de recorrido de la matriz.

No se utilizan bibliotecas de clustering. Las operaciones principales para un análisis futuro son construir TF-IDF, calcular distancias, recorrer parejas de clusters, buscar mínimos entre miembros y registrar fusiones. No se realiza aún el conteo formal T(n).

## Prueba controlada de cuatro elementos

| Artículo | 1 | 2 | 3 | 4 |
|---|---:|---:|---:|---:|
| 1 | 0 | 0,1 | 0,6 | 0,8 |
| 2 | 0,1 | 0 | 0,3 | 0,7 |
| 3 | 0,6 | 0,3 | 0 | 0,4 |
| 4 | 0,8 | 0,7 | 0,4 | 0 |

Primera fusión: `{1}` con `{2}` a 0,1, creando el nodo 5.

- `D({1,2},{3}) = min(0,6; 0,3) = 0,3`.
- `D({1,2},{4}) = min(0,8; 0,7) = 0,7`.
- `D({3},{4}) = 0,4`.

Segunda fusión: `{3}` con el nodo 5 a 0,3, creando el nodo 6 con `{1,2,3}`.

Finalmente, `D({1,2,3},{4}) = min(0,8; 0,7; 0,4) = 0,4`. Se fusionan los nodos 4 y 6 creando la raíz 7. Se obtienen tres fusiones, verificadas por pruebas automatizadas, que también contrastan las distancias intermedias explícitamente.

## Endpoint y dendrograma posterior

`GET /api/clustering/single`

Devuelve `criterio`, `cantidadArticulos`, `distanciaBase`, `articulos` (IDs y títulos), `raiz` y `fusiones`. Para el corpus actual hay 20 hojas, 19 fusiones y raíz 39. No se añade un endpoint de matriz porque la matriz ya puede inspeccionarse y probarse internamente.

El dendrograma posterior colocará los artículos como hojas a altura cero. En orden de historial, conectará los dos IDs hijos de cada fusión a la altura indicada por `distancia`. La última fusión identifica la raíz. Los títulos y miembros permiten etiquetar las hojas y comprobar cada subárbol. La respuesta contiene toda esta información, pero esta fase no dibuja el dendrograma ni selecciona un corte o un agrupamiento mejor.

Datos inválidos o coseno indefinido producen HTTP 400 con explicación; un error de lectura produce HTTP 500 sin exponer detalles internos.

## Verificación

Se añadieron 15 pruebas: 5 de matriz, 5 de Single Linkage, 2 de servicio y 3 de controlador. Cubren las 400 posiciones de la matriz real frente al coseno existente, integridad de distancias, ejemplo controlado, desempates, distancia cero, inmutabilidad, IDs, 19 fusiones, reproducibilidad, conservación del JSON y contrato HTTP.

Ejecución final con `MINILM_MODEL_DIR` y `BGE_MODEL_DIR` configuradas: `mvn test` aprobó **143 pruebas**, sin fallos, errores ni omisiones, incluidas las pruebas reales de los algoritmos anteriores. `npm run build` en `frontend` compiló correctamente. No se modificó el frontend ni los algoritmos de similitud.

## Historial real de los 20 artículos

Las filas siguientes se registran desde la respuesta real del endpoint. Los IDs 1–20 son artículos; 21–39 son clusters nuevos. La pertenencia se reconstruye siguiendo los hijos, y el endpoint también devuelve explícitamente todos los artículos de cada fusión.

| Paso | Nuevo cluster | Hijo izquierdo | Hijo derecho | Distancia | Articulos |
|---:|---:|---:|---:|---:|---:|
| 1 | 21 | 2 | 6 | 0.8324093325358022 | 2 |
| 2 | 22 | 1 | 11 | 0.8476840653718773 | 2 |
| 3 | 23 | 12 | 20 | 0.8578908916091218 | 2 |
| 4 | 24 | 5 | 21 | 0.8603436430117323 | 3 |
| 5 | 25 | 9 | 24 | 0.8624168595720059 | 4 |
| 6 | 26 | 10 | 25 | 0.8810203707246097 | 5 |
| 7 | 27 | 23 | 26 | 0.8833965693734883 | 7 |
| 8 | 28 | 8 | 27 | 0.8841211362227599 | 8 |
| 9 | 29 | 17 | 19 | 0.9015671227962578 | 2 |
| 10 | 30 | 28 | 29 | 0.9047059028272006 | 10 |
| 11 | 31 | 22 | 30 | 0.9059327765055991 | 12 |
| 12 | 32 | 16 | 31 | 0.9073028875060953 | 13 |
| 13 | 33 | 15 | 32 | 0.9169238646755918 | 14 |
| 14 | 34 | 4 | 33 | 0.9237635408890643 | 15 |
| 15 | 35 | 18 | 34 | 0.9262239559102410 | 16 |
| 16 | 36 | 7 | 35 | 0.9264799162444617 | 17 |
| 17 | 37 | 14 | 36 | 0.9282443559025819 | 18 |
| 18 | 38 | 13 | 37 | 0.9307330007281855 | 19 |
| 19 | 39 | 3 | 38 | 0.9327793907732033 | 20 |
