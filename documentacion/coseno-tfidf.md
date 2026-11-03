# Similitud del Coseno con TF-IDF

Se utilizan los tokens vigentes de los abstracts procesados. No se cambia el
corpus, no se retokeniza, no se eliminan palabras comunes o números y no se
modifican Jaccard, Dice ni Levenshtein. No se utilizan bibliotecas de TF-IDF o coseno.

## Variante exacta

- **TF(t,d) = frecuencia(t,d)**: número de apariciones del término en el documento.
  No se divide por su longitud ni se aplica logaritmo a TF.
- **DF(t)**: número de documentos que contienen el término al menos una vez.
  Las repeticiones dentro de un artículo no incrementan DF.
- **IDF(t) = ln(N / DF(t))**, con logaritmo natural, sin suavizado y sin sumar 1.
  N es la cantidad real de documentos del corpus cargado (actualmente 20),
  incluyendo los no seleccionados para la comparación.
- **Peso(t,d) = TF(t,d) × IDF(t)**.

Se eligió esta variante por su sencillez matemática. Un término presente en todos
los documentos tiene IDF = ln(1) = 0. No se sustituye este cero por otro peso.

## Implementación explícita

`CosenoTfidf.construirRepresentacion` recorre los tokens, forma un vocabulario
global de términos distintos y cuenta las frecuencias de cada documento.
Para cada término cuenta DF recorriendo los documentos y calcula IDF. Después
construye cada vector en el mismo orden del vocabulario, con un peso por término;
los términos ausentes tienen TF y peso cero.

`comparar` toma los dos vectores seleccionados y acumula con un ciclo:

`productoPunto = Σ pesoA[i] × pesoB[i]`

`normaA = sqrt(Σ pesoA[i]²)` y `normaB = sqrt(Σ pesoB[i]²)`

`coseno(A,B) = productoPunto / (normaA × normaB)`

Los pesos son no negativos; el resultado matemático está entre 0 y 1. Se acotan
posibles desviaciones de redondeo a ese intervalo. Para vectores no nulos iguales,
el coseno es 1 dentro de tolerancia numérica. La representación inspeccionable
conserva vocabulario, TF, DF, IDF y vectores en listas inmutables, para preparar
posteriormente la demostración matemática. No se devuelven por HTTP por defecto.

### Vacíos y norma cero

El corpus debe contener documentos y los tokens no pueden ser nulos ni blancos.
No se descartan silenciosamente términos inválidos. Un documento sin tokens
produce un vector cero; también puede ocurrir si solo contiene términos presentes
en todos los documentos. Si cualquiera de los vectores seleccionados tiene norma
cero, se rechaza la comparación con un mensaje explícito, incluso al compararlo
consigo mismo: no se inventa un valor para el cociente indefinido.

Un documento vacío no seleccionado permanece en el corpus y cuenta para N;
no aporta términos al vocabulario ni aumenta DF.

## Integración

`CosenoTfidfService` carga el corpus una sola vez mediante el servicio existente,
resuelve los identificadores y pasa los tokens de todos los artículos al algoritmo.
El servicio de corpus conserva su comportamiento previo de regeneración desde PDF
si el JSON no existe; esta fase no modifica ese flujo.

`GET /api/coseno-tfidf?articuloA=15&articuloB=19`

La respuesta contiene `articuloA`, `articuloB`, `porcentaje` y `resultado`:
`totalDocumentos`, `tamanoVocabulario`, `productoPunto`, `normaA`, `normaB` y
`similitud`. No contiene vocabulario, frecuencias ni vectores completos.
Parámetros inválidos, artículos inexistentes y norma cero producen HTTP 400;
errores de lectura producen HTTP 500 con un mensaje general.

La opción **Coseno TF-IDF** abre una pantalla propia que reutiliza los estilos
actuales. Los selectores cargan `/api/articulos` y el botón consulta el endpoint
real. Muestra títulos, porcentaje, similitud, N, tamaño de vocabulario, producto
punto y normas. No es una pantalla comparativa de algoritmos.

## Caso real: artículos 15 y 19

El vocabulario es global, de los 20 abstracts, y no solo de la pareja:

| Medida | Resultado aproximado |
|---|---:|
| N | 20 |
| Tamaño del vocabulario global | 1131 |
| Producto punto | 41,640274232546155 |
| Norma A (artículo 15) | 31,83952479896092 |
| Norma B (artículo 19) | 33,89094605127457 |
| Similitud | 0,03858898108939938 |
| Porcentaje | 3,858898… % ≈ 3,86 % |

Para 15 frente a 15 el coseno es aproximadamente 1 (100 %), dentro de tolerancia.
Todos los valores se calculan desde el corpus, sin excepciones por identificador.

El coseno mide cercanía entre vectores de términos ponderados: las repeticiones
aportan a TF y la presencia en muchos documentos reduce IDF. El porcentaje no
significa porcentaje de plagio ni equivalencia semántica. No se establecen umbrales.
No es directamente intercambiable con los porcentajes de Jaccard o Dice.

## Pruebas y alcance

`mvn test` verifica un ejemplo pequeño con valores matemáticos conocidos de TF,
DF, IDF, pesos, producto y normas; identidad, disjunción, simetría, límites, vacíos,
norma cero, términos inválidos y uso de documentos no seleccionados para IDF.
Las pruebas de servicio comprueban el caso real, carga única, uso exclusivo de
tokens y conservación del JSON; las HTTP comprueban respuesta resumida y errores.

No se realiza aún el conteo formal T(n) ni la demostración completa término por
término. No se añaden dependencias, modelos de IA, clustering o búsqueda de patrones.
