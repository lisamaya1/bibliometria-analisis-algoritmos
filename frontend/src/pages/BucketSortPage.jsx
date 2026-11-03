import { useState } from 'react'
import { obtenerResultadoBucketSort } from '../api/bucketSortApi'
import TablaResultadosMinimal from '../components/TablaResultadosMinimal'
import MetricasMinimal from '../components/MetricasMinimal'
import MensajeError from '../components/MensajeError'

function BucketSortPage() {
  const [topK, setTopK] = useState(15)
  const [filtrarStopwords, setFiltrarStopwords] = useState(true)
  const [resultado, setResultado] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState(null)

  const atajosTop = [10, 15, 20, 30, 50]

  // El algoritmo SOLO se ejecuta cuando el usuario hace clic en el botón
  async function handleEjecutarBucketSort() {
    setCargando(true)
    setError(null)
    try {
      const datos = await obtenerResultadoBucketSort(topK, filtrarStopwords)
      setResultado(datos)
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }

  const terminos = resultado?.terminos || []

  return (
    <div className="dashboard-minimal-container dashboard-verde">
      {/* ─── BARRA DE PESTAÑAS / PILLS EN VERDE ─── */}
      <div className="pills-nav-bar">
        <div className="pills-nav-left">
          <div className="pill-item pill-active-verde">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z" />
              <path d="m3.3 7 8.7 5 8.7-5" />
              <path d="M12 22V12" />
            </svg>
            <span>Análisis Bucket Sort</span>
          </div>

          {/* Atajos de Top K estilo pill */}
          <div className="pills-subgroup pills-subgroup-verde">
            {atajosTop.map(k => (
              <button
                key={k}
                className={`pill-item pill-choice ${topK === k ? 'pill-selected-verde' : ''}`}
                onClick={() => setTopK(k)}
                disabled={cargando}
              >
                Top {k}
              </button>
            ))}
          </div>

          {/* Filtro Stopwords en pastilla */}
          <button
            className={`pill-item pill-choice pill-toggle ${filtrarStopwords ? 'pill-on-verde' : ''}`}
            onClick={() => setFiltrarStopwords(!filtrarStopwords)}
            disabled={cargando}
            title={filtrarStopwords ? 'Stopwords excluidas (vocabulario temático)' : 'Stopwords incluidas'}
          >
            <span className="pill-indicator-dot pill-indicator-dot-verde"></span>
            <span>Stopwords: {filtrarStopwords ? 'Filtradas' : 'Completas'}</span>
          </button>
        </div>

        {/* Botón único de ejecución */}
        <div className="pills-nav-right">
          <button
            className="btn-pill-action btn-pill-action-verde"
            onClick={handleEjecutarBucketSort}
            disabled={cargando}
          >
            {cargando ? (
              <>
                <span className="spinner-pill"></span>
                <span>Procesando con Bucket Sort…</span>
              </>
            ) : (
              <>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <polygon points="5 3 19 12 5 21 5 3" />
                </svg>
                <span>Ejecutar Bucket Sort</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* ─── TÍTULO PRINCIPAL MINIMALISTA ─── */}
      <div className="dashboard-title-row">
        <div className="dashboard-title-group">
          <h1 className="dashboard-main-title">Análisis de Palabras</h1>
          <span className="dashboard-badge-capsule dashboard-badge-capsule-verde">O(n + B)</span>
        </div>
        <p className="dashboard-subtitle">
          Distribución cuantitativa y ordenamiento lineal por cubetas sobre el corpus documental AIEd.
        </p>
      </div>

      {/* Error si el backend falla */}
      {error && <MensajeError mensaje={error} />}

      {/* ─── ESTADO INICIAL (Aún no ejecutado) ─── */}
      {!resultado && !cargando && !error && (
        <div className="bienvenida-minimal-card">
          <div className="bienvenida-min-body">
            <h3>Listo para ejecutar Bucket Sort</h3>
          </div>
        </div>
      )}

      {/* ─── RESULTADOS (Solo tras presionar Ejecutar) ─── */}
      {resultado && (
        <>
          {/* APARTADO REQUERIDO: Métricas Minimalistas en Verde */}
          <MetricasMinimal
            tiempoMs={resultado.tiempoMs || 0}
            totalElementos={resultado.totalElementos || 0}
            numeroCubetas={resultado.numeroCubetas || 0}
            frecuenciaMinima={resultado.frecuenciaMinima ?? 1}
            frecuenciaMaxima={resultado.frecuenciaMaxima ?? 1}
            totalArticulos={resultado.totalArticulos || 20}
            algoritmo="Bucket Sort"
          />

          {/* ÚNICA SECCIÓN DE RESULTADOS: Tabla de Resultados a Ancho Completo */}
          <section className="dashboard-tabla-seccion-full">
            <TablaResultadosMinimal
              terminos={terminos}
              numeroCubetas={resultado.numeroCubetas || 10}
            />
          </section>

          {/* Pie informativo sutil */}
          <div className="dashboard-footer-info-verde">
            <span>
              ✓ Se ordenaron <strong>{resultado.totalElementos}</strong> términos únicos mediante <strong>{resultado.numeroCubetas} cubetas</strong> en <strong>{resultado.tiempoMs?.toFixed(4)} ms</strong>.
            </span>
          </div>
        </>
      )}
    </div>
  )
}

export default BucketSortPage
