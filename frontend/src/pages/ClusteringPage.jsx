import { useEffect, useState } from 'react'
import Dendrograma from '../components/Dendrograma'
import { criteriosClustering, obtenerDendrograma, obtenerEvaluacionClustering } from '../api/clusteringApi'
import { mismaParticion } from '../utils/dendrograma'
import './ComparacionPage.css'
import './ClusteringPage.css'

const decimal = new Intl.NumberFormat('es-CO', { minimumFractionDigits: 7, maximumFractionDigits: 9 })

export default function ClusteringPage() {
  const [criterio, setCriterio] = useState('single')
  const [datos, setDatos] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [intento, setIntento] = useState(0)
  const [evaluacion, setEvaluacion] = useState(null)
  const [cargandoEvaluacion, setCargandoEvaluacion] = useState(true)
  const [errorEvaluacion, setErrorEvaluacion] = useState('')
  const [intentoEvaluacion, setIntentoEvaluacion] = useState(0)

  useEffect(() => {
    const control = new AbortController()
    obtenerDendrograma(criterio, control.signal).then(resultado => {
      if (!control.signal.aborted) setDatos(resultado)
    }).catch(fallo => {
      if (!control.signal.aborted) setError(fallo.message)
    }).finally(() => {
      if (!control.signal.aborted) setCargando(false)
    })
    return () => control.abort()
  }, [criterio, intento])

  useEffect(() => {
    const control = new AbortController()
    obtenerEvaluacionClustering(control.signal).then(resultado => {
      if (!control.signal.aborted) setEvaluacion(resultado)
    }).catch(fallo => {
      if (!control.signal.aborted) setErrorEvaluacion(fallo.message)
    }).finally(() => {
      if (!control.signal.aborted) setCargandoEvaluacion(false)
    })
    return () => control.abort()
  }, [intentoEvaluacion])

  function cambiarCriterio(valor) {
    if (valor === criterio) return
    setDatos(null)
    setError('')
    setCargando(true)
    setCriterio(valor)
  }

  function reintentar() {
    setError('')
    setCargando(true)
    setIntento(v => v + 1)
  }

  const mejor = evaluacion?.criterios.find(c => c.criterio === criterio)?.mejor
  const destacados = evaluacion?.criterios.filter(c => evaluacion.criteriosMasCoherentes.includes(c.criterio)) || []
  const compartenParticion = destacados.length > 1 && destacados.every(c =>
    c.mejor.k === destacados[0].mejor.k && mismaParticion(c.mejor.clusters, destacados[0].mejor.clusters))

  return <div className="dashboard-minimal-container dashboard-verde comparacion-pagina clustering-pagina">
    <div className="dashboard-title-row">
      <div className="dashboard-title-group"><h1 className="dashboard-main-title">Clustering Jerárquico</h1></div>
      <p className="dashboard-subtitle">Agrupamiento de abstracts mediante distancias de Coseno TF-IDF y evaluación interna con Silhouette.</p>
    </div>

    <section className="comparacion-tarjeta" aria-label="Evaluación interna de clustering">
      <h2>Evaluación con Silhouette</h2>
      {cargandoEvaluacion && <p role="status">Cargando evaluación de Silhouette…</p>}
      {errorEvaluacion && <div className="comparacion-error" role="alert">
        <p>{errorEvaluacion}</p>
        <button className="btn-secundario" onClick={() => {
          setErrorEvaluacion(''); setCargandoEvaluacion(true); setIntentoEvaluacion(v => v + 1)
        }}>Reintentar evaluación</button>
      </div>}
      {evaluacion && <>
        <div className="clustering-evaluaciones">
          {evaluacion.criterios.map(c => <div key={c.criterio} className={`clustering-evaluacion ${c.criterio === criterio ? 'clustering-evaluacion-seleccionada' : ''}`} data-evaluacion={c.criterio}>
            <h3>{criteriosClustering[c.criterio]}</h3>
            <p>Mejor k: <strong>{c.mejor.k}</strong></p>
            <p>Silhouette: <strong>{decimal.format(c.mejor.silhouette)}</strong></p>
          </div>)}
        </div>
        <div className="comparacion-resumen clustering-conclusion">
          <p><strong>Criterios más coherentes según Silhouette: {destacados.map(c => criteriosClustering[c.criterio]).join(' y ')}.</strong></p>
          {destacados.length > 1 && <p>
            {destacados.length === 2 ? 'Ambos obtienen' : 'Obtienen'} el mismo valor máximo de Silhouette: {decimal.format(evaluacion.maximoSilhouette)}.
            {compartenParticion && ` Producen la misma partición óptima para k=${destacados[0].mejor.k}.`}
          </p>}
          <p>Los valores actuales de Silhouette son cercanos a cero, por lo que la separación entre grupos es débil con la representación TF-IDF utilizada. Esta evaluación no establece un algoritmo universalmente mejor.</p>
        </div>
      </>}
    </section>

    <div className="comparacion-controles clustering-controles">
      <label htmlFor="criterio-clustering">Criterio de agrupamiento
        <select id="criterio-clustering" value={criterio} onChange={e => cambiarCriterio(e.target.value)}>
          {Object.entries(criteriosClustering).map(([id, nombre]) => <option key={id} value={id}>{nombre}</option>)}
        </select>
      </label>
    </div>
    {cargando && <p className="comparacion-ayuda" role="status">Cargando dendrograma de {criteriosClustering[criterio]}…</p>}
    {error && <div className="comparacion-error" role="alert"><p>{error}</p><button className="btn-secundario" onClick={reintentar}>Reintentar dendrograma</button></div>}
    {datos && <section className="comparacion-tarjeta clustering-dendrograma" aria-label={`Resultados de ${criteriosClustering[criterio]}`}>
      <div className="comparacion-datos clustering-datos">
        <span>Artículos: <strong>{datos.cantidadArticulos}</strong></span>
        <span>Fusiones: <strong>{datos.fusiones.length}</strong></span>
        <span>Criterio: <strong>{criteriosClustering[criterio]}</strong></span>
        {mejor && <><span>Mejor k: <strong>{mejor.k}</strong></span><span>Silhouette: <strong>{decimal.format(mejor.silhouette)}</strong></span></>}
      </div>
      <Dendrograma key={criterio} datos={datos} nombre={criteriosClustering[criterio]} mejor={mejor} />
    </section>}
  </div>
}
