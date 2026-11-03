param([string]$Destino = (Join-Path $env:USERPROFILE 'modelos\all-MiniLM-L6-v2'))
$ErrorActionPreference = 'Stop'
$revision = '1110a243fdf4706b3f48f1d95db1a4f5529b4d41'
$base = "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/$revision"
$archivos = @('onnx/model.onnx', 'tokenizer.json', 'tokenizer_config.json', 'special_tokens_map.json',
    'vocab.txt', 'config.json', 'sentence_bert_config.json', '1_Pooling/config.json', 'modules.json')
$shaModelo = '6fd5d72fe4589f189f8ebc006442dbb529bb7ce38f8082112682524616046452'
$registros = @()
Add-Type -AssemblyName System.Net.Http
$cliente = New-Object System.Net.Http.HttpClient
$cliente.Timeout = [TimeSpan]::FromMinutes(15)
try {
    foreach ($archivo in $archivos) {
        $ruta = Join-Path $Destino $archivo
        $null = New-Item -ItemType Directory -Path (Split-Path $ruta) -Force
        if (-not (Test-Path -LiteralPath $ruta)) {
            Write-Host "Descargando $archivo"
            $respuesta = $cliente.GetAsync("$base/$archivo", [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead).GetAwaiter().GetResult()
            try {
                $null = $respuesta.EnsureSuccessStatusCode()
                $entrada = $respuesta.Content.ReadAsStreamAsync().GetAwaiter().GetResult()
                $salida = [System.IO.File]::Create("$ruta.partial")
                try { $entrada.CopyTo($salida) } finally { $salida.Dispose(); $entrada.Dispose() }
                Move-Item -LiteralPath "$ruta.partial" -Destination $ruta
            } finally { $respuesta.Dispose() }
        }
        $hash = (Get-FileHash -LiteralPath $ruta -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($archivo -eq 'onnx/model.onnx' -and $hash -ne $shaModelo) {
            throw 'El SHA-256 del modelo no coincide con el archivo oficial. No se generara el manifiesto.'
        }
        $registros += [ordered]@{ archivo = $archivo; sha256 = $hash; bytes = (Get-Item -LiteralPath $ruta).Length }
    }
    $manifiesto = [ordered]@{ modelo = 'sentence-transformers/all-MiniLM-L6-v2'; revision = $revision; archivos = $registros }
    $json = $manifiesto | ConvertTo-Json -Depth 5
    [System.IO.File]::WriteAllText((Join-Path $Destino 'manifest.json'), $json, (New-Object System.Text.UTF8Encoding($false)))
    Write-Host "Modelo verificado en $Destino"
    Write-Host 'Configura MINILM_MODEL_DIR con esa ruta antes de iniciar el backend.'
} finally { $cliente.Dispose() }
