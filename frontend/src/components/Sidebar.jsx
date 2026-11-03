import { useEffect } from 'react'

function Sidebar({ ventanaActiva, setVentanaActiva, totalArticulos = 20, isOpen, setIsOpen }) {
  const items = [
    {
      id: 'informacion',
      titulo: 'Información',
      subtitulo: 'Corpus de artículos',
      icono: (
        <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
          <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
          <line x1="9" y1="7" x2="15" y2="7" />
          <line x1="9" y1="11" x2="15" y2="11" />
        </svg>
      ),
      badge: `${totalArticulos}`
    },
    {
      id: 'bucketsort',
      titulo: 'Bucket Sort',
      subtitulo: 'Distribución O(n+B)',
      icono: (
        <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z" />
          <path d="m3.3 7 8.7 5 8.7-5" />
          <path d="M12 22V12" />
        </svg>
      ),
      badge: 'O(n+B)'
    },
    {
      id: 'countingsort',
      titulo: 'Counting Sort',
      subtitulo: 'Histograma O(n+k)',
      icono: (
        <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <line x1="18" y1="20" x2="18" y2="10" />
          <line x1="12" y1="20" x2="12" y2="4" />
          <line x1="6" y1="20" x2="6" y2="14" />
        </svg>
      ),
      badge: 'O(n+k)'
    },
    {
      id: 'radixsort',
      titulo: 'Radix Sort',
      subtitulo: 'Por dígitos O(d·(n+b))',
      icono: (
        <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <rect x="3" y="3" width="7" height="7" />
          <rect x="14" y="3" width="7" height="7" />
          <rect x="14" y="14" width="7" height="7" />
          <rect x="3" y="14" width="7" height="7" />
        </svg>
      ),
      badge: 'O(d·(n+b))'
    },
    {
      id: 'jaccard',
      titulo: 'Jaccard',
      subtitulo: 'Similitud entre abstracts',
      icono: (
        <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="9" cy="12" r="6" />
          <circle cx="15" cy="12" r="6" />
        </svg>
      )
    },
    {
      id: 'dice',
      titulo: 'Sørensen-Dice',
      subtitulo: 'Similitud entre abstracts',
      icono: (
        <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="9" cy="12" r="6" />
          <circle cx="15" cy="12" r="6" />
          <path d="M12 7v10" />
        </svg>
      )
    },
    {
      id: 'levenshtein',
      titulo: 'Levenshtein',
      subtitulo: 'Distancia entre abstracts',
      icono: (
        <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M4 6h10M4 12h6M4 18h10M15 10l4 4-4 4M19 14h-7" />
        </svg>
      )
    },
    {
      id: 'coseno-tfidf',
      titulo: 'Coseno TF-IDF',
      subtitulo: 'Vectores de términos',
      icono: (
        <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M4 20V4M4 20h16M4 20l8-14M4 20l15-7M10 20a6 6 0 0 0-3-5" />
        </svg>
      )
    },
    {
      id: 'comparacion-clasica',
      titulo: 'Comparación clásica',
      subtitulo: 'Dos o más artículos',
      icono: (
        <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M4 4h16v16H4zM4 9h16M10 4v16M15 9v11" />
        </svg>
      )
    },
    {
      id: 'minilm',
      titulo: 'MiniLM · IA 1',
      subtitulo: 'Embeddings locales',
      icono: (
        <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="6" cy="6" r="2" /><circle cx="18" cy="6" r="2" /><circle cx="12" cy="18" r="2" />
          <path d="M8 6h8M7 8l4 8M17 8l-4 8" />
        </svg>
      )
    },
    {
      id: 'bge',
      titulo: 'BGE · IA 2',
      subtitulo: 'Embeddings locales',
      icono: (
        <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="6" cy="6" r="2" /><circle cx="18" cy="6" r="2" /><circle cx="12" cy="18" r="2" />
          <path d="M8 6h8M7 8l4 8M17 8l-4 8" />
        </svg>
      )
    },
    {
      id: 'clustering',
      titulo: 'Clustering Jerárquico',
      subtitulo: 'Dendrogramas y Silhouette',
      icono: (
        <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M4 4h6v8h5v8h5M4 12h6M4 20h11" />
        </svg>
      )
    },
    {
      id: 'busqueda-exacta',
      titulo: 'Búsqueda exacta',
      subtitulo: 'KMP, Boyer-Moore y Rabin-Karp',
      icono: (
        <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="10" cy="10" r="6" /><path d="m15 15 6 6M7 10h6" />
        </svg>
      )
    },
    {
      id: 'comparacion',
      titulo: 'Comparación',
      subtitulo: 'Resultados y tiempos',
      icono: (
        <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M4 20h16M7 16V9M12 16V4M17 16v-5" />
        </svg>
      )
    }
  ]

  useEffect(() => {
    localStorage.setItem('sidebarAbierto', String(isOpen))
  }, [isOpen])

  return (
    <aside className={`sidebar-minimal sidebar-verde ${isOpen ? 'is-open' : 'is-collapsed'}`}>
      {/* Encabezado minimalista */}
      <div className="sidebar-min-header">
        <button
          className="sidebar-min-toggle"
          onClick={() => setIsOpen(!isOpen)}
          title={isOpen ? 'Contraer menú' : 'Expandir menú'}
          aria-label="Alternar menú lateral"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            {isOpen ? (
              <>
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </>
            ) : (
              <>
                <line x1="4" y1="7" x2="20" y2="7" />
                <line x1="4" y1="12" x2="20" y2="12" />
                <line x1="4" y1="17" x2="20" y2="17" />
              </>
            )}
          </svg>
        </button>

        {isOpen && (
          <div className="sidebar-min-brand">
            <div className="sidebar-min-logo sidebar-min-logo-verde">
              <span>B</span>
            </div>
            <div className="sidebar-min-title-group">
              <span className="sidebar-min-title">Bibliometría</span>
              <span className="sidebar-min-subtitle">Análisis 2026-2</span>
            </div>
          </div>
        )}
      </div>

      {isOpen && <div className="sidebar-min-section-label">ALGORITMOS & CORPUS</div>}

      {/* Lista de navegación simple en verde */}
      <nav className="sidebar-min-nav">
        {items.map(item => {
          const activo = ventanaActiva === item.id
          return (
            <button
              key={item.id}
              className={`sidebar-min-item ${activo ? 'activo activo-verde' : ''}`}
              onClick={() => setVentanaActiva(item.id)}
              title={!isOpen ? `${item.titulo} (${item.badge})` : undefined}
            >
              <span className="sidebar-min-icon">{item.icono}</span>
              {isOpen && (
                <div className="sidebar-min-text">
                  <span className="sidebar-min-item-title">{item.titulo}</span>
                  <span className="sidebar-min-item-sub">{item.subtitulo}</span>
                </div>
              )}
              {isOpen && item.badge && (
                <span className="sidebar-min-badge">{item.badge}</span>
              )}
            </button>
          )
        })}
      </nav>

      {/* Pie minimalista */}
      {isOpen && (
        <div className="sidebar-min-footer">
          <div className="sidebar-min-uq">
            <span className="sidebar-min-dot sidebar-min-dot-verde"></span>
            <div>
              <p className="sidebar-min-uq-text">Universidad del Quindío</p>
              <p className="sidebar-min-uq-sub">Ingeniería de Sistemas</p>
            </div>
          </div>
        </div>
      )}
    </aside>
  )
}

export default Sidebar
