// Interpreta el árbol recibido. No calcula distancias ni decide fusiones.
export function construirArbol(datos) {
  if (!datos || !Array.isArray(datos.articulos) || !Array.isArray(datos.fusiones) ||
      datos.cantidadArticulos !== datos.articulos.length || datos.articulos.length < 2 ||
      datos.fusiones.length !== datos.articulos.length - 1) throw new Error('El historial del dendrograma está incompleto.')
  const nodos = new Map()
  const activos = new Set()
  for (const articulo of datos.articulos) {
    if (!Number.isInteger(articulo.id) || !articulo.titulo || nodos.has(articulo.id)) throw new Error('Las hojas del dendrograma no son válidas.')
    nodos.set(articulo.id, { ...articulo, distancia: 0, articulos: [articulo.id], tamano: 1 })
    activos.add(articulo.id)
  }
  for (const fusion of datos.fusiones) {
    const izquierdo = nodos.get(fusion.izquierdo)
    const derecho = nodos.get(fusion.derecho)
    if (!izquierdo || !derecho || fusion.izquierdo === fusion.derecho || nodos.has(fusion.id) ||
        !activos.has(fusion.izquierdo) || !activos.has(fusion.derecho) ||
        !Number.isFinite(fusion.distancia) || fusion.distancia < 0 || fusion.distancia > 1 ||
        fusion.distancia < Math.max(izquierdo.distancia, derecho.distancia) - 1e-12 ||
        !Array.isArray(fusion.articulos) || fusion.tamano !== izquierdo.tamano + derecho.tamano ||
        fusion.articulos.length !== fusion.tamano || new Set(fusion.articulos).size !== fusion.tamano ||
        ![...izquierdo.articulos, ...derecho.articulos].every(id => fusion.articulos.includes(id))) {
      throw new Error('El backend devolvió una fusión inválida para el dendrograma.')
    }
    nodos.set(fusion.id, { ...fusion, izquierdo, derecho })
    activos.delete(fusion.izquierdo)
    activos.delete(fusion.derecho)
    activos.add(fusion.id)
  }
  if (activos.size !== 1 || !activos.has(datos.raiz)) throw new Error('La raíz del dendrograma no corresponde al historial.')
  return nodos.get(datos.raiz)
}

export function posicionarArbol(raiz) {
  const hojas = []
  const uniones = []
  function visitar(nodo) {
    if (!nodo.izquierdo) {
      const hoja = { ...nodo, y: 70 + hojas.length * 34 }
      hojas.push(hoja)
      return hoja
    }
    const izquierdo = visitar(nodo.izquierdo)
    const derecho = visitar(nodo.derecho)
    const union = { ...nodo, izquierdo, derecho, y: (izquierdo.y + derecho.y) / 2 }
    uniones.push(union)
    return union
  }
  visitar(raiz)
  return { hojas, uniones, alto: 110 + hojas.length * 34 }
}

export function distanciaCorte(historial, cantidadArticulos, k) {
  const incluidas = cantidadArticulos - k
  if (incluidas <= 0 || incluidas >= historial.length) return null
  const anterior = historial[incluidas - 1].distancia
  const siguiente = historial[incluidas].distancia
  // Con alturas empatadas un corte horizontal no puede expresar todos los k.
  return siguiente > anterior ? (anterior + siguiente) / 2 : null
}

export function mismaParticion(a, b) {
  const clave = grupos => grupos.map(g => [...g.articulos].sort((x, y) => x - y).join(',')).sort().join('|')
  return clave(a) === clave(b)
}
