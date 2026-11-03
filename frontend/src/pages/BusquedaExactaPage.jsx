import { useEffect, useRef, useState } from 'react'
import { obtenerArticulos } from '../api/bucketSortApi'
import { compararBusqueda } from '../api/busquedaApi'
import './ComparacionPage.css'
import './BusquedaExactaPage.css'

const numero = new Intl.NumberFormat('es-CO')
const ejemplos = ['chatgpt', 'november 2022', 'quantum entanglement']
const algoritmos = [
  { id: 'kmp', nombre: 'KMP', explicacion: 'Reutiliza información de prefijos mediante LPS y evita retroceder en el texto.' },
  { id: 'boyer-moore', nombre: 'Boyer-Moore', explicacion: 'Compara desde la derecha y utiliza Bad Character para realizar saltos.' },
  { id: 'rabin-karp', nombre: 'Rabin-Karp', explicacion: 'Compara hashes de ventanas y solo verifica caracteres cuando los hashes coinciden.' }
]

function Metrica({ nombre, valor, unidad = '' }) {
  return <div><dt>{nombre}</dt><dd>{Number.isFinite(valor) ? `${numero.format(valor)}${unidad}` : 'No disponible'}</dd></div>
}

function TarjetaAlgoritmo({ algoritmo, resultado, actualizaciones }) {
  const hash = resultado.estadisticasHash
  return <article className="comparacion-tarjeta busqueda-algoritmo" aria-labelledby={`busqueda-${algoritmo.id}`}>
    <h3 id={`busqueda-${algoritmo.id}`}>{algoritmo.nombre}</h3>
    <p className="comparacion-ayuda">{algoritmo.explicacion}</p>
    <dl className="busqueda-metricas">
      <Metrica nombre="Ocurrencias" valor={resultado.ocurrencias} />
      <Metrica nombre={algoritmo.id === 'rabin-karp' ? 'Comparaciones de caracteres' : 'Comparaciones de búsqueda'} valor={resultado.comparaciones} />
      {algoritmo.id === 'kmp' && <Metrica nombre="Comparaciones de preparación LPS" valor={resultado.comparacionesPreparacion} />}
      {algoritmo.id === 'boyer-moore' && <Metrica nombre="Actualizaciones Bad Character" valor={actualizaciones} />}
      {algoritmo.id === 'rabin-karp' && <>
        <Metrica nombre="Ventanas procesadas" valor={hash?.ventanasProcesadas} />
        <Metrica nombre="Coincidencias de hash" valor={hash?.coincidenciasHash} />
        <Metrica nombre="Colisiones" valor={hash?.colisiones} />
        <Metrica nombre="Actualizaciones rolling" valor={hash?.actualizacionesRolling} />
      </>}
    </dl>
    <div className="busqueda-tiempos">
      <h4>Tiempo observado en esta ejecución</h4>
      <dl className="busqueda-metricas">
        <Metrica nombre="Preparación" valor={resultado.tiempoPreparacionNs} unidad=" ns" />
        <Metrica nombre="Búsqueda" valor={resultado.tiempoBusquedaNs} unidad=" ns" />
      </dl>
    </div>
  </article>
}

