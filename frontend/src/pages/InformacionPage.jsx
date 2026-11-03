import { useState, useEffect } from 'react'
import { obtenerArticulos } from '../api/bucketSortApi'
import MensajeError from '../components/MensajeError'
import logoUQ from '../assets/Logo UQ.jpg'

const COLORES_GRADIENTE = [
  'linear-gradient(135deg, #bbf7d0 0%, #86efac 100%)',
  'linear-gradient(135deg, #a7f3d0 0%, #6ee7b7 100%)',
  'linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%)',
  'linear-gradient(135deg, #6ee7b7 0%, #34d399 100%)',
  'linear-gradient(135deg, #34d399 0%, #10b981 100%)',
  'linear-gradient(135deg, #ecfdf5 0%, #bbf7d0 100%)',
]

function InformacionPage() {
  const [articulos, setArticulos] = useState([])
  const [busqueda, setBusqueda] = useState('')
  const [expandidoId, setExpandidoId] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)

  async function cargar() {
    setCargando(true)
    setError(null)
    try {
      const datos = await obtenerArticulos()
      setArticulos(datos)
    } catch (err) {
      setError(err.message || 'Error al obtener los artículos desde el servidor.')
    } finally {
      setCargando(false)
    }
  }

  useEffect(() => {
    cargar()
  }, [])

  const filtrados = articulos.filter(a => {
    const termino = busqueda.toLowerCase().trim()
    if (!termino) return true
    const titulo = (a.titulo || '').toLowerCase()
    const archivo = (a.archivo || '').toLowerCase()
    const autores = (a.autores || []).join(' ').toLowerCase()
    return titulo.includes(termino) || autores.includes(termino) || archivo.includes(termino)
  })

  return (
    <div className="info-pagina">
      {/* ── HERO ─────────────────────────────────────────── */}
      <section className="hero-section">
        <div className="hero-izquierda">
          <h1 className="hero-titulo">
            Proyecto<br />
            <span className="hero-titulo-acento">Análisis de Algoritmos</span>
          </h1>

          <div className="hero-equipo">
            <div className="equipo-item">
              <span className="equipo-dot"></span>Elizabeth Maya
            </div>
          </div>
        </div>

        <div className="hero-derecha">
          <div className="hero-img-wrapper">
            <img src={logoUQ} alt="Universidad del Quindío" className="hero-img" />
          </div>
        </div>
      </section>

      {/* ── SECCIÓN ARTÍCULOS ─────────────────────────────── */}
      <section id="seccion-articulos" className="articulos-section">
        <div className="articulos-section-header">
          <h2>Artículos del Corpus Bibliométrico</h2>
          <p>Investigaciones sobre Inteligencia Artificial en Educación (AIEd) consultadas desde el Backend</p>
        </div>

        {/* Error si no se puede conectar con el backend */}
        {error && (
          <div className="error-contenedor-articulos">
            <MensajeError mensaje={error} />
            <div style={{ textAlign: 'center', marginTop: '1rem' }}>
              <button className="btn-secundario" onClick={cargar}>
                ↻ Reintentar consulta al Backend
              </button>
            </div>
          </div>
        )}

        {/* Búsqueda */}
        {!error && (
          <div className="busqueda-barra">
            <div className="busqueda-input-wrapper">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <input
                type="text"
                placeholder="Buscar por título, autor o archivo..."
                value={busqueda}
                onChange={e => setBusqueda(e.target.value)}
              />
              {busqueda && (
                <button className="limpiar-busqueda" onClick={() => setBusqueda('')}>✕</button>
              )}
            </div>
            <div className="conteo-articulos">
              Mostrando <strong>{filtrados.length}</strong> de {articulos.length} artículos del backend
            </div>
          </div>
        )}

        {/* Grid de tarjetas */}
        {cargando ? (
          <div className="cargando-estado">
            <div className="spinner"></div>
            <p>Consultando artículos al Backend Spring Boot (/api/articulos)...</p>
          </div>
        ) : error ? null : filtrados.length === 0 ? (
          <div className="sin-resultados">
            <p>No se encontraron artículos con <strong>"{busqueda}"</strong>.</p>
            <button className="btn-secundario" onClick={() => setBusqueda('')}>Limpiar filtro</button>
          </div>
        ) : (
          <div className="cards-grid">
            {filtrados.map((art, idx) => {
              const expandido = expandidoId === art.id
              const gradiente = COLORES_GRADIENTE[idx % COLORES_GRADIENTE.length]
              const primerAutor = (art.autores || [])[0] || ''
              const masAutores = (art.autores || []).length > 1 ? ` et al.` : ''
              const extracto = (art.resumenOriginal || '').slice(0, 110) + ((art.resumenOriginal || '').length > 110 ? '…' : '')

              return (
                <article key={art.id} className={`art-card ${expandido ? 'expandida' : ''}`}>
                  {/* Imagen superior con gradiente */}
                  <div className="art-card-imagen" style={{ background: gradiente }}>
                    <span className="art-card-num">#{art.id}</span>
                    <div className="art-card-icon-badge">
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                        <polyline points="14 2 14 8 20 8" />
                        <line x1="16" y1="13" x2="8" y2="13" />
                        <line x1="16" y1="17" x2="8" y2="17" />
                        <polyline points="10 9 9 9 8 9" />
                      </svg>
                    </div>
                  </div>

                  {/* Cuerpo */}
                  <div className="art-card-body">
                    <span className="art-card-archivo">📄 {art.archivo}</span>
                    <h3 className="art-card-titulo">{art.titulo}</h3>
                    {primerAutor && (
                      <p className="art-card-autores">👥 {primerAutor}{masAutores}</p>
                    )}
                    {extracto && (
                      <p className="art-card-extracto">{extracto}</p>
                    )}

                    {art.totalPalabras > 0 && (
                      <span className="art-card-palabras">{art.totalPalabras} palabras</span>
                    )}

                    {art.resumenOriginal && (
                      <button
                        className="btn-toggle-abstract"
                        onClick={() => setExpandidoId(expandido ? null : art.id)}
                      >
                        {expandido ? '▲ Ocultar resumen' : '▼ Ver resumen completo'}
                      </button>
                    )}

                    {expandido && (
                      <div className="abstract-contenido">
                        <p>{art.resumenOriginal}</p>
                      </div>
                    )}
                  </div>
                </article>
              )
            })}
          </div>
        )}
      </section>
    </div>
  )
}

export default InformacionPage
