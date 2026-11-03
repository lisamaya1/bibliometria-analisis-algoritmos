# Proyecto de Análisis de Algoritmos

Autora: Elizabeth Maya.

## Organización

```text
backend/        Spring Boot, pruebas, datos del corpus y scripts
frontend/       React y Vite
documentacion/  Documentación técnica de los algoritmos
```

El backend y el frontend se compilan y ejecutan por separado. Se requieren Java 21, Maven y Node.js con npm.

## Backend

Desde la raíz del repositorio, en PowerShell:

```powershell
cd backend
$env:MINILM_MODEL_DIR = 'C:\Users\HP\modelos\all-MiniLM-L6-v2'
$env:BGE_MODEL_DIR = 'C:\Users\HP\modelos\bge-small-en-v1.5'
mvn spring-boot:run
```

Las rutas de los modelos son externas y deben ajustarse a cada equipo. Su preparación se explica en `documentacion/minilm.md` y `documentacion/bge.md`. El servidor escucha en `http://localhost:8080`.

Ejecutar Maven desde `backend` conserva las rutas relativas del corpus: `datos/procesados/corpus_procesado.json` y `datos/pdfs_originales`. Los scripts están en `backend/scripts`.

## Frontend

En otra terminal, desde la raíz del repositorio:

```powershell
cd frontend
npm install
npm run dev
```

Abrir la dirección que indique Vite, normalmente `http://localhost:5173`. Durante el desarrollo, Vite reenvía `/api` al backend en el puerto 8080.

## Verificación y compilación

En `backend`, con los modelos configurados: `mvn test` y `mvn package`.

En `frontend`: `npm run build`. Los archivos estáticos se generan en `frontend/dist`.

## Despliegue en Render

La publicación usa **un único Web Service Docker**. Vite compila el frontend y
Docker copia `frontend/dist` a `src/main/resources/static` durante la construcción
del backend. Spring Boot entrega tanto `/` y `/assets/*` como `/api/*` desde el
mismo origen. No se necesita otro servicio, una base de datos, Redis ni CORS nuevo.
La navegación usa estado de React: no requiere React Router ni un fallback global
que pueda ocultar errores de la API.

Este proceso no cambia el desarrollo local descrito arriba. No copia archivos
generados al código fuente del equipo: la integración ocurre dentro del build
Docker. En producción solo se ejecuta Java; no se utilizan Maven ni el servidor
de desarrollo de Vite.

### Construir y probar la imagen

Se necesita Docker Desktop iniciado en modo contenedores Linux. Desde la raíz:

```powershell
docker build -t bibliometria-render .
docker run --rm --name bibliometria-render-prueba --memory=2g -p 8080:8080 bibliometria-render
```

La primera construcción descarga imágenes, dependencias y aproximadamente 224 MB
decimales de pesos ONNX, además de sus archivos auxiliares. Requiere Internet;
las comparaciones posteriores generan embeddings localmente, sin API externa
ni API key. Las capas de construcción permiten reutilizar descargas.

Para comprobar un puerto distinto, ejecutar como alternativa:

```powershell
docker run --rm --name bibliometria-render-prueba --memory=2g -e PORT=10000 -p 18080:10000 bibliometria-render
```

En ese caso abrir `http://localhost:18080`. El puerto de la izquierda es el del
equipo y el de la derecha es el que recibe Spring dentro del contenedor.
`EXPOSE 8080` es informativo; `server.port=${PORT:8080}` selecciona el puerto.

Desde otra terminal, para el primer comando de ejecución:

```powershell
$base = 'http://localhost:8080'
(Invoke-WebRequest "$base/").StatusCode
(Invoke-WebRequest "$base/index.html").StatusCode
Invoke-RestMethod "$base/api/health"
Invoke-RestMethod "$base/api/articulos"
Invoke-RestMethod "$base/api/jaccard?articuloA=15&articuloB=19"
Invoke-RestMethod "$base/api/ia/minilm?articuloA=15&articuloB=19"
Invoke-RestMethod "$base/api/ia/bge?articuloA=15&articuloB=19"
docker exec bibliometria-render-prueba id
docker exec bibliometria-render-prueba sh -c 'test -r /app/datos/procesados/corpus_procesado.json && test -d /app/datos/pdfs_originales'
docker logs bibliometria-render-prueba
docker stop --timeout 30 bibliometria-render-prueba
```

