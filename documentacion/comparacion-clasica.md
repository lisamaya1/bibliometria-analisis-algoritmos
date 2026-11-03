# Comparación clásica con selección múltiple

La pantalla **Comparación clásica** permite elegir entre dos y los 20 artículos
del corpus mediante casillas con ID y título. Indica los seleccionados y el número
de parejas, permite seleccionar todos o limpiar la selección y bloquea la ejecución
con menos de dos. Al cambiar la selección retira resultados anteriores.

## Consulta y generación de parejas

`GET /api/similitud/clasicos?articulos=3,15,19`

Se utiliza GET porque es una consulta que no modifica datos y sigue el patrón
REST de los endpoints individuales; la lista del corpus tiene como máximo 20 IDs.
Los IDs repetidos se cuentan una sola vez. Se requieren al menos dos distintos,
positivos y existentes. La respuesta mantiene el orden del corpus, independientemente
del orden en que se hayan marcado las casillas o enviado los IDs.

El servicio carga el corpus una vez. Recorre los seleccionados con dos ciclos:

```text
para i desde 0 hasta n-1:
    para j desde i+1 hasta n-1:
        comparar seleccionado[i] con seleccionado[j]
```

Así genera `n(n-1)/2` parejas sin identidades ni inversas repetidas. Con 3, 15 y 19
se generan 3 vs 15, 3 vs 19 y 15 vs 19. Con cuatro artículos se generan seis parejas;
con los 20, 190. No hay una biblioteca de combinaciones.

## Reutilización de algoritmos

`ComparacionClasicaService` invoca las clases existentes de Jaccard, Dice y
Levenshtein, sin duplicar sus implementaciones. Jaccard y Dice reciben los tokens
actuales; Levenshtein recibe el abstract procesado completo.

Para Coseno TF-IDF construye una sola representación de **todo** el corpus y usa
`compararRepresentacion` para cada pareja. El endpoint individual continúa usando
el mismo cálculo y la fórmula no cambia: TF crudo e IDF = ln(N/DF). Los documentos
no seleccionados también intervienen en IDF. No se cambia el JSON ni el
preprocesamiento. Se mantiene el comportamiento previo del servicio de corpus
de regenerar el JSON si falta; la verificación usa el archivo existente.

Las comparaciones son secuenciales. No se almacenan matrices de Levenshtein en la
respuesta. Se devuelven metadatos, cantidad de parejas, N para IDF y resultados
resumidos: similitud y porcentaje de cada método, además de distancia de edición.
No se devuelven conjuntos ni vectores completos; sus pantallas individuales siguen
disponibles para inspección.

IDs inválidos, insuficientes o inexistentes generan HTTP 400. Si alguna comparación
no está definida (por ejemplo, norma TF-IDF cero), se rechaza la solicitud con
mensaje de la pareja afectada, sin devolver una tabla parcial como completa.
Errores de lectura generan HTTP 500 con un mensaje general. El frontend muestra
el mensaje y permite volver a ejecutar. El corpus actual admite las comparaciones.

## Tabla registrada del caso académico vigente: 15 vs 19

| Algoritmo | Similitud decimal aproximada | Porcentaje aproximado | Distancia |
|---|---:|---:|---:|
| Jaccard | 0,1369294606 | 13,69 % | No aplica |
| Sørensen-Dice | 0,2408759124 | 24,09 % | No aplica |
| Levenshtein normalizado | 0,2391598916 | 23,92 % | 1123 |
| Coseno TF-IDF | 0,0385889811 | 3,86 % | No aplica |

Estos valores provienen del corpus vigente. El frontend no los fija manualmente.
La selección múltiple es funcionalidad de la aplicación; la pareja académica
permanece en los artículos 15 y 19.

## Interpretación

Los resultados no representan exactamente el mismo concepto de similitud y no
son una competencia donde el porcentaje mayor indique el mejor algoritmo:

- Jaccard mide coincidencia respecto a la unión de términos únicos.
- Dice usa el doble de la intersección respecto a la suma de tamaños de conjuntos.
- Levenshtein normalizado mide cercanía textual mediante operaciones de edición.
- Coseno TF-IDF compara vectores ponderados por importancia de los términos.

La pantalla muestra esta distinción de forma visible y no declara ganadores.

## Verificación y alcance

Ejecutar `mvn test` y, desde `frontend`, `npm.cmd run build`.
Las pruebas verifican dos, tres y cuatro artículos; pares sin repetición;
selección insuficiente; IDs repetidos e inválidos; orden independiente; carga única;
caso real y coherencia con las respuestas HTTP individuales. Se prueba también
que reutilizar la representación TF-IDF conserva los resultados y que sus índices
se validan. No se implementan IA, clustering, búsqueda de patrones ni T(n).
