# Lectura y procesamiento de los PDF

Este módulo lee los artículos y guarda sus datos en `datos/procesados/corpus_procesado.json`. No utiliza una base de datos ni incluye algoritmos de similitud.

## Cómo funciona

1. `LectorPdfBox` obtiene el texto y la posición de las líneas del PDF.
2. `ExtractorArticulos` identifica título, autores y resumen.
3. `ProcesadorResumen` limpia una copia del resumen y la separa en palabras.
4. `ArchivoArticulosJson` guarda los resultados para que el equipo pueda utilizarlos sin leer otra vez los PDF.

Los archivos están en `src/main/java/com/uniquindio/bibliometria/ingesta/` y `preprocesamiento/`. Los nombres de las bibliotecas Java y PDFBox conservan su idioma porque forman parte de sus interfaces.

## Comandos

Se necesita JDK 21 y Maven en PATH. Ejecutar desde la carpeta `backend`:

```powershell
# Generar el JSON con los PDF de datos/pdfs_originales
.\scripts\procesar-pdfs.ps1 -Modo Exportar -OcultarAvisosFuentes

# Revisar un artículo: cambiar el número para elegir otro
.\scripts\procesar-pdfs.ps1 -Modo Inspeccionar -Articulo 2 -OcultarAvisosFuentes

```

Por defecto se lee `datos/pdfs_originales/` y se guarda `datos/procesados/corpus_procesado.json`. Se puede indicar otro ZIP o carpeta con `-Origen` y otro archivo JSON con `-Salida`. Los artículos se ordenan por el número inicial del nombre del PDF.

El script compila únicamente este módulo en una carpeta temporal y la elimina al terminar. No cambia el `pom.xml` del equipo ni copia los PDF. Exportar reemplaza el JSON de salida; inspeccionar no lo modifica. Si un artículo tiene errores de lectura o campos obligatorios faltantes, la exportación se detiene antes de escribir el archivo.

## Contenido del JSON

Es una lista con un objeto por artículo. Cada objeto contiene:

| Campo | Contenido |
|---|---|
| `titulo` | Título extraído. |
| `autores` | Lista de autores. |
| `resumenOriginal` | Resumen extraído, sin aplicar limpieza. |
| `resumenProcesado` | Copia del resumen después de la limpieza. |
| `palabras` | Lista de palabras, incluyendo repeticiones. |

El JSON contiene únicamente estos cinco campos por artículo. Se usa UTF-8 para conservar acentos y otros caracteres. El título, los autores y el contenido de los artículos mantienen su idioma original.

Para leer los datos desde otro módulo Java:

```java
var archivoJson = new ArchivoArticulosJson();
var articulos = archivoJson.leer(Path.of("datos/procesados/corpus_procesado.json"));
```

## Limpieza y revisión

Los títulos y autores se normalizan con reglas generales: espacios habituales, guiones y comillas normales, ligaduras como `ﬁ` convertidas en `fi` y acentos conservados. No se hacen correcciones por artículo.

La limpieza predeterminada une palabras partidas por guion y salto de línea, reconstruye espacios usando las posiciones del PDF, aplica esa misma normalización de caracteres y convierte a minúsculas. Las rayas que separan frases se convierten en guiones con espacios para no pegar palabras. Se conservan números, siglas, acentos y símbolos técnicos. No se eliminan palabras comunes ni se aplica stemming o lematización. El resumen original permanece intacto como referencia de lo que contenía el PDF.

Las reglas están separadas en `OpcionesPreprocesamiento`. Las opciones del script son `-ConservarTexto`, `-NormalizarEspacios`, `-Minusculas`, `-NormalizarUnicode`, `-UnirGuiones` y `-ReconstruirEspacios`. La normalización de caracteres está activada por defecto. `-ConservarTexto` desactiva la limpieza del resumen; las demás opciones indicadas pueden activarse después.

Lisa confirmó la revisión manual de los 20 PDF. Se verificaron los títulos, autores, límites de los resúmenes, conservación del original, separación de palabras y lectura/escritura del JSON. Los datos se generan leyendo los PDF.

Las reglas de extracción se validaron con estos 20 documentos y usan la posición del texto en la primera página. Para nuevos documentos hay que revisar los resultados. Los PDF escaneados requieren OCR, que este módulo no implementa. Los avisos de fuentes sin correspondencia Unicode se pueden ocultar con `-OcultarAvisosFuentes`; ocultarlos no recupera los caracteres.

La configuración Maven del equipo ya incluye PDFBox y Jackson. Falta coordinar cómo los demás módulos consumirán el JSON; no se han modificado controladores, servicios ni archivos de otros integrantes.
