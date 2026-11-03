function TablaEjecuciones({
  ejecuciones = [],
  promedio = 0,
  stopwordsFiltradas = true,
  totalElementos = 1012
}) {
  if (!ejecuciones || ejecuciones.length === 0) {
    return null
  }

  // Encontrar el valor máximo entre las ejecuciones para escalar las barras visuales
  const maxTiempo = Math.max(...ejecuciones, promedio, 0.001)

  return (
    <div className="card-grafica-minimal card-tabla-minimal" style={{ marginBottom: '1.75rem' }}>
      <div className="card-grafica-header">
        <div className="card-grafica-title-group">
          <svg className="card-grafica-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="10" />
            <polyline points="12 6 12 12 16 14" />
          </svg>
          <span className="card-grafica-title">MEDICIÓN EMPÍRICA FORMAL · 5 MUESTRAS CONSECUTIVAS</span>
        </div>
        <div className="pill-promedio-verde">
          <span className="pill-promedio-dot"></span>
          <span>Tiempo Promedio: <strong>{promedio.toFixed(4)} ms</strong></span>
        </div>
      </div>

      <p className="dashboard-subtitle" style={{ marginBottom: '1.15rem' }}>
        Registro secuencial de tiempos reales sobre los {totalElementos.toLocaleString()} términos del vocabulario{' '}
        <strong>{stopwordsFiltradas ? 'temático (sin stopwords)' : 'completo (con stopwords)'}</strong>.
      </p>

      <div className="tabla-min-scroll">
        <table className="tabla-minimal-ref">
          <thead>
            <tr>
              <th className="th-min-num">#</th>
              <th>MUESTRA / ITERACIÓN</th>
              <th>RÉGIMEN DE EJECUCIÓN</th>
              <th className="th-min-frec">TIEMPO REAL</th>
              <th className="th-min-barra">ESCALA RELATIVA</th>
            </tr>
          </thead>
          <tbody>
            {ejecuciones.map((tiempo, idx) => {
              const esPrimera = idx === 0
              const porcentaje = Math.max(12, Math.round((tiempo / maxTiempo) * 100))

              return (
                <tr key={idx} className="tr-minimal-row">
                  <td className="td-min-num">
                    <span className="puesto-circle puesto-circle-other-verde">
                      {idx + 1}
                    </span>
                  </td>
                  <td>
                    <span className="termino-nombre-bold">Ejecución {idx + 1}</span>
                  </td>
                  <td>
                    {esPrimera ? (
                      <span className="tag-frio-verde">
                        <span className="punto-frio-verde"></span> En frío (Compilación JIT)
                      </span>
                    ) : (
                      <span className="tag-caliente-verde">
                        <span className="punto-caliente-verde"></span> Régimen estabilizado
                      </span>
                    )}
                  </td>
                  <td className="th-min-frec">
                    <span className="frec-valor-num">{tiempo.toFixed(4)}</span>
                    <span className="frec-uds-sub">ms</span>
                  </td>
                  <td className="td-min-barra">
                    <div className="barra-min-flex">
                      <div className="barra-min-track">
                        <div
                          className="barra-min-fill-verde"
                          style={{ width: `${porcentaje}%` }}
                        />
                      </div>
                      <span className="barra-min-pct">{porcentaje}%</span>
                    </div>
                  </td>
                </tr>
              )
            })}

            {/* Fila destacada del Promedio */}
            <tr className="fila-promedio-verde">
              <td className="td-min-num">
                <span className="puesto-circle puesto-circle-1-verde">★</span>
              </td>
              <td>
                <strong className="termino-nombre-bold" style={{ color: 'var(--verde-900)' }}>
                  PROMEDIO ARITMÉTICO
                </strong>
              </td>
              <td>
                <span className="tag-promedio-verde">5 muestras independientes</span>
              </td>
              <td className="th-min-frec">
                <strong className="frec-valor-num" style={{ color: 'var(--verde-800)', fontSize: '0.92rem' }}>
                  {promedio.toFixed(4)}
                </strong>
                <span className="frec-uds-sub" style={{ color: 'var(--verde-700)' }}>ms</span>
              </td>
              <td className="td-min-barra">
                <div className="barra-min-flex">
                  <div className="barra-min-track" style={{ background: 'var(--verde-200)' }}>
                    <div
                      className="barra-min-fill-verde"
                      style={{
                        width: `${Math.round((promedio / maxTiempo) * 100)}%`,
                        background: 'var(--verde-700)'
                      }}
                    />
                  </div>
                  <span className="barra-min-pct" style={{ color: 'var(--verde-800)', fontWeight: 700 }}>
                    {Math.round((promedio / maxTiempo) * 100)}%
                  </span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div className="ejecuciones-nota-pie-verde">
        <span>
          💡 <strong>Nota metodológica:</strong> La ejecución #1 absorbe la sobrecarga inicial de calentamiento y compilación JIT (HotSpot) en la máquina virtual Java. Las ejecuciones #2 a #5 reflejan el régimen estabilizado y la capacidad algorítmica pura de Counting Sort en memoria.
        </span>
      </div>
    </div>
  )
}

export default TablaEjecuciones
