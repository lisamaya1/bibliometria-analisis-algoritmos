function TablaResultados({ terminos = [] }) {
  if (terminos.length === 0) {
    return <p className="sin-datos">No hay términos para mostrar.</p>
  }

  // Frecuencia máxima dentro de la lista visible para escalar la barra visual al 100%
  const maxFrecuencia = terminos[0]?.frecuencia || 1

  return (
    <div className="tabla-wrapper">
      <table className="tabla-morada">
        <thead>
          <tr>
            <th className="th-puesto">Puesto</th>
            <th className="th-termino">Término / Palabra</th>
            <th className="th-frecuencia">Frecuencia</th>
            <th className="th-barra">Distribución Visual</th>
          </tr>
        </thead>
        <tbody>
          {terminos.map((item, index) => {
            const porcentaje = Math.round((item.frecuencia / maxFrecuencia) * 100)
            const esTop1 = index === 0
            const esTop2 = index === 1
            const esTop3 = index === 2

            let medallaClase = ''
            if (esTop1) medallaClase = 'medalla-oro'
            else if (esTop2) medallaClase = 'medalla-plata'
            else if (esTop3) medallaClase = 'medalla-bronce'

            return (
              <tr key={item.termino} className={index < 3 ? 'fila-destacada' : ''}>
                <td className="celda-puesto">
                  <span className={`puesto-badge ${medallaClase}`}>
                    #{index + 1}
                  </span>
                </td>
                <td className="celda-termino">
                  <span className="termino-texto">{item.termino}</span>
                </td>
                <td className="celda-frecuencia">
                  <span className="frecuencia-valor">{item.frecuencia}</span>
                  <span className="frecuencia-sub">ocurrencias</span>
                </td>
                <td className="celda-barra">
                  <div className="barra-contenedor">
                    <div
                      className="barra-progreso-morada"
                      style={{ width: `${porcentaje}%` }}
                    />
                    <span className="barra-porcentaje">{porcentaje}%</span>
                  </div>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

export default TablaResultados
