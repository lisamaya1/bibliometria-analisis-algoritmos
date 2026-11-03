# BGE · IA 2: embeddings y distancia euclidiana

Se compara el abstract procesado vigente de cada artículo. No se modifica el corpus, el preprocesamiento ni sus tokens clásicos. El tokenizador oficial BGE genera sus propios tokens de entrada al Transformer. No se usan prefijos de consulta porque se comparan dos documentos de forma simétrica.

## Modelo y preparación

- Modelo: `BAAI/bge-small-en-v1.5`, inglés, licencia MIT.
- Revisión fija: `5c38ec7c405ec4b44b94cc5a9bb96e735b38267a`.
- Origen: https://huggingface.co/BAAI/bge-small-en-v1.5/tree/5c38ec7c405ec4b44b94cc5a9bb96e735b38267a
- ONNX FP32 oficial: `onnx/model.onnx`, 133.093.490 bytes.
- SHA-256 ONNX: `828e1496d7fabb79cfa4dcd84fa38625c0d3d21da474a00f08db0f559940cf35`.
- Dimensión: 384; límite: 512 tokens incluyendo `[CLS]` y `[SEP]`.
- Dependencias existentes: ONNX Runtime Java `1.31.1` y DJL Hugging Face Tokenizers `0.39.0`. No se añadieron dependencias.

Desde la carpeta `backend`:

```powershell
.\scripts\preparar-bge.ps1
$env:BGE_MODEL_DIR = 'C:\Users\HP\modelos\bge-small-en-v1.5'
# Opcional para habilitar también IA 1 en esta terminal:
$env:MINILM_MODEL_DIR = 'C:\Users\HP\modelos\all-MiniLM-L6-v2'
mvn spring-boot:run
```

El script descarga fuera del repositorio: `onnx/model.onnx`, `tokenizer.json`, `config.json`, `sentence_bert_config.json`, `tokenizer_config.json`, `special_tokens_map.json`, `vocab.txt`, `1_Pooling/config.json` y `modules.json`. Genera `manifest.json` con origen, revisión, tamaños y hashes SHA-256 de los nueve archivos. Verifica además el hash oficial del ONNX. El backend comprueba el manifiesto, los hashes, dimensión, límite y configuración CLS antes de cargar.

Modelo y tokenizador se cargan una vez por instancia del backend, no en cada solicitud. No hay descarga durante las solicitudes, API externa ni API key. Tras preparar modelo y dependencias, la inferencia es local y no requiere Internet. Si no se configura el modelo, su endpoint responde 503 con un mensaje, y los demás módulos siguen disponibles.

En despliegue debe proporcionarse la carpeta externa y `BGE_MODEL_DIR`, con bibliotecas nativas compatibles con el sistema del servidor. No se incluyen pesos dentro del repositorio ni del código fuente.

## Generación del embedding

1. Tokenizar el abstract procesado sin truncamiento ni tokens especiales automáticos.
2. Dividir en fragmentos consecutivos de hasta 510 tokens de contenido, sin solapamiento.
3. Añadir `[CLS]` y `[SEP]`, máscara de atención y tipos de token para ejecutar ONNX.
4. Extraer el primer estado de `last_hidden_state`, correspondiente a `[CLS]`. No se usa mean pooling ni `pooler_output`.
5. Normalizar cada embedding con norma L2.
6. Promediar los embeddings de fragmentos ponderando por su cantidad de tokens de contenido.
7. Normalizar el vector final con L2.

La fragmentación y combinación son una **adaptación del proyecto para conservar todo el abstract**. No equivalen exactamente a procesar el documento completo en una única entrada: no hay atención entre fragmentos. Para un solo fragmento se conserva su embedding normalizado.

Se reutilizan `MatematicaEmbeddings` (normas, normalización y promedio ponderado) y `FragmentadorTokens`. MiniLM delega únicamente la partición en esta utilidad, conservando su límite y mean pooling. BGE tiene interfaz, cargador y generador propios, sin dependencias de clases específicas de MiniLM.

## Distancia e interpretación

El cálculo Java implementa explícitamente las restas, cuadrados, suma y raíz:

`d(A,B) = sqrt(sum((Ai - Bi)^2))`

La respuesta incluye suma de cuadrados, normas A/B y distancia; no devuelve los vectores completos ni un porcentaje.

