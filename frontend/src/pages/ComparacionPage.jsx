import { useState } from 'react'
import { obtenerComparacion } from '../api/comparacionApi'
import './ComparacionPage.css'

const formatoTiempo = new Intl.NumberFormat('es-CO', { minimumFractionDigits: 4, maximumFractionDigits: 6 })

function ComparacionPage() {
  const [topK, setTopK] = useState(15)
  const [filtrarStopwords, setFiltrarStopwords] = useState(true)
  const [resultado, setResultado] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState('')

  async function ejecutarComparacion() {
    setCargando(true)
    setError('')
    setResultado(null)
    try {
      setResultado(await obtenerComparacion(topK, filtrarStopwords))
    } catch (fallo) {
      setError(fallo.message)
    } finally {
      setCargando(false)
    }
  }

  const filtrosModificados = resultado &&
    (resultado.topK !== topK || resultado.stopwordsFiltradas !== filtrarStopwords)

  return (
    <div className="dashboard-minimal-container dashboard-verde comparacion-pagina">
      <div className="dashboard-title-row">
        <div className="dashboard-title-group">
          <h1 className="dashboard-main-title">Comparación de ordenamientos</h1>
        </div>
        <p className="dashboard-subtitle">Compara Bucket Sort, Counting Sort y Radix Sort con los mismos datos.</p>
      </div>

      <section className="comparacion-controles" aria-label="Opciones de comparación">
        <label>
          Resultados por algoritmo
          <select value={topK} onChange={evento => setTopK(Number(evento.target.value))} disabled={cargando}>
            {[10, 15, 20, 30, 50].map(cantidad => <option key={cantidad} value={cantidad}>Top {cantidad}</option>)}
          </select>
        </label>
        <label className="comparacion-filtro">
          <input type="checkbox" checked={filtrarStopwords}
            onChange={evento => setFiltrarStopwords(evento.target.checked)} disabled={cargando} />
          Filtrar palabras comunes
        </label>
        <button className="btn-pill-action btn-pill-action-verde" onClick={ejecutarComparacion} disabled={cargando}>
          {cargando ? 'Comparando algoritmos…' : 'Ejecutar los tres y comparar'}
        </button>
      </section>

      <p className="comparacion-ayuda">Cada algoritmo se mide cinco veces. El más rápido se determina por su tiempo promedio.</p>
      {error && <p className="comparacion-error" role="alert">{error}</p>}
      {cargando && <p role="status">Ejecutando la comparación. Espera a que terminen todos los algoritmos.</p>}
      {!resultado && !cargando && !error && (
        <section className="comparacion-tarjeta comparacion-inicio">
          <h2>Una ejecución, todos los resultados</h2>
          <p>Presiona el botón para ver los tiempos, las listas ordenadas y el resumen de la comparación.</p>
        </section>
      )}

      {resultado && (
        <div aria-live="polite">
          {filtrosModificados && <p className="comparacion-aviso">Cambiaste las opciones. Ejecuta otra comparación para aplicarlas.</p>}
          <section className={`comparacion-tarjeta comparacion-resumen ${resultado.resultadosEquivalentes ? '' : 'comparacion-resumen-error'}`}>
            <h2>Resumen de la comparación</h2>
            <p className="comparacion-ganador">{resultado.resumen}</p>
            <p>{resultado.resultadosEquivalentes
              ? 'Todos conservaron las mismas palabras y frecuencias y las ordenaron de mayor a menor.'
              : 'Revisa los resultados marcados como incorrectos antes de comparar su rendimiento.'}</p>
            <div className="comparacion-datos">
              <span>{resultado.totalArticulos} artículos</span>
              <span>{resultado.totalElementos} términos ordenados</span>
              <span>{resultado.algoritmos.length} algoritmos</span>
              <span>{resultado.repeticiones} mediciones por algoritmo</span>
              <span>Palabras comunes: {resultado.stopwordsFiltradas ? 'filtradas' : 'incluidas'}</span>
            </div>
          </section>

          <section className="comparacion-tarjeta">
            <h2>Tiempos de ejecución</h2>
            <div className="comparacion-tabla-scroll">
              <table className="comparacion-tabla">
                <thead><tr><th>Algoritmo</th><th>Tiempo promedio (ms)</th><th>Resultado</th></tr></thead>
                <tbody>
                  {resultado.algoritmos.map(algoritmo => (
                    <tr key={algoritmo.algoritmo} className={resultado.ganadores.includes(algoritmo.algoritmo) ? 'comparacion-fila-ganadora' : ''}>
                      <th scope="row">{algoritmo.algoritmo}
                        {resultado.ganadores.includes(algoritmo.algoritmo) && <span className="comparacion-distintivo">Menor promedio</span>}
                      </th>
                      <td>{formatoTiempo.format(algoritmo.tiempoPromedioMs)}</td>
                      <td>{algoritmo.resultadoCorrecto ? 'Correcto' : 'Incorrecto'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="comparacion-ayuda">Se mide únicamente el ordenamiento. El tiempo puede variar entre comparaciones.</p>
            <details className="comparacion-mediciones">
              <summary>Ver los tiempos de cada ejecución</summary>
              <div className="comparacion-tabla-scroll">
                <table className="comparacion-tabla">
                  <thead><tr><th>Ejecución</th>{resultado.algoritmos.map(a => <th key={a.algoritmo}>{a.algoritmo} (ms)</th>)}</tr></thead>
                  <tbody>{Array.from({ length: resultado.repeticiones }, (_, indice) => (
                    <tr key={indice}><th scope="row">{indice + 1}</th>
                      {resultado.algoritmos.map(a => <td key={a.algoritmo}>{formatoTiempo.format(a.ejecucionesMs[indice])}</td>)}
                    </tr>
                  ))}</tbody>
                </table>
              </div>
            </details>
          </section>

          <h2 className="comparacion-titulo-resultados">Resultados de cada algoritmo · Top {resultado.topK}</h2>
          <p className="comparacion-ayuda">Las palabras con la misma frecuencia pueden aparecer en distinto orden.</p>
          <div className="comparacion-listas">
            {resultado.algoritmos.map(algoritmo => (
              <section className="comparacion-tarjeta" key={algoritmo.algoritmo}>
                <h3>{algoritmo.algoritmo}</h3>
                <div className="comparacion-ranking">
                  <table className="comparacion-tabla">
                    <thead><tr><th>#</th><th>Palabra</th><th>Frecuencia</th></tr></thead>
                    <tbody>{algoritmo.terminos.map((termino, indice) => (
                      <tr key={termino.termino}><td>{indice + 1}</td><th scope="row">{termino.termino}</th><td>{termino.frecuencia}</td></tr>
                    ))}</tbody>
                  </table>
                </div>
              </section>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

export default ComparacionPage