Con el segundo comando cambiar `$base` a `http://localhost:18080`. Los assets que
referencia el HTML deben responder 200; una ruta inexistente de `/api/*` debe
responder un error, sin devolver el HTML de React. `id` debe mostrar UID 10001,
no root. El health check responde `{"status":"ok"}` sin leer datos ni ejecutar IA.
El arranque usa `java -jar` directamente para recibir las señales de apagado.

### Datos, modelos y licencias

El directorio de trabajo es `/app`. La imagen conserva:

```text
/app/app.jar
/app/datos/procesados/corpus_procesado.json
/app/datos/pdfs_originales/                 # los 20 PDF
/opt/models/minilm/                        # MiniLM y manifest.json
/opt/models/bge/                           # BGE y manifest.json
/opt/models/ATTRIBUTIONS.md
```

La etapa de modelos ejecuta `deployment/prepare-model.sh`, un script POSIX sin
PowerShell ni Python. Descarga nueve archivos funcionales y la model card de cada
modelo desde revisiones inmutables, verifica **todos** contra los SHA-256 fijados
en `deployment/models/*.sha256` y copia un manifiesto compatible con los cargadores
actuales. Un hash distinto detiene el build. No se aceptan hashes nuevos calculados
sin contrastarlos. Los archivos ONNX no se guardan en Git ni en el JAR; se copian
una sola vez a la imagen final desde la etapa de modelos.

| Modelo | Revisión | Licencia |
| --- | --- | --- |
| `sentence-transformers/all-MiniLM-L6-v2` | `1110a243fdf4706b3f48f1d95db1a4f5529b4d41` | Apache-2.0 |
| `BAAI/bge-small-en-v1.5` | `5c38ec7c405ec4b44b94cc5a9bb96e735b38267a` | MIT |

Cada modelo conserva su `README.md` oficial y `LICENSE`. Las atribuciones y el
origen de las licencias están en `deployment/ATTRIBUTIONS.md`. No se cambian
dimensiones, tokenización, pooling, límites, fragmentación ni fórmulas.

Los archivos de la aplicación, corpus y modelos son legibles por el usuario sin
privilegios. Su carpeta `/home/app` y `/tmp` permiten extraer bibliotecas nativas.
Los JAR actuales incluyen bibliotecas Linux del tokenizador; no se cambian las
dependencias ONNX Runtime y DJL.

### Variables de entorno

| Variable | Comportamiento |
| --- | --- |
| `PORT` | Render la proporciona. Sin ella Spring usa 8080. No fijarla en el Blueprint. |
| `MINILM_MODEL_DIR` | En Docker: `/opt/models/minilm`. En desarrollo se conserva la ruta externa del equipo. |
| `BGE_MODEL_DIR` | En Docker: `/opt/models/bge`. En desarrollo se conserva la ruta externa del equipo. |
| `JAVA_TOOL_OPTIONS` | Por defecto: `-XX:InitialRAMPercentage=10.0 -XX:MaxRAMPercentage=40.0 -XX:+ExitOnOutOfMemoryError`. Se puede reemplazar desde Render o con `docker run -e`. |

El heap máximo se limita al 40 % de la memoria que detecta la JVM para dejar
espacio a ONNX, el tokenizador, metaspace y otras asignaciones nativas. Esto no
limita toda la memoria del proceso ni garantiza estabilidad bajo cualquier carga.
La imagen final usa JRE 21 y UID/GID 10001; Node y Maven quedan en las etapas de build.

Sin las variables de modelos, el backend local arranca y los endpoints de IA
responden **503**. En Docker las variables están definidas por defecto. Si aparece
503, revisar en los logs la ruta, los hashes, permisos y carga de bibliotecas;
el health check puede seguir devolviendo 200 porque solo verifica Spring, no IA.
No se descarga un modelo durante una petición ni se devuelve una similitud ficticia.

### Memoria y plan

`render.yaml` propone **`free`**, una instancia gratuita de 512 MiB y CPU compartida.
Es una configuración orientada a demostraciones y tráfico bajo; estos archivos no
crean ni contratan el servicio por sí solos. La imagen se verificó localmente con
los límites de 512 MiB y 0.1 CPU: inició ambos modelos y respondió correctamente,
pero el consumo llegó a aproximadamente 503 MiB bajo cuatro análisis simultáneos.
Por ese margen reducido, conviene evitar solicitudes de IA concurrentes. El primer
acceso también puede tardar mientras Render reactiva el servicio gratuito.

