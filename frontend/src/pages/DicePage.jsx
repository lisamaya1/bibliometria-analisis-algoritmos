import { useEffect, useState } from 'react'
import { obtenerArticulos } from '../api/bucketSortApi'
import { obtenerDice } from '../api/diceApi'
import './ComparacionPage.css'
import './JaccardPage.css'

const porcentaje = new Intl.NumberFormat('es-CO', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const decimal = new Intl.NumberFormat('es-CO', { minimumFractionDigits: 4, maximumFractionDigits: 6 })

function DicePage() {
  const [articulos, setArticulos] = useState([])
  const [articuloA, setArticuloA] = useState('')
  const [articuloB, setArticuloB] = useState('')
  const [cargando, setCargando] = useState(true)
  const [comparando, setComparando] = useState(false)
  const [errorCarga, setErrorCarga] = useState('')
  const [error, setError] = useState('')
  const [comparacion, setComparacion] = useState(null)
  const [intentoCarga, setIntentoCarga] = useState(0)

  useEffect(() => {
    let vigente = true
    obtenerArticulos().then(datos => {
      if (vigente) setArticulos(datos)
    }).catch(fallo => {
      if (vigente) setErrorCarga(fallo.message || 'No se pudieron cargar los artículos.')
    }).finally(() => {
      if (vigente) setCargando(false)
    })
    return () => { vigente = false }
  }, [intentoCarga])

  function reintentarCarga() {
    setErrorCarga('')
    setCargando(true)
    setIntentoCarga(valor => valor + 1)
  }

  function cambiarSeleccion(valor, actualizar) {
    actualizar(valor)
    setComparacion(null)
    setError('')
  }

  async function comparar(evento) {
    evento.preventDefault()
    if (!articuloA || !articuloB || comparando) return
    setComparando(true)
    setError('')
    setComparacion(null)
    try {
      setComparacion(await obtenerDice(articuloA, articuloB))
    } catch (fallo) {
      setError(fallo.message)
    } finally {
      setComparando(false)
    }
  }

  const resultado = comparacion?.resultado
  const metricas = resultado ? [
    { etiqueta: 'Términos del conjunto A', simbolo: '|A|', valor: resultado.cardinalidadA },
    { etiqueta: 'Términos del conjunto B', simbolo: '|B|', valor: resultado.cardinalidadB },
    { etiqueta: 'Intersección', simbolo: '|A ∩ B|', valor: resultado.cardinalidadInterseccion }
  ] : []

  return (
    <div className="dashboard-minimal-container dashboard-verde comparacion-pagina jaccard-pagina">
      <div className="dashboard-title-row">
        <div className="dashboard-title-group">
          <h1 className="dashboard-main-title">Similitud de Sørensen-Dice</h1>
        </div>
        <p className="dashboard-subtitle">Compara los términos de dos abstracts del corpus.</p>
      </div>

      {cargando && <p role="status">Cargando artículos…</p>}
      {errorCarga && (
        <div className="comparacion-error" role="alert">
          <p>{errorCarga}</p>
          <button className="btn-secundario" onClick={reintentarCarga}>Reintentar carga</button>
        </div>
      )}
      {!cargando && !errorCarga && articulos.length === 0 && (
        <p className="comparacion-aviso">No hay artículos disponibles para comparar.</p>
      )}

      <form className="comparacion-controles jaccard-controles" onSubmit={comparar}>
        {[{ nombre: 'Artículo A', valor: articuloA, actualizar: setArticuloA, id: 'articulo-a' },
          { nombre: 'Artículo B', valor: articuloB, actualizar: setArticuloB, id: 'articulo-b' }].map(campo => (
          <label key={campo.id} htmlFor={campo.id}>
            {campo.nombre}
            <select id={campo.id} value={campo.valor} required
              disabled={cargando || comparando || Boolean(errorCarga)}
              onChange={evento => cambiarSeleccion(evento.target.value, campo.actualizar)}>
              <option value="">Selecciona un artículo</option>
              {articulos.map(articulo => (
                <option key={articulo.id} value={articulo.id}>{articulo.id}. {articulo.titulo}</option>
              ))}
            </select>
          </label>
        ))}
        <button type="submit" className="btn-pill-action btn-pill-action-verde"
          disabled={cargando || comparando || Boolean(errorCarga) || !articuloA || !articuloB}>
          {comparando ? 'Comparando…' : 'Comparar'}
        </button>
      </form>
      <p className="comparacion-ayuda">Selecciona ambos artículos. Puedes comparar un artículo consigo mismo. Cada término se cuenta una sola vez.</p>
      {comparando && <p role="status">Calculando Sørensen-Dice…</p>}
      {error && <p className="comparacion-error" role="alert">{error}</p>}

      {comparacion && (
        <div aria-live="polite">
          <section className="comparacion-tarjeta comparacion-resumen jaccard-resumen" aria-label="Resultado de Sørensen-Dice">
            <h2>Resultado de la comparación</h2>
            <p><strong>Artículo A · {comparacion.articuloA.id}:</strong> {comparacion.articuloA.titulo}</p>
            <p><strong>Artículo B · {comparacion.articuloB.id}:</strong> {comparacion.articuloB.titulo}</p>
            <div className="jaccard-porcentaje">
              <span>Similitud</span>
              <strong>{porcentaje.format(comparacion.porcentaje)} %</strong>
            </div>
            <p>Valor decimal: <strong>{decimal.format(resultado.similitud)}</strong></p>
            <p>Dice(A, B) = 2 × {resultado.cardinalidadInterseccion} / ({resultado.cardinalidadA} + {resultado.cardinalidadB})</p>
            <p className="comparacion-ayuda">Este valor mide coincidencia de términos; no representa un porcentaje de plagio.</p>
          </section>

          <div className="metricas-min-grid jaccard-metricas">
            {metricas.map(metrica => (
              <div key={metrica.simbolo} className="metrica-min-card metrica-min-card-verde">
                <span className="metrica-min-tag metrica-min-tag-verde">{metrica.simbolo}</span>
                <div className="metrica-min-main">
                  <span className="metrica-min-valor">{metrica.valor}</span>
                  <span className="metrica-min-label">{metrica.etiqueta}</span>
                </div>
              </div>
            ))}
          </div>

          <section className="comparacion-tarjeta comparacion-mediciones jaccard-detalles" aria-label="Detalle de términos">
            <h2>Detalle opcional</h2>
            <details>
              <summary>Ver intersección</summary>
              <p>{resultado.interseccion.join(', ') || 'No hay términos compartidos.'}</p>
            </details>
            <details>
              <summary>Ver conjuntos</summary>
              <h3>Conjunto A</h3>
              <p>{resultado.conjuntoA.join(', ') || 'Conjunto vacío.'}</p>
              <h3>Conjunto B</h3>
              <p>{resultado.conjuntoB.join(', ') || 'Conjunto vacío.'}</p>
            </details>
          </section>
        </div>
      )}
    </div>
  )
}

export default DicePage
