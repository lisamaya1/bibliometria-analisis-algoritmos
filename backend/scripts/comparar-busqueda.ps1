param(
    [int]$Articulo = 19,
    [ValidateNotNullOrEmpty()][string]$Patron = 'chatgpt',
    [ValidateRange(1, 1000000)][int]$Calentamiento = 2000,
    [ValidateRange(1, 1000000)][int]$Repeticiones = 5000
)
$ErrorActionPreference = 'Stop'
$raizProyecto = Split-Path -Parent $PSScriptRoot
$carpetaTemporal = Join-Path ([System.IO.Path]::GetTempPath()) ('bibliometria-busqueda-' + [guid]::NewGuid())
$utf8 = New-Object System.Text.UTF8Encoding($false)
$anteriorConsola = [Console]::OutputEncoding
$anteriorSalida = $OutputEncoding
Push-Location $raizProyecto
try {
    [Console]::OutputEncoding = $utf8
    $OutputEncoding = $utf8
    $null = New-Item -ItemType Directory -Path $carpetaTemporal
    $rutaClasspath = Join-Path $carpetaTemporal 'classpath.txt'
    # Compilacion y preparacion del classpath ocurren antes de cualquier cronometro.
    & mvn -q -DskipTests compile dependency:build-classpath "-Dmdep.outputFile=$rutaClasspath"
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo preparar el ejecutable local.' }
    $dependencias = [System.IO.File]::ReadAllText($rutaClasspath).Trim()
    $clases = Join-Path $raizProyecto 'target/classes'
    $classpath = $clases + [System.IO.Path]::PathSeparator + $dependencias
    & java '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' -cp $classpath com.uniquindio.bibliometria.consola.ConsolaComparacionBusqueda "$Articulo" $Patron "$Calentamiento" "$Repeticiones"
    if ($LASTEXITCODE -ne 0) { throw 'Fallo la comparacion local de busqueda.' }
} finally {
    [Console]::OutputEncoding = $anteriorConsola
    $OutputEncoding = $anteriorSalida
    Pop-Location
    if (Test-Path -LiteralPath $carpetaTemporal) {
        $verificada = [System.IO.Path]::GetFullPath($carpetaTemporal)
        $raizTemporal = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath()).TrimEnd('\')
        if ([System.IO.Path]::GetDirectoryName($verificada) -eq $raizTemporal -and
                [System.IO.Path]::GetFileName($verificada).StartsWith('bibliometria-busqueda-')) {
            Remove-Item -LiteralPath $verificada -Recurse -Force
        }
    }
}
