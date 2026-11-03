# Jaccard integrado al backend

`Jaccard` adapta la implementación histórica `JaccardSimilarity` de la rama
`feature/pdf-processing`. Conserva los recorridos explícitos para construir los
conjuntos, su intersección y su unión mediante `LinkedHashSet`. No utiliza una
biblioteca de similitud. Sus ocho pruebas unitarias se adaptaron al paquete actual
y se añadieron pruebas del servicio, del corpus real y del endpoint.

## Entradas y cálculo

`JaccardService` reutiliza `PreprocesamientoService.obtenerCorpus` y compara
exclusivamente `ArticuloDocumental.tokens()`. No vuelve a tokenizar, no usa el título
ni los autores y no aplica el filtro de palabras comunes de los ordenamientos.
El JSON y los resúmenes originales no se modifican al consultar el corpus existente.
Si falta el JSON, el servicio existente puede reconstruirlo desde los PDF; no se
cambia ese comportamiento en esta fase.

Los tokens repetidos se cuentan una sola vez. La fórmula es:

`J(A,B) = |A ∩ B| / |A ∪ B|`

La respuesta incluye los dos conjuntos, la intersección, la unión, la similitud
entre 0 y 1 y el porcentaje. Sus tamaños permiten revisar las cardinalidades.
Un único conjunto vacío produce 0. Dos conjuntos vacíos se rechazan porque
producirían 0/0. Se conserva la política de rechazo utilizada por la consola
histórica; no se trasladan las convenciones opcionales ZERO y ONE que no se
necesitan en esta integración. Se rechazan listas nulas y tokens nulos o blancos.
Comparar un artículo no vacío consigo mismo está permitido y produce 1.

## Consulta

Desde la carpeta `backend`, iniciar el backend con `mvn spring-boot:run`.
En otra terminal PowerShell:

```powershell
Invoke-RestMethod 'http://localhost:8080/api/jaccard?articuloA=15&articuloB=19' | ConvertTo-Json -Depth 8
```

Ambos identificadores son obligatorios y corresponden a los devueltos por
`GET /api/articulos`. Se pueden seleccionar otras parejas. Un identificador
inválido, inexistente o una unión vacía devuelve HTTP 400; un error de lectura
devuelve HTTP 500 con un mensaje general. Los parámetros faltantes o no numéricos
también devuelven HTTP 400. Se utiliza inyección por constructor como en el servicio
de comparación de ordenamientos. No se añade una pantalla en esta fase.

## Caso académico vigente: artículos 15 y 19

Con los tokens del JSON actual:

| Medida | Valor |
|---|---:|
| Tamaño del conjunto A (artículo 15) | 133 |
| Tamaño del conjunto B (artículo 19) | 141 |
| Intersección | 33 |
| Unión | 241 |
| Jaccard | 33/241 ≈ 0,136929 |
| Porcentaje | 13,69 % |

Es coincidencia léxica de conjuntos, no una medida de plagio ni una equivalencia
semántica entre documentos. No se asigna un umbral de similitud.

### Resultado histórico

`31/243 ≈ 12,76 %` corresponde únicamente a una configuración anterior del
preprocesamiento. La normalización vigente de ligaduras Unicode transforma
`artiﬁcial` en `artificial` y `ﬁndings` en `findings` en el artículo 19.
Ambas palabras ya estaban normalizadas en el artículo 15: aparecen dos
coincidencias adicionales. Los tamaños individuales siguen siendo 133 y 141,
pero la intersección pasa de 31 a 33 y la unión de 243 a 241.
No se revierten reglas para reproducir el resultado histórico.

## Verificación y alcance

Ejecutar `mvn test`. Las pruebas comprueban igualdad, disjunción, coincidencia
parcial, vacíos, entradas inválidas, simetría, límites, inmutabilidad, selección de
artículos, uso exclusivo de tokens y respuestas HTTP. El caso real contrasta los
conjuntos con una referencia independiente basada en listas y verifica que no
cambie el archivo JSON. No requiere leer nuevamente los PDF.

El conteo formal T(n) queda pendiente de la metodología del profesor. Esta fase
no incorpora otros algoritmos de similitud, IA, clustering ni búsqueda de patrones.