export default function BusquedaExactaPage() {
  const [articulos, setArticulos] = useState([])
  const [articulo, setArticulo] = useState('')
  const [patron, setPatron] = useState('')
  const [cargando, setCargando] = useState(true)
  const [buscando, setBuscando] = useState(false)
  const [errorCarga, setErrorCarga] = useState('')
  const [error, setError] = useState('')
  const [datos, setDatos] = useState(null)
  const [intento, setIntento] = useState(0)
  const solicitud = useRef(null)

  useEffect(() => {
    let vigente = true
    obtenerArticulos().then(lista => { if (vigente) setArticulos(lista) })
      .catch(fallo => { if (vigente) setErrorCarga(fallo.message) })
      .finally(() => { if (vigente) setCargando(false) })
    return () => { vigente = false }
  }, [intento])
  useEffect(() => () => solicitud.current?.abort(), [])

  function cambiar(valor, actualizar) {
    actualizar(valor)
    setDatos(null)
    setError('')
  }

  async function buscar(id, textoPatron) {
    if (buscando || cargando || errorCarga) return
    if (!id || textoPatron.length === 0) {
      setError('Selecciona un artículo y escribe un patrón no vacío.')
      return
    }
    const control = new AbortController()
    solicitud.current = control
    setBuscando(true)
    setError('')
    setDatos(null)
    try {
      const respuesta = await compararBusqueda(id, textoPatron, control.signal)
      if (!control.signal.aborted) setDatos(respuesta)
    } catch (fallo) {
      if (!control.signal.aborted) setError(fallo.message)
    } finally {
      if (!control.signal.aborted) setBuscando(false)
    }
  }

  function probarEjemplo(textoPatron) {
    setArticulo('19')
    setPatron(textoPatron)
    buscar('19', textoPatron)
  }

  const bloqueado = cargando || buscando || Boolean(errorCarga) || articulos.length === 0
  const comun = datos?.resultados.find(r => r.algoritmo === 'kmp')

  return <div className="dashboard-minimal-container dashboard-verde comparacion-pagina busqueda-pagina">
    <div className="dashboard-title-row">
      <div className="dashboard-title-group"><h1 className="dashboard-main-title">Búsqueda exacta</h1></div>
      <p className="dashboard-subtitle">Busca un patrón en un abstract procesado y compara KMP, Boyer-Moore y Rabin-Karp.</p>
    </div>
    {cargando && <p role="status">Cargando artículos…</p>}
    {errorCarga && <div className="comparacion-error" role="alert">
      <p>{errorCarga}</p>
      <button className="btn-secundario" onClick={() => {
        setErrorCarga(''); setCargando(true); setIntento(v => v + 1)
      }}>Reintentar carga</button>
    </div>}
    {!cargando && !errorCarga && articulos.length === 0 && <p className="comparacion-aviso">No hay artículos disponibles.</p>}

    <form className="comparacion-controles busqueda-controles" onSubmit={e => { e.preventDefault(); buscar(articulo, patron) }}>
      <label htmlFor="busqueda-articulo"><span id="busqueda-articulo-etiqueta">Artículo</span>
        <select id="busqueda-articulo" aria-labelledby="busqueda-articulo-etiqueta" value={articulo} required disabled={bloqueado}
          onChange={e => cambiar(e.target.value, setArticulo)}>
          <option value="">Selecciona un artículo</option>
          {articulos.map(a => <option key={a.id} value={a.id}>Artículo {a.id} — {a.titulo}</option>)}
        </select>
      </label>
      <label htmlFor="busqueda-patron">Patrón
        <input id="busqueda-patron" type="text" value={patron} required disabled={bloqueado}
          autoComplete="off" autoCapitalize="off" autoCorrect="off" spellCheck={false}
          placeholder="Ejemplo: chatgpt" aria-describedby="busqueda-condiciones"
          onChange={e => cambiar(e.target.value, setPatron)} />
      </label>
      <button type="submit" className="btn-pill-action btn-pill-action-verde"
        disabled={bloqueado || !articulo || patron.length === 0}>{buscando ? 'Buscando…' : 'Buscar y comparar'}</button>
    </form>
    <p id="busqueda-condiciones" className="comparacion-ayuda">La búsqueda es exacta y sensible a mayúsculas, espacios y puntuación. El patrón se envía tal como lo escribes. Se utiliza el abstract procesado.</p>

    {articulos.some(a => a.id === 19) && <div className="busqueda-ejemplos" aria-label="Casos sugeridos">
      <span>Probar artículo 19:</span>
      {ejemplos.map(ejemplo => <button type="button" className="btn-secundario" key={ejemplo}
        disabled={bloqueado} onClick={() => probarEjemplo(ejemplo)}>{ejemplo}</button>)}
    </div>}
    {buscando && <p role="status">Ejecutando los tres algoritmos sobre la misma entrada…</p>}
    {error && <p className="comparacion-error" role="alert">{error}</p>}

    {datos && comun && <div aria-live="polite">
      <section className="comparacion-tarjeta comparacion-resumen busqueda-resumen" aria-label="Coincidencias encontradas">
        <h2>Coincidencias encontradas</h2>
        <p><strong>Artículo {datos.articulo.id}:</strong> {datos.articulo.titulo}</p>
        <p>Patrón: <code className="busqueda-patron-visible">{comun.patron}</code></p>
        <div className="comparacion-datos">
          <span>Longitud del abstract: <strong>{numero.format(comun.longitudTexto)}</strong></span>
          <span>Longitud del patrón: <strong>{numero.format(comun.longitudPatron)}</strong></span>
        </div>
        <p className="busqueda-ocurrencias"><strong>{comun.ocurrencias} {comun.ocurrencias === 1 ? 'ocurrencia' : 'ocurrencias'}</strong></p>
        {comun.ocurrencias === 0 ? <p>No se encontraron coincidencias.</p> : <>
          <p><strong>Posiciones:</strong></p>
          <ul className="busqueda-posiciones" aria-label="Posiciones encontradas">
            {comun.posiciones.map(posicion => <li key={posicion}>{posicion}</li>)}
          </ul>
        </>}
        <p className="comparacion-ayuda">Posiciones base 0, medidas en puntos de código Unicode. Los tres algoritmos devuelven las mismas posiciones.</p>
      </section>
      <h2 className="comparacion-titulo-resultados">Comparación de algoritmos</h2>
      <p className="comparacion-aviso">Las métricas internas de los tres algoritmos no son directamente equivalentes. Rabin-Karp realiza trabajo de hashing que no aparece como comparación de caracteres.</p>
      <p className="comparacion-ayuda">Los tiempos corresponden a una sola ejecución, no a un benchmark definitivo. No incluyen lectura del JSON ni HTTP. La medición con calentamiento y repeticiones permanece en la documentación interna.</p>
      <div className="busqueda-tarjetas">
        {algoritmos.map(algoritmo => <TarjetaAlgoritmo key={algoritmo.id} algoritmo={algoritmo}
          resultado={datos.resultados.find(r => r.algoritmo === algoritmo.id)}
          actualizaciones={datos.actualizacionesTablaBadCharacter} />)}
      </div>
    </div>}
  </div>
}
