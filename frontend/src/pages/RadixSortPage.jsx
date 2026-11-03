import { useState } from 'react'
import { obtenerResultadoRadixSort } from '../api/radixSortApi'
import TablaResultadosMinimal from '../components/TablaResultadosMinimal'
import MetricasMinimal from '../components/MetricasMinimal'
import MensajeError from '../components/MensajeError'

function RadixSortPage() {
  const [topK, setTopK] = useState(15)
  const [filtrarStopwords, setFiltrarStopwords] = useState(true)
  const [resultado, setResultado] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState(null)

  const atajosTop = [10, 15, 20, 30, 50]

  // El algoritmo SOLO se ejecuta cuando el usuario hace clic en el botón
  async function handleEjecutarRadixSort() {
    setCargando(true)
    setError(null)
    try {
      const datos = await obtenerResultadoRadixSort(topK, filtrarStopwords)
      setResultado(datos)
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }

  const terminos = resultado?.terminos || []
  const numDigitos = resultado?.numDigitos || 0

  return (
    <div className="dashboard-minimal-container dashboard-verde">
      {/* ─── BARRA DE PESTAÑAS / PILLS EN VERDE ─── */}
      <div className="pills-nav-bar">
        <div className="pills-nav-left">
          <div className="pill-item pill-active-verde">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="3" width="7" height="7" />
              <rect x="14" y="3" width="7" height="7" />
              <rect x="14" y="14" width="7" height="7" />
              <rect x="3" y="14" width="7" height="7" />
            </svg>
            <span>Análisis Radix Sort</span>
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
            onClick={handleEjecutarRadixSort}
            disabled={cargando}
          >
            {cargando ? (
              <>
                <span className="spinner-pill"></span>
                <span>Procesando con Radix Sort…</span>
              </>
            ) : (
              <>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <polygon points="5 3 19 12 5 21 5 3" />
                </svg>
                <span>Ejecutar Radix Sort</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* ─── TÍTULO PRINCIPAL MINIMALISTA ─── */}
      <div className="dashboard-title-row">
        <div className="dashboard-title-group">
          <h1 className="dashboard-main-title">Análisis de Palabras</h1>
          <span className="dashboard-badge-capsule dashboard-badge-capsule-verde">O(d·(n+b))</span>
        </div>
        <p className="dashboard-subtitle">
          Ordenamiento no comparativo por dígitos (LSD) con Counting Sort estable como subrutina sobre el corpus documental AIEd.
        </p>
      </div>

      {/* Error si el backend falla */}
      {error && <MensajeError mensaje={error} />}

      {/* ─── ESTADO INICIAL (Aún no ejecutado) ─── */}
      {!resultado && !cargando && !error && (
        <div className="bienvenida-minimal-card">
          <div className="bienvenida-min-body">
            <h3>Listo para ejecutar Radix Sort</h3>
          </div>
        </div>
      )}

      {/* ─── RESULTADOS (Solo tras presionar Ejecutar) ─── */}
      {resultado && (
        <>
          {/* APARTADO REQUERIDO: Métricas Minimalistas en Verde */}
          <MetricasMinimal
            tiempoMs={resultado.tiempoMs || resultado.tiempoPromedioMs || 0}
            totalElementos={resultado.totalElementos || 0}
            rangoK={numDigitos}
            numeroCubetas={numDigitos}
            frecuenciaMinima={resultado.frecuenciaMinima ?? 1}
            frecuenciaMaxima={resultado.frecuenciaMaxima ?? 1}
            totalArticulos={resultado.totalArticulos || 20}
            algoritmo="Radix Sort"
          />

          {/* ÚNICA SECCIÓN DE RESULTADOS: Tabla de Resultados a Ancho Completo */}
          <section className="dashboard-tabla-seccion-full">
            <TablaResultadosMinimal
              terminos={terminos}
              esRadix={true}
            />
          </section>

          {/* Pie informativo sutil */}
          <div className="dashboard-footer-info-verde">
            <span>
              ✓ Se ordenaron <strong>{resultado.totalElementos}</strong> términos únicos mediante <strong>Radix Sort O(d·(n+b))</strong> con d = <strong>{numDigitos}</strong> dígitos en <strong>{(resultado.tiempoMs || resultado.tiempoPromedioMs)?.toFixed(4)} ms</strong>.
            </span>
          </div>
        </>
      )}
    </div>
  )
}

export default RadixSortPage
