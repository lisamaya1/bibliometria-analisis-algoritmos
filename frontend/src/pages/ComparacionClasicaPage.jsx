import { useEffect, useState } from 'react'
import { obtenerArticulos } from '../api/bucketSortApi'
import { obtenerComparacionClasica } from '../api/comparacionClasicaApi'
import './ComparacionPage.css'
import './ComparacionClasicaPage.css'

const porcentaje = new Intl.NumberFormat('es-CO', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const decimal = new Intl.NumberFormat('es-CO', { minimumFractionDigits: 4, maximumFractionDigits: 6 })

function ResultadoSimilitud({ resultado }) {
  return <><strong>{porcentaje.format(resultado.porcentaje)} %</strong><span className="clasica-decimal">{decimal.format(resultado.similitud)}</span></>
}

function ComparacionClasicaPage() {
  const [articulos, setArticulos] = useState([])
  const [seleccion, setSeleccion] = useState([])
  const [cargando, setCargando] = useState(true)
  const [comparando, setComparando] = useState(false)
  const [errorCarga, setErrorCarga] = useState('')
  const [error, setError] = useState('')
  const [resultado, setResultado] = useState(null)
  const [intento, setIntento] = useState(0)

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
  }, [intento])

  function cambiarSeleccion(nueva) {
    setSeleccion(nueva)
    setResultado(null)
    setError('')
  }

  async function comparar(evento) {
    evento.preventDefault()
    if (seleccion.length < 2 || comparando) return
    setComparando(true)
    setResultado(null)
    setError('')
    try {
      setResultado(await obtenerComparacionClasica(seleccion))
    } catch (fallo) {
      setError(fallo.message)
    } finally {
      setComparando(false)
    }
  }

  const ocupado = cargando || comparando || Boolean(errorCarga)
  const cantidadParejas = seleccion.length * (seleccion.length - 1) / 2

  return (
    <div className="dashboard-minimal-container dashboard-verde comparacion-pagina clasica-pagina">
      <div className="dashboard-title-row">
        <div className="dashboard-title-group"><h1 className="dashboard-main-title">Comparación clásica</h1></div>
        <p className="dashboard-subtitle">Selecciona dos o más artículos y compara todas sus parejas con los cuatro algoritmos clásicos.</p>
      </div>

      <section className="comparacion-tarjeta clasica-explicacion" aria-label="Cómo interpretar los resultados">
        <p><strong>Los algoritmos miden aspectos distintos de la similitud. Un porcentaje más alto no significa automáticamente que un algoritmo sea mejor.</strong></p>
        <ul>
          <li><strong>Jaccard:</strong> coincidencia respecto a la unión de términos únicos.</li>
          <li><strong>Dice:</strong> doble de la coincidencia respecto a la suma de tamaños de los conjuntos.</li>
          <li><strong>Levenshtein:</strong> cercanía textual mediante operaciones de edición.</li>
          <li><strong>Coseno TF-IDF:</strong> similitud vectorial ponderada por la importancia de los términos en todo el corpus.</li>
        </ul>
      </section>

      {cargando && <p role="status">Cargando artículos…</p>}
      {errorCarga && <div className="comparacion-error" role="alert">
        <p>{errorCarga}</p>
        <button className="btn-secundario" onClick={() => { setErrorCarga(''); setCargando(true); setIntento(valor => valor + 1) }}>Reintentar carga</button>
      </div>}
      {!cargando && !errorCarga && articulos.length === 0 && <p className="comparacion-aviso">No hay artículos disponibles.</p>}

      <form className="comparacion-tarjeta" onSubmit={comparar}>
        <div className="clasica-acciones">
          <h2>Artículos del corpus</h2>
          <button type="button" className="btn-secundario" disabled={ocupado || articulos.length === 0}
            onClick={() => cambiarSeleccion(articulos.map(articulo => articulo.id))}>Seleccionar todos</button>
          <button type="button" className="btn-secundario" disabled={ocupado || seleccion.length === 0}
            onClick={() => cambiarSeleccion([])}>Limpiar selección</button>
        </div>
        <fieldset className="clasica-articulos" disabled={ocupado}>
          <legend>Selecciona al menos dos artículos</legend>
          <div className="clasica-lista">
            {articulos.map(articulo => <label key={articulo.id} className={seleccion.includes(articulo.id) ? 'clasica-elegido' : ''}>
              <input type="checkbox" checked={seleccion.includes(articulo.id)}
                onChange={evento => cambiarSeleccion(evento.target.checked
                  ? [...seleccion, articulo.id] : seleccion.filter(id => id !== articulo.id))} />
              <span><strong>{articulo.id}.</strong> {articulo.titulo}</span>
            </label>)}
          </div>
        </fieldset>
        <p className="comparacion-ayuda clasica-seleccion" aria-live="polite">
          <strong>{seleccion.length} artículos seleccionados · {cantidadParejas} parejas.</strong>
          {seleccion.length > 0 && <span> IDs: {articulos.filter(articulo => seleccion.includes(articulo.id)).map(articulo => articulo.id).join(', ')}.</span>}
          {seleccion.length < 2 && <span> Selecciona al menos dos artículos para comparar.</span>}
        </p>
        <button type="submit" className="btn-pill-action btn-pill-action-verde" disabled={ocupado || seleccion.length < 2}>
          {comparando ? 'Comparando…' : 'Comparar seleccionados'}
        </button>
      </form>

      {comparando && <p role="status">Calculando las {cantidadParejas} parejas. Espera a que termine la comparación.</p>}
      {error && <p className="comparacion-error" role="alert">{error}</p>}
      {resultado && <section className="comparacion-tarjeta" aria-label="Resultados de la comparación clásica" aria-live="polite">
        <h2>Resultados por pareja</h2>
        <p className="comparacion-ayuda">{resultado.articulosSeleccionados.length} artículos · {resultado.totalParejas} parejas sin repetición. TF-IDF utiliza {resultado.totalDocumentosIdf} documentos para IDF. Cada celda muestra porcentaje y valor decimal.</p>
        <div className="comparacion-tabla-scroll" tabIndex={0} role="region" aria-label="Tabla de resultados, desplazamiento horizontal disponible">
          <table className="comparacion-tabla clasica-tabla">
            <caption>Similitud de los cuatro algoritmos para cada pareja seleccionada</caption>
            <thead><tr><th scope="col">Pareja de artículos</th><th scope="col">Jaccard</th><th scope="col">Sørensen-Dice</th><th scope="col">Levenshtein</th><th scope="col">Coseno TF-IDF</th></tr></thead>
            <tbody>{resultado.parejas.map(pareja => <tr key={`${pareja.articuloA.id}-${pareja.articuloB.id}`}>
              <th scope="row"><strong>{pareja.articuloA.id} vs {pareja.articuloB.id}</strong>
                <span className="clasica-titulo">{pareja.articuloA.id}. {pareja.articuloA.titulo}</span>
                <span className="clasica-titulo">{pareja.articuloB.id}. {pareja.articuloB.titulo}</span></th>
              <td><ResultadoSimilitud resultado={pareja.jaccard} /></td>
              <td><ResultadoSimilitud resultado={pareja.dice} /></td>
              <td><ResultadoSimilitud resultado={pareja.levenshtein} /><span className="clasica-distancia">Distancia: {pareja.levenshtein.distancia}</span></td>
              <td><ResultadoSimilitud resultado={pareja.cosenoTfidf} /></td>
            </tr>)}</tbody>
          </table>
        </div>
        <p className="comparacion-ayuda">En pantallas pequeñas, desplaza la tabla horizontalmente para ver los cuatro algoritmos. Los detalles siguen disponibles en sus pantallas individuales. El caso académico es 15 vs 19.</p>
      </section>}
    </div>
  )
}

export default ComparacionClasicaPage
