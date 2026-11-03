function PanelControles({
  topK,
  setTopK,
  filtrarStopwords,
  setFiltrarStopwords,
  cargando,
  onEjecutar,
  nombreAlgoritmo = 'Bucket Sort'
}) {
  const atajosTop = [10, 15, 20, 30, 50]

  return (
    <section className="controles-card">
      <div className="controles-header">
        <div className="controles-header-badge">CONFIGURACIÓN DE ENTRADA</div>
        <h2>⚙️ Parámetros del Algoritmo</h2>
        <p className="controles-descripcion">
          Ajusta los parámetros para ordenar los términos del vocabulario bibliométrico por frecuencia mediante {nombreAlgoritmo}.
        </p>
      </div>

      <div className="controles-body">
        {/* Campo: Top K */}
        <div className="control-grupo">
          <label htmlFor="topK" className="control-label">
            <span>Términos en Ranking (Top K):</span>
            <span className="control-valor-actual">{topK} palabras</span>
          </label>
          <div className="input-con-atajos">
            <input
              id="topK"
              type="number"
              min={1}
              max={100}
              value={topK}
              onChange={e => setTopK(Math.max(1, Math.min(100, Number(e.target.value) || 1)))}
              className="input-numero"
            />
            <div className="atajos-chips">
              {atajosTop.map(k => (
                <button
                  key={k}
                  type="button"
                  className={`chip-btn ${topK === k ? 'activo' : ''}`}
                  onClick={() => setTopK(k)}
                >
                  Top {k}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Campo: Filtrar stopwords */}
        <div className="control-grupo">
          <label className="toggle-switch-container">
            <input
              id="stopwords"
              type="checkbox"
              checked={filtrarStopwords}
              onChange={e => setFiltrarStopwords(e.target.checked)}
              className="toggle-checkbox"
            />
            <span className="toggle-slider"></span>
            <div className="toggle-info">
              <span className="toggle-title">Filtrar palabras vacías (Stopwords en inglés)</span>
              <span className="toggle-hint">
                {filtrarStopwords
                  ? 'Activado: Excluye palabras funcionales ("and", "the", "of", "in"...) para resaltar vocabulario temático.'
                  : 'Desactivado: Muestra el vocabulario completo incluyendo palabras funcionales.'}
              </span>
            </div>
          </label>
        </div>
      </div>

      {/* Botón de ejecución */}
      <div className="controles-footer">
        <button
          className="btn-ejecutar-morado"
          onClick={onEjecutar}
          disabled={cargando}
        >
          {cargando ? (
            <>
              <span className="spinner-btn"></span>
              Procesando con {nombreAlgoritmo}...
            </>
          ) : (
            <>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <polygon points="5 3 19 12 5 21 5 3" />
              </svg>
              Ejecutar {nombreAlgoritmo}
            </>
          )}
        </button>
      </div>
    </section>
  )
}

export default PanelControles
