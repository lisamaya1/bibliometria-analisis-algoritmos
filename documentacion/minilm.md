# IA 1: MiniLM local con ONNX Runtime

## Modelo y dependencias

Modelo: **sentence-transformers/all-MiniLM-L6-v2**, inglés, embeddings de 384
dimensiones. Se ejecuta el Transformer preentrenado en CPU, durante cada comparación;
no se entrena ni se consultan APIs externas. No hay embeddings del corpus
precalculados ni cachés de resultados.

- `com.microsoft.onnxruntime:onnxruntime:1.31.1`: inferencia ONNX CPU.
- `ai.djl.huggingface:tokenizers:0.39.0`: tokenizador oficial; incluye la dependencia
  `ai.djl:api:0.39.0`, sin motores PyTorch o TensorFlow.

Se comprobó bytecode Java 8 (major 52) en los artefactos y ejecución nativa real
con Java 21.0.8, Windows x64. Las DLL del tokenizador se extraen de su JAR a la
caché local. ONNX Runtime también utiliza bibliotecas nativas, que deben corresponder
al sistema del despliegue. Windows necesita el runtime de Visual C++ requerido por
ONNX Runtime. No se añade GPU, Python ni un microservicio de inferencia.

Fuentes oficiales:

- [Modelo y pooling](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2).
- [Archivos ONNX](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/tree/main/onnx).
- [ONNX Runtime Java](https://onnxruntime.ai/docs/get-started/with-java.html).
- [Tokenización DJL](https://docs.djl.ai/master/extensions/tokenizers/index.html).

## Archivos externos y reproducibilidad

Revisión fijada: `1110a243fdf4706b3f48f1d95db1a4f5529b4d41`.

Origen de cada archivo:
`https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/1110a243fdf4706b3f48f1d95db1a4f5529b4d41/`.

Archivos preparados desde esa revisión:

```text
onnx/model.onnx
tokenizer.json
tokenizer_config.json
special_tokens_map.json
vocab.txt
config.json
sentence_bert_config.json
1_Pooling/config.json
modules.json
manifest.json                  # generado localmente: revisión, tamaños y SHA-256
```

El modelo ONNX tiene **90.405.214 bytes**. SHA-256 oficial:
`6fd5d72fe4589f189f8ebc006442dbb529bb7ce38f8082112682524616046452`.
Los nueve archivos oficiales y el manifiesto ocuparon 91.107.083 bytes en esta
preparación. La licencia declarada del modelo es Apache-2.0; se debe mantener su
atribución y licencia cuando se distribuya.

Los archivos están fuera del repositorio, en
`C:\Users\HP\modelos\all-MiniLM-L6-v2`. El código permite otra ubicación.
No se introduce el modelo en el JAR ni se realizan operaciones Git.

Desde la carpeta `backend`:

```powershell
# Solo para preparar o verificar los archivos. Requiere conexión para descargarlos.
.\scripts\preparar-minilm.ps1

# Configurar esta terminal e iniciar el backend:
$env:MINILM_MODEL_DIR = 'C:\Users\HP\modelos\all-MiniLM-L6-v2'
mvn spring-boot:run
```

`-Destino` permite elegir otra carpeta al ejecutar el script. No descarga de nuevo
archivos existentes; verifica el hash oficial del modelo y registra los hashes del
resto. Si un archivo existente está dañado, se debe sustituir por el de la revisión
indicada antes de generar de nuevo el manifiesto.

El componente verifica al iniciar los hashes registrados, revisión, dimensión y
límite de entrada. Crea **una sesión y un tokenizador compartidos** y cierra sus
recursos al apagar Spring. No vuelve a cargar archivos en cada request. Se fijaron
dos hilos de trabajo intraoperación y uno interoperación. El acceso al componente
se sincroniza para evitar compartir estado mutable del tokenizador entre solicitudes.

DJL se configura en modo offline y con telemetría desactivada antes de abrir el
tokenizador desde disco. No se usa la carga del tokenizador por nombre remoto.
Con archivos y dependencias preparados no se necesita Internet ni API key para
generar los embeddings. No se desconectó físicamente la red durante la prueba.

Sin `MINILM_MODEL_DIR`, o si la carga falla, las funciones anteriores siguen
disponibles. El endpoint MiniLM devuelve **503** con instrucciones generales; no
inventa resultados ni intenta descargar un modelo durante la solicitud.
Configurar la ruta exige reiniciar el backend; no se modifica permanentemente el
entorno de Windows desde el código.

## Flujo y matemática explícita

Se usa `ArticuloDocumental.abstractProcesado()` completo, sin modificar su contenido
ni los tokens del JSON. El tokenizador WordPiece propio del modelo se utiliza solo
para la entrada de MiniLM; sus tokens no equivalen a los tokens clásicos del corpus.
No se filtran stopwords ni números. `[UNK]`, cuando aparezca, es un token legítimo
del modelo, no un motivo para descartar contenido.

1. Tokenizar sin truncamiento, padding ni tokens especiales.
2. Dividir los IDs de contenido consecutivamente en bloques de hasta **254**.
3. Añadir `[CLS]` y `[SEP]`, leídos del vocabulario oficial, a cada bloque.
4. Ejecutar ONNX con `input_ids`, `attention_mask` y `token_type_ids` cuando el
   modelo lo requiera. El máximo es **256 posiciones**, según la configuración
   Sentence Transformers, aunque el Transformer base admita 512 posiciones.
5. Realizar mean pooling usando la máscara: para cada dimensión j,
   `p[j] = Σ mascara[i] × estado[i][j] / Σ mascara[i]`.
   Como en el procedimiento oficial, los tokens especiales con máscara 1 participan;
   el relleno con máscara 0 no participa. En la inferencia actual se procesa un
   fragmento por vez, sin relleno.
6. Normalizar cada embedding de fragmento: `u = p / sqrt(Σ p[j]²)`.
7. Promediar los embeddings normalizados con peso igual a los tokens de contenido:
   `v[j] = Σ (cantidadContenido[k] × u[k][j]) / Σ cantidadContenido[k]`.
8. Normalizar el vector combinado para obtener el embedding final.
9. Calcular producto punto, normas y coseno mediante ciclos Java:
   `cos(A,B) = Σ A[j]B[j] / (sqrt(Σ A[j]²) × sqrt(Σ B[j]²))`.

**La fragmentación y combinación ponderada son una adaptación del proyecto para
conservar todo el abstract.** La ejecución estándar de este modelo trunca entradas
demasiado largas. No se utiliza solapamiento. La representación pierde interacciones
contextuales entre fragmentos, lo que debe considerarse al interpretar resultados.

El coseno conserva la escala [-1, 1] de los vectores densos; no se fuerza a cero
un resultado negativo ni se aplica una transformación a otra escala. El porcentaje
es `100 × coseno`. Solo se acotan pequeñas desviaciones de redondeo a [-1, 1].
Textos sin tokens, máscaras sin posiciones válidas y vectores de norma cero se
rechazan explícitamente.

## API y pantalla

`GET /api/ia/minilm?articuloA=15&articuloB=19`

La respuesta incluye artículo A/B (ID y título), tokens de contenido, número de
fragmentos, bandera de fragmentación, tokens de cada fragmento, modelo, revisión,
dimensión, producto punto, normas, coseno y porcentaje. No devuelve los 384 valores
de los embeddings. Los IDs inválidos o inexistentes producen HTTP 400; el modelo
no disponible produce 503; errores de lectura del corpus producen 500.

La pantalla **MiniLM · IA 1** permite seleccionar dos artículos y comparar. Muestra
procedencia, dimensión, tokens, fragmentación y resultados. Utiliza las respuestas
reales del backend. No agrega IA 2 ni comparación final clásicos frente a IA.

## Caso real registrado: 15 vs 19

| Dato | Artículo 15 | Artículo 19 |
|---|---:|---:|
| Tokens de contenido WordPiece | 275 | 297 |
| Fragmentos | 2 | 2 |
| Tokens por fragmento, sin especiales | 254, 21 | 254, 43 |
| Dimensión final | 384 | 384 |
| Norma del embedding final | 0,9999999999999997 | 0,9999999999999996 |

Producto punto: **0,5852352272986916**.
Coseno: **0,585235227298692**.
Porcentaje: **58,52352272986921 %**, mostrado como **58,52 %**.

Para **15 vs 15**: coseno **1**, porcentaje **100 %**. La inversión de la pareja
conservó la similitud dentro de tolerancia numérica. Los últimos decimales pueden
variar ligeramente entre plataformas y versiones de ejecución; por eso se fija
la revisión y se utilizan tolerancias en las pruebas.

El resultado representa cercanía entre representaciones semánticas del modelo,
no porcentaje de plagio, veracidad o equivalencia garantizada entre artículos.
El promedio por fragmentos también puede ocultar diferencias locales del texto.
No se establecen umbrales ni se concluye que un porcentaje mayor haga mejor al método.

## Verificaciones realizadas

```powershell
# Ejecutar desde backend
$env:MINILM_MODEL_DIR = 'C:\Users\HP\modelos\all-MiniLM-L6-v2'
mvn test

cd ../frontend
npm.cmd run build
```

Con el modelo configurado aprobaron **110 pruebas, sin fallos, errores ni omitidas**.
Son 22 pruebas nuevas: pooling/máscara, normalización, coseno controlado, fragmentos,
pesos, vacíos, servicio, errores HTTP y dos pruebas con el modelo real. Un texto
de 600 tokens se procesó en 254 + 254 + 92, sin truncar la parte final. Se verificó
que el JSON del corpus no cambia. Sin variable de entorno las dos pruebas reales
se omiten; las matemáticas y de controladores usan modelos pequeños controlados.

El frontend compiló y se verificó en navegador: caso 15–19, 15–15, selección
incompleta, cambio de selección, artículo inexistente, fallo de conexión y móvil.

### Mediciones observadas, no garantías de rendimiento

En este equipo Windows x64/Java 21, un registro de las pruebas midió **3233 ms**
para cargar/verificar el componente (incluida extracción inicial de bibliotecas)
y **330 ms** para una comparación 15–19 dentro de esa ejecución de pruebas.

En un proceso Spring separado, después de las comprobaciones de navegador y
tres peticiones locales adicionales 15–19, se observaron:

- Memoria residente del proceso Java: **275,23 MiB**.
- Pico de memoria residente del mismo proceso: **276,92 MiB**.
- Memoria privada del proceso: **319,65 MiB**.
- Tiempos HTTP observados desde PowerShell: **661,18; 285,34; 256,20 ms**.

Estos datos corresponden a una observación local, incluyen el backend completo y
memoria nativa, y no aíslan el consumo del modelo. Los tiempos HTTP incluyen
cliente local, lectura del corpus, inferencia y respuesta; no son únicamente el
tiempo del Transformer ni una evaluación estadística. No incluyen Maven, Node,
VS Code o el navegador en las cifras de memoria del proceso Java.

Para desplegar se requieren modelo/configuración externos, bibliotecas nativas
compatibles, permisos de lectura y una carpeta escribible para extraer las
bibliotecas. El modelo está cargado por proceso; varias réplicas consumirían memoria
por separado. No se selecciona todavía plataforma ni se configura infraestructura.

No se realiza T(n), entrenamiento, IA 2, clustering o búsqueda de patrones.
