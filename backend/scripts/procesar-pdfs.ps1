param(
    [ValidateSet('Inspeccionar', 'Exportar')][string]$Modo = 'Inspeccionar',
    [string]$Origen = (Join-Path (Split-Path -Parent $PSScriptRoot) 'datos/pdfs_originales'),
    [string]$Salida = (Join-Path (Split-Path -Parent $PSScriptRoot) 'datos/procesados/corpus_procesado.json'),
    [int]$Articulo = 0,
    [switch]$ConservarTexto,
    [switch]$NormalizarEspacios,
    [switch]$Minusculas,
    [switch]$NormalizarUnicode,
    [switch]$UnirGuiones,
    [switch]$ReconstruirEspacios,
    [switch]$OcultarAvisosFuentes
)

$ErrorActionPreference = 'Stop'
$raizProyecto = Split-Path -Parent $PSScriptRoot
$carpetaTemporal = Join-Path ([System.IO.Path]::GetTempPath()) ('bibliometria-pdf-' + [guid]::NewGuid())
$utf8 = New-Object System.Text.UTF8Encoding($false)
$codificacionConsolaAnterior = [Console]::OutputEncoding
$codificacionSalidaAnterior = $OutputEncoding
$opcionesJava = @('-Dfile.encoding=UTF-8', '-Dstdout.encoding=UTF-8', '-Dstderr.encoding=UTF-8')
try {
    [Console]::OutputEncoding = $utf8
    $OutputEncoding = $utf8
    foreach ($herramienta in @('java', 'javac', 'mvn')) {
        if (-not (Get-Command $herramienta -ErrorAction SilentlyContinue)) {
            throw "Falta una herramienta en PATH: $herramienta"
        }
    }
    if (-not $Origen) { throw 'Indique -Origen con un ZIP o una carpeta de PDF' }
    New-Item -ItemType Directory -Path $carpetaTemporal | Out-Null
    if ($OcultarAvisosFuentes) {
        # Oculta solo los avisos de fuentes sin correspondencia Unicode.
        $rutaRegistro = Join-Path $carpetaTemporal 'logging.properties'
        $configuracionRegistro = @'
handlers=java.util.logging.ConsoleHandler
.level=INFO
java.util.logging.ConsoleHandler.level=INFO
java.util.logging.ConsoleHandler.encoding=UTF-8
java.util.logging.ConsoleHandler.filter=com.uniquindio.bibliometria.ingesta.FiltroAvisosFuentes
'@
        [System.IO.File]::WriteAllText($rutaRegistro, $configuracionRegistro, $utf8)
        $opcionesJava += "-Djava.util.logging.config.file=$rutaRegistro"
    }
    # Compila este modulo en TEMP sin cambiar el pom.xml del equipo.
    $dependencias = @'
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>local.validation</groupId><artifactId>pdf-processing</artifactId><version>1</version>
  <dependencies>
    <dependency><groupId>org.apache.pdfbox</groupId><artifactId>pdfbox</artifactId><version>3.0.4</version></dependency>
    <dependency><groupId>tools.jackson.core</groupId><artifactId>jackson-databind</artifactId><version>3.1.5</version></dependency>
  </dependencies>
</project>
'@
    $rutaPomTemporal = Join-Path $carpetaTemporal 'pom.xml'
    [System.IO.File]::WriteAllText($rutaPomTemporal, $dependencias, $utf8)
    & mvn -q -f $rutaPomTemporal dependency:copy-dependencies "-DoutputDirectory=$carpetaTemporal\lib"
    if ($LASTEXITCODE -ne 0) { throw 'No se pudieron cargar las dependencias' }
    $raizPaquetes = Join-Path $raizProyecto 'src/main/java/com/uniquindio/bibliometria'
    $carpetasCodigo = @('ingesta', 'preprocesamiento')
    $archivosCodigo = @($carpetasCodigo | ForEach-Object {
        Get-ChildItem -LiteralPath (Join-Path $raizPaquetes $_) -Filter *.java |
            Where-Object { $_.Name -ne 'test.java' } | ForEach-Object FullName
    })
    $carpetaClases = Join-Path $carpetaTemporal 'classes'
    & javac -encoding UTF-8 -cp "$carpetaTemporal\lib\*" -d $carpetaClases @archivosCodigo
    if ($LASTEXITCODE -ne 0) { throw 'Fallo la compilacion' }
    $rutaClases = "$carpetaClases;$carpetaTemporal\lib\*"
    $argumentos = @($Modo.ToLowerInvariant(), $Origen)
    if ($Modo -eq 'Exportar') {
        if ($Articulo -gt 0) { throw 'La exportacion guarda todos los articulos; no use -Articulo' }
        $argumentos += $Salida
    }
    if ($Modo -eq 'Inspeccionar' -and $Articulo -gt 0) { $argumentos += "$Articulo" }
    if ($Modo -eq 'Exportar' -or ($Modo -eq 'Inspeccionar' -and $Articulo -gt 0)) {
        if ($ConservarTexto) { $argumentos += '--conservar-texto' }
        if ($NormalizarEspacios) { $argumentos += '--espacios' }
        if ($Minusculas) { $argumentos += '--minusculas' }
        if ($NormalizarUnicode) { $argumentos += '--unicode' }
        if ($UnirGuiones) { $argumentos += '--unir-guiones' }
        if ($ReconstruirEspacios) { $argumentos += '--reconstruir-espacios' }
    }
    & java @opcionesJava -cp $rutaClases com.uniquindio.bibliometria.ingesta.ConsolaPdf @argumentos
    if ($LASTEXITCODE -ne 0) { throw 'Fallo la ejecucion' }
} finally {
    [Console]::OutputEncoding = $codificacionConsolaAnterior
    $OutputEncoding = $codificacionSalidaAnterior
    # Verifica que la carpeta sea temporal antes de borrarla.
    if (Test-Path -LiteralPath $carpetaTemporal) {
        $temporalVerificado = [System.IO.Path]::GetFullPath($carpetaTemporal)
        $raizTemporal = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath()).TrimEnd('\')
        if ([System.IO.Path]::GetDirectoryName($temporalVerificado) -eq $raizTemporal -and
            [System.IO.Path]::GetFileName($temporalVerificado).StartsWith('bibliometria-pdf-')) {
            Remove-Item -LiteralPath $temporalVerificado -Recurse -Force
        }
    }
}
