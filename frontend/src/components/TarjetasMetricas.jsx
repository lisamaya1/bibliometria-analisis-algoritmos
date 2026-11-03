function TarjetasMetricas({
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
  const valorParametro = esCounting ? (rangoK || numeroCubetas) : numeroCubetas

  const metricas = [
    {
      id: 'terminos',
      valor: totalElementos.toLocaleString(),
      etiqueta: 'Número de términos distintos',
      simbolo: '(n)',
      detalle: 'Elementos ordenados en el vocabulario',
      icono: (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M4 7V4h16v3M9 20h6M12 4v16" />
        </svg>
      )
    },
    {
      id: 'frecuencias',
      valor: `${frecuenciaMinima} / ${frecuenciaMaxima}`,
      etiqueta: 'Frecuencia mínima / máxima',
      simbolo: '(f_mín / f_máx)',
      detalle: `Rango cuantitativo de dispersión [${frecuenciaMinima}, ${frecuenciaMaxima}]`,
      icono: (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
        </svg>
      )
    },
    {
      id: 'parametroClave',
      valor: valorParametro,
      etiqueta: esCounting ? 'Rango de claves cuantitativas' : 'Número de cubetas utilizado',
      simbolo: esCounting ? '(k)' : '(B)',
      detalle: esCounting
        ? `Rango de frecuencias: k = f_máx - f_mín + 1 (${valorParametro})`
        : `Partición óptima: B ≈ √n (${valorParametro} cubetas)`,
      icono: (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z" />
          <path d="m3.3 7 8.7 5 8.7-5" />
          <path d="M12 22V12" />
        </svg>
      )
    },
    {
      id: 'tiempo',
      valor: `${tiempoMs.toFixed(3)} ms`,
      etiqueta: 'Tiempo empírico de ejecución',
      simbolo: esCounting ? 'O(n + k)' : 'O(n + B)',
      detalle: 'Medido con System.nanoTime() en backend',
      icono: (
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="12" cy="12" r="10" />
          <polyline points="12 6 12 12 16 14" />
        </svg>
      )
    }
  ]

  return (
    <div className="apartado-parametros">
      <div className="apartado-header">
        <span className="apartado-badge">APARTADO REQUERIDO · PARÁMETROS DEL ALGORITMO</span>
        <h3 className="apartado-titulo">Métricas de Ejecución {algoritmo}</h3>
        <p className="apartado-sub">
          Parámetros formales de entrada y rendimiento asintótico del ordenamiento sobre el corpus ({totalArticulos} artículos).
        </p>
      </div>

      <div className="grid-metricas">
        {metricas.map(m => (
          <div className="tarjeta-metrica-destacada" key={m.id}>
            <div className="metrica-icono-box">{m.icono}</div>
            <div className="metrica-cuerpo">
              <div className="metrica-valor-row">
                <span className="metrica-valor-grande">{m.valor}</span>
                <span className="metrica-simbolo-tag">{m.simbolo}</span>
              </div>
              <span className="metrica-etiqueta-nombre">{m.etiqueta}</span>
              <span className="metrica-detalle-texto">{m.detalle}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

export default TarjetasMetricas
