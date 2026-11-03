function MetricasMinimal({
  tiempoMs = 0,
  totalElementos = 0,
  numeroCubetas = 0,
  rangoK = 0,
  frecuenciaMinima = 1,
  frecuenciaMaxima = 1,
  totalArticulos = 20,
  algoritmo = 'Bucket Sort'
}) {
  const esCounting = algoritmo.toLowerCase().includes('counting')
  const esRadix = algoritmo.toLowerCase().includes('radix')
  const valorK = rangoK || (frecuenciaMaxima - frecuenciaMinima + 1)

  // Determinar la tercera métrica según el algoritmo
  let terceraMetrica
  if (esCounting) {
    terceraMetrica = {
      id: 'k',
      etiqueta: 'Rango de claves',
      simbolo: 'k',
      valor: valorK.toLocaleString(),
      subtitulo: `k = f_máx - f_mín + 1 (${valorK})`,
      icono: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <line x1="18" y1="20" x2="18" y2="10" />
          <line x1="12" y1="20" x2="12" y2="4" />
          <line x1="6" y1="20" x2="6" y2="14" />
        </svg>
      )
    }
  } else if (esRadix) {
    terceraMetrica = {
      id: 'd',
      etiqueta: 'Dígitos procesados',
      simbolo: 'd',
      valor: numeroCubetas,
      subtitulo: `d = ⌊log₁₀(f_máx)⌋ + 1 (${numeroCubetas} pasadas)`,
      icono: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <rect x="3" y="3" width="7" height="7" />
          <rect x="14" y="3" width="7" height="7" />
          <rect x="14" y="14" width="7" height="7" />
          <rect x="3" y="14" width="7" height="7" />
        </svg>
      )
    }
  } else {
    terceraMetrica = {
      id: 'b',
      etiqueta: 'Cubetas activas',
      simbolo: 'B',
      valor: numeroCubetas,
      subtitulo: `B ≈ √n (${numeroCubetas} cubetas)`,
      icono: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z" />
          <path d="m3.3 7 8.7 5 8.7-5" />
          <path d="M12 22V12" />
        </svg>
      )
    }
  }

  // Determinar el símbolo de complejidad
  let simboloTiempo = 'O(n + B)'
  if (esCounting) simboloTiempo = 'O(n + k)'
  else if (esRadix) simboloTiempo = 'O(d·(n+b))'

  const metricas = [
    {
      id: 'n',
      etiqueta: 'Términos distintos',
      simbolo: 'n',
      valor: totalElementos.toLocaleString(),
      subtitulo: 'Vocabulario analizado',
      icono: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M4 7V4h16v3M9 20h6M12 4v16" />
        </svg>
      )
    },
    {
      id: 'frecuencias',
      etiqueta: 'Rango frecuencias',
      simbolo: 'f_mín · f_máx',
      valor: `${frecuenciaMinima} / ${frecuenciaMaxima}`,
      subtitulo: 'Dispersión de ocurrencias',
      icono: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
        </svg>
      )
    },
    terceraMetrica,
    {
      id: 'tiempo',
      etiqueta: 'Tiempo empírico',
      simbolo: simboloTiempo,
      valor: `${tiempoMs.toFixed(3)} ms`,
      subtitulo: 'Medido en nanosegundos',
      icono: (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10" />
          <polyline points="12 6 12 12 16 14" />
        </svg>
      )
    }
  ]

  return (
    <div className="metricas-min-grid">
      {metricas.map(m => (
        <div key={m.id} className="metrica-min-card metrica-min-card-verde">
          <div className="metrica-min-top">
            <span className="metrica-min-tag metrica-min-tag-verde">{m.simbolo}</span>
            <div className="metrica-min-icon-wrapper metrica-min-icon-verde">{m.icono}</div>
          </div>
          <div className="metrica-min-main">
            <span className="metrica-min-valor">{m.valor}</span>
            <span className="metrica-min-label">{m.etiqueta}</span>
          </div>
          <span className="metrica-min-sub">{m.subtitulo}</span>
        </div>
      ))}
    </div>
  )
}

export default MetricasMinimal
