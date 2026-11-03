import { useMemo, useState } from 'react'
import { construirArbol, distanciaCorte, posicionarArbol } from '../utils/dendrograma'

const decimal = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 9 })
const inicio = 340
const anchoEscala = 1000
const x = distancia => inicio + distancia * anchoEscala

export default function Dendrograma({ datos, nombre, mejor }) {
  const dibujo = useMemo(() => posicionarArbol(construirArbol(datos)), [datos])
  const [seleccionada, setSeleccionada] = useState(datos.fusiones[0].id)
  const [ampliado, setAmpliado] = useState(false)
  const fusion = datos.fusiones.find(f => f.id === seleccionada)
  const corte = mejor ? distanciaCorte(datos.fusiones, datos.cantidadArticulos, mejor.k) : null
  const marcas = Array.from({ length: 11 }, (_, i) => i / 10)

  return <>
    <div className="clustering-grafico-cabecera">
      <h2>Dendrograma · {nombre}</h2>
      <button className="btn-secundario" type="button" aria-pressed={ampliado} onClick={() => setAmpliado(v => !v)}>
        {ampliado ? 'Tamaño normal' : 'Ampliar dendrograma'}
      </button>
    </div>
    <p className="comparacion-ayuda">Escala lineal de 0 a 1: cada unión se sitúa en su distancia real de fusión. Selecciona una unión para ver su distancia exacta. Puedes desplazar el gráfico horizontalmente.</p>
    <div className="clustering-grafico-scroll" tabIndex="0" role="region" aria-label="Gráfico desplazable del dendrograma">
      <svg className={`clustering-svg ${ampliado ? 'clustering-svg-ampliado' : ''}`} viewBox={`0 0 1400 ${dibujo.alto}`}
        role="group" aria-label={`Dendrograma de ${nombre}: ${dibujo.hojas.length} artículos y ${dibujo.uniones.length} fusiones`}>
        <text x={inicio} y="18" className="clustering-eje-titulo">Distancia de fusión · 1 − coseno TF-IDF</text>
        {marcas.map(valor => <g key={valor}>
          <line x1={x(valor)} x2={x(valor)} y1="46" y2={dibujo.alto - 35} className="clustering-cuadricula" />
          <text x={x(valor)} y="39" textAnchor="middle" className="clustering-marca">{decimal.format(valor)}</text>
        </g>)}
        {dibujo.uniones.map(nodo => <g key={nodo.id} className={`clustering-union ${nodo.id === seleccionada ? 'clustering-union-activa' : ''}`}
          data-fusion={nodo.id} data-distancia={nodo.distancia} data-x={x(nodo.distancia)}
          role="button" tabIndex="0" aria-label={`Fusión ${nodo.id}, distancia ${decimal.format(nodo.distancia)}, ${nodo.tamano} artículos`}
          aria-pressed={nodo.id === seleccionada} onClick={() => setSeleccionada(nodo.id)}
          onKeyDown={evento => { if (evento.key === 'Enter' || evento.key === ' ') { evento.preventDefault(); setSeleccionada(nodo.id) } }}>
          <title>{`Fusión ${nodo.id}: distancia ${decimal.format(nodo.distancia)}; artículos ${nodo.articulos.join(', ')}`}</title>
          <path d={`M ${x(nodo.izquierdo.distancia)} ${nodo.izquierdo.y} H ${x(nodo.distancia)} V ${nodo.derecho.y} H ${x(nodo.derecho.distancia)}`} />
          <circle cx={x(nodo.distancia)} cy={nodo.y} r="4" />
        </g>)}
        {corte !== null && <g className="clustering-corte" data-corte={corte}>
          <line x1={x(corte)} x2={x(corte)} y1="46" y2={dibujo.alto - 35} />
          <text x={x(corte)} y={dibujo.alto - 12} textAnchor="middle">Corte k={mejor.k}</text>
        </g>}
        {dibujo.hojas.map(hoja => <text key={hoja.id} data-articulo={hoja.id} x="12" y={hoja.y + 4} className="clustering-hoja">
          <title>{`Artículo ${hoja.id}: ${hoja.titulo}`}</title>
          <tspan className="clustering-hoja-id">Artículo {hoja.id}</tspan>
          <tspan>{` · ${hoja.titulo.length > 29 ? hoja.titulo.slice(0, 29) + '…' : hoja.titulo}`}</tspan>
        </text>)}
      </svg>
    </div>
    {mejor && <p className="comparacion-ayuda clustering-corte-leyenda">
      <span aria-hidden="true">┄</span> Corte recomendado: <strong>k={mejor.k}</strong>.
      {corte !== null ? ` La línea se ubica entre dos alturas de fusión, a distancia ${decimal.format(corte)}.` : ' No se dibuja línea horizontal porque las alturas empatadas no permiten representar ese k mediante una sola distancia.'}
    </p>}
    {fusion && <div className="clustering-fusion-detalle" aria-live="polite">
      <strong>Fusión {fusion.id}</strong> · Hijos {fusion.izquierdo} y {fusion.derecho} · Distancia: <strong>{decimal.format(fusion.distancia)}</strong> · Tamaño: {fusion.tamano}.
      <p>Artículos: {fusion.articulos.join(', ')}.</p>
    </div>}
    <details className="comparacion-mediciones">
      <summary>Ver las {datos.fusiones.length} fusiones y sus distancias</summary>
      <div className="comparacion-tabla-scroll">
        <table className="comparacion-tabla"><thead><tr><th>Fusión</th><th>Hijos</th><th>Distancia</th><th>Tamaño</th></tr></thead>
          <tbody>{datos.fusiones.map(f => <tr key={f.id}><th scope="row">{f.id}</th><td>{f.izquierdo} + {f.derecho}</td><td>{decimal.format(f.distancia)}</td><td>{f.tamano}</td></tr>)}</tbody>
        </table>
      </div>
    </details>
  </>
}