**Una distancia menor indica mayor cercanía entre los embeddings. Una distancia de 0 representa vectores iguales.** No es una probabilidad ni un nivel de plagio.

Para vectores de norma 1: `d² = 2 - 2cos(A,B)`, por lo que la distancia queda entre 0 y 2, salvo redondeo numérico. En estos mismos vectores normalizados, ordenar por distancia ascendente equivale a ordenar por coseno descendente. No se transforma la distancia en porcentaje ni se establecen umbrales universales.

## Endpoint e interfaz

`GET /api/ia/bge?articuloA=15&articuloB=19`

La respuesta identifica ambos artículos, modelo, revisión, dimensión, tokens de contenido, cantidad de fragmentos, distribución por fragmento y resultado matemático. IDs inválidos/inexistentes producen 400, modelo no disponible 503 y errores de lectura del corpus 500.

En React, la navegación **BGE · IA 2** abre su pantalla propia. Usa el endpoint real, permite identidad, bloquea selecciones incompletas, limpia resultados al cambiar selección y muestra errores del servidor. Reutiliza los estilos de las pantallas anteriores. La navegación existente usa estado de React, sin una URL propia por pantalla.

## Caso académico: 15 vs 19

Medición real en CPU, Java 21, Windows, el 29/10/2026:

| Dato | Artículo 15 | Artículo 19 |
|---|---:|---:|
| Tokens de contenido BGE | 275 | 297 |
| Fragmentos | 1 | 1 |
| Distribución | [275] | [297] |
| Dimensión | 384 | 384 |
| Norma final | 1,0 | 1,0 |

- Suma de cuadrados: `0.3300004132038559`.
- Distancia euclidiana: `0.5744566243014836`.
- En pantalla: aproximadamente `0,574457`, sin porcentaje.
- 15 vs 15: suma de cuadrados y distancia `0.0`.
- Simetría verificada con 19 vs 15.

## Pruebas y rendimiento observado

```powershell
# Ejecutar desde backend
$env:BGE_MODEL_DIR = 'C:\Users\HP\modelos\bge-small-en-v1.5'
$env:MINILM_MODEL_DIR = 'C:\Users\HP\modelos\all-MiniLM-L6-v2'
mvn test
cd ../frontend
npm run build
```

Se añadieron 18 pruebas BGE: 5 matemáticas, 4 de generación/fragmentación, 4 de servicio, 3 de controlador y 2 con modelo real. Cubren distancia conocida 3-4-5, identidad, simetría, relación con coseno, normalización, CLS, pesos, conservación y orden de tokens, límites, texto corto/largo, artículos inexistentes, abstract procesado, errores HTTP y conservación del JSON.

Suite completa: **128 aprobadas, 0 fallos, 0 errores y 0 omitidas** con las dos rutas configuradas. Sin las variables, las pruebas reales correspondientes se omiten explícitamente; las matemáticas y de integración controlada no requieren descargar modelos.

Compilación React correcta. Verificación con navegador y API real: 15 vs 19, 15 vs 15, selección incompleta, cambio de selección, error real 400, error de conexión y pantalla móvil sin desbordamiento horizontal.

Observaciones de una ejecución local (no son garantías de rendimiento):

- Carga y verificación BGE al arrancar el backend: **5.415 ms**.
- Primera petición HTTP BGE 15 vs 19: **2.436,09 ms**.
- Dos peticiones posteriores: **675,59 ms** y **673,77 ms**.
- El cronómetro HTTP incluye cliente, lectura del corpus, inferencias, transporte y serialización; no mide únicamente el Transformer. La carga de modelos ocurre antes de servir solicitudes.
- Después de tres peticiones 15 vs 19 y una 15 vs 15: **422,92 MiB RSS**, pico **434,29 MiB**, memoria privada **467,01 MiB** del proceso Java completo con BGE y MiniLM cargados. No es memoria exclusiva de BGE ni incluye navegador, Maven o Vite.

Durante la adaptación se corrigió una constante de identificación heredada de MiniLM, detectada por la validación del manifiesto. La ejecución final funciona con las dependencias existentes; no apareció incompatibilidad que requiriera otra librería.

No se realizó análisis formal T(n), comparación conjunta de seis métodos ni cambios al corpus o al preprocesamiento.
