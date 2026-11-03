function TablaResultadosMinimal({ terminos = [], numeroCubetas = 10, esCounting = false, esRadix = false }) {
  if (!terminos || terminos.length === 0) {
    return <p className="sin-datos-minimal">No hay términos disponibles en este ranking.</p>
  }

  const maxFrecuencia = terminos[0]?.frecuencia || 1

  return (
    <div className="card-grafica-minimal card-tabla-minimal">
      <div className="card-grafica-header">
        <div className="card-grafica-title-group">
          <svg className="card-grafica-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="8" y1="6" x2="21" y2="6" />
            <line x1="8" y1="12" x2="21" y2="12" />
            <line x1="8" y1="18" x2="21" y2="18" />
            <line x1="3" y1="6" x2="3.01" y2="6" />
            <line x1="3" y1="12" x2="3.01" y2="12" />
            <line x1="3" y1="18" x2="3.01" y2="18" />
          </svg>
          <span className="card-grafica-title">TABLA DE RESULTADOS · RANKING DE PALABRAS</span>
        </div>
      </div>

      <div className="tabla-min-scroll">
        <table className="tabla-minimal-ref">
          <thead>
            <tr>
              <th className="th-min-num">#</th>
              <th className="th-min-termino">PALABRA / TÉRMINO</th>
              <th className="th-min-frec">FRECUENCIA</th>
              <th className="th-min-cubeta">{esCounting ? 'CLAVE (f)' : esRadix ? 'DÍGITOS (d)' : 'CUBETA'}</th>
              <th className="th-min-barra">% DEL TOP</th>
            </tr>
          </thead>
          <tbody>
            {terminos.map((item, index) => {
              const porcentaje = Math.round((item.frecuencia / maxFrecuencia) * 100)
              const puesto = index + 1

              // Puestos en tonos verdes
              let circleColorClass = 'puesto-circle-other-verde'
              if (puesto === 1) circleColorClass = 'puesto-circle-1-verde'
              else if (puesto === 2) circleColorClass = 'puesto-circle-2-verde'
              else if (puesto === 3) circleColorClass = 'puesto-circle-3-verde'

              const cubetaAprox = Math.min(
                numeroCubetas - 1,
                Math.floor((item.frecuencia / maxFrecuencia) * (numeroCubetas - 1))
              )

              return (
                <tr key={item.termino} className="tr-minimal-row">
                  <td className="td-min-num">
                    <span className={`puesto-circle ${circleColorClass}`}>
                      {puesto}
                    </span>
                  </td>
                  <td className="td-min-termino">
                    <span className="termino-nombre-bold">{item.termino}</span>
                  </td>
                  <td className="td-min-frec">
                    <span className="frec-valor-num">{item.frecuencia}</span>
                    <span className="frec-uds-sub">uds</span>
                  </td>
                  <td className="td-min-cubeta">
                    <span className="cubeta-tag-verde">
                      {esCounting ? `f_${item.frecuencia}` : esRadix ? `d_${String(item.frecuencia).length}` : `B_${cubetaAprox}`}
                    </span>
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
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default TablaResultadosMinimal
