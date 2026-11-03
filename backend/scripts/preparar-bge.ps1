param([string]$Destino = (Join-Path $env:USERPROFILE 'modelos\bge-small-en-v1.5'))
$ErrorActionPreference = 'Stop'
$revision = '5c38ec7c405ec4b44b94cc5a9bb96e735b38267a'
$base = "https://huggingface.co/BAAI/bge-small-en-v1.5/resolve/$revision"
$archivos = @('onnx/model.onnx', 'tokenizer.json', 'tokenizer_config.json', 'special_tokens_map.json',
    'vocab.txt', 'config.json', 'sentence_bert_config.json', '1_Pooling/config.json', 'modules.json')
$shaModelo = '828e1496d7fabb79cfa4dcd84fa38625c0d3d21da474a00f08db0f559940cf35'
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
    $manifiesto = [ordered]@{ modelo = 'BAAI/bge-small-en-v1.5'; revision = $revision; origen = $base; archivos = $registros }
    $json = $manifiesto | ConvertTo-Json -Depth 5
    [System.IO.File]::WriteAllText((Join-Path $Destino 'manifest.json'), $json, (New-Object System.Text.UTF8Encoding($false)))
    Write-Host "Modelo verificado en $Destino"
    Write-Host 'Configura BGE_MODEL_DIR con esa ruta antes de iniciar el backend.'
} finally { $cliente.Dispose() }