El plan se puede cambiar en **Compute → Edit** de Render sin cambiar código Java.
Si se gestiona mediante Blueprint, mantener `plan` sincronizado en `render.yaml`
para que una sincronización posterior no restaure otra configuración.
Consulta [planes y memoria](https://render.com/docs/compute-plans) y
[precios vigentes](https://render.com/pricing).

### Crear el servicio mediante Blueprint

1. Cuando autorices la publicación de estos cambios, subirlos a la rama `main`
   de `lisamaya1/bibliometria-analisis-algoritmos`. No desplegar un commit anterior
   que todavía no tenga `Dockerfile` y `render.yaml`.
2. En Render, elegir **New → Blueprint** y conectar ese repositorio.
3. Utilizar `render.yaml` desde la raíz. Revisar que se proponga un único Web
   Service Docker, contexto `.`, Dockerfile `./Dockerfile`, una instancia y el plan
   `free`. No establecer Root Directory en `backend` ni en `frontend`.
4. Confirmar la creación del servicio gratuito. No añadir bases
   de datos, Static Site separado ni disco persistente.
5. Revisar los logs de build y los de ejecución. Deben indicar Spring iniciado
   en el puerto recibido y ambos modelos cargados con sus revisiones fijadas.
6. Abrir la URL HTTPS asignada por Render y repetir las verificaciones HTTP
   anteriores cambiando `$base` por esa URL, sin agregar el puerto interno.
7. Comprobar en Settings que el health check sea `/api/health`.

Spring sirve todos los endpoints existentes. La misma URL entrega el frontend;
no se introducen URLs absolutas de Render en React ni se cambia el proxy local.
Véanse [Docker en Render](https://render.com/docs/docker),
[Blueprints](https://render.com/docs/blueprint-spec) y
[puertos](https://render.com/docs/web-services#port-binding).

El filesystem de Render es efímero: los cambios realizados en ejecución no son
persistentes tras un reemplazo de instancia. Este proyecto lee el corpus y los
modelos incluidos en la imagen, por lo que no necesita disco persistente. No usar
la instancia como lugar para editar o almacenar permanentemente un corpus nuevo.

### Verificación sin Docker

Los comandos locales siguen siendo independientes:

```powershell
cd frontend
npm ci
npm run build
cd ../backend
mvn test
mvn package
```

Sin las dos variables de modelos, la suite existente descubre 251 pruebas y omite
las cuatro pruebas reales de IA por sus condiciones actuales: no es un fallo ni
se añade `skipTests`. Para ejecutarlas también, configurar ambas rutas como en
las instrucciones locales. El Dockerfile ejecuta `mvn package` con las pruebas
activas y la misma omisión condicional; las inferencias Linux deben verificarse
después sobre la imagen final con los modelos disponibles.

### Resultado de la verificación local de Docker

El 2 de noviembre de 2026 se construyó y ejecutó la imagen en Linux x86_64:

- `npm ci` y `npm run build`: correctos en el equipo y en Docker.
- `mvn test` y `mvn package`: correctos, 251 detectadas, 247 aprobadas,
  4 omitidas por ausencia de variables de modelos, 0 fallos y 0 errores.
  El empaquetado Docker reprodujo ese resultado.
- 32 rutas/casos HTTP verificados con modelos y 30 sin modelos: HTML, assets,
  artículos, algoritmos, errores y health check. Sin modelos, IA respondió 503.
- Puerto configurable probado con `PORT=10000`; sin `PORT`, se usó 8080.
- UID 10001, los 20 PDF disponibles y apagado graceful confirmado en los logs.
- MiniLM 15–19: `0.585235227298692`; BGE 15–19: `0.5744566243014836`.
  En 15–15 se obtuvo respectivamente 1 y 0. Jaccard conservó `33/241`.
- Docker informó **566,8 MiB** tras las peticiones, con límite de 2 GiB y una CPU.
  Es una observación puntual, no un pico medido ni garantía de capacidad.
- `render.yaml` validado contra el JSON Schema oficial de Render.

Quedan pendientes la publicación autorizada de estos archivos y la comprobación
en una instancia real de Render. No se ha creado ni contratado un servicio.
