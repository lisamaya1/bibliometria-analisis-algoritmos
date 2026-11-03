import { construirArbol } from '../utils/dendrograma'

export const criteriosClustering = {
  single: 'Single Linkage', complete: 'Complete Linkage', average: 'Average Linkage'
}

async function consultar(ruta, signal) {
  let respuesta
  try {
    respuesta = await fetch(`/api/clustering/${ruta}`, { signal })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new Error('No se pudo conectar con el servidor. Comprueba que el backend esté en ejecución.')
  }
  let datos
  try { datos = await respuesta.json() } catch {
    throw new Error(`El servidor no devolvió JSON válido (código ${respuesta.status}).`)
  }
  if (!respuesta.ok) throw new Error(datos?.mensaje || `No se pudo cargar clustering (código ${respuesta.status}).`)
  return datos
}

export async function obtenerDendrograma(criterio, signal) {
  if (!criteriosClustering[criterio]) throw new Error('Selecciona un criterio válido.')
  const datos = await consultar(criterio, signal)
  if (datos.criterio !== criterio) throw new Error('El servidor devolvió otro criterio de agrupamiento.')
  construirArbol(datos)
  return datos
}

export async function obtenerEvaluacionClustering(signal) {
  const datos = await consultar('evaluacion', signal)
  if (!Array.isArray(datos?.criterios) || datos.criterios.length !== 3 ||
      !Number.isInteger(datos.cantidadArticulos) || !Number.isFinite(datos.maximoSilhouette) ||
      !Array.isArray(datos.criteriosMasCoherentes) || !datos.criteriosMasCoherentes.length ||
      !datos.criteriosMasCoherentes.every(c => criteriosClustering[c]) ||
      new Set(datos.criterios.map(c => c.criterio)).size !== 3 ||
      !datos.criterios.every(c => criteriosClustering[c.criterio] &&
        Number.isInteger(c.mejor?.k) && c.mejor.k >= 2 && c.mejor.k < datos.cantidadArticulos &&
        Number.isFinite(c.mejor?.silhouette) && Math.abs(c.mejor.silhouette) <= 1 &&
        Array.isArray(c.mejor?.clusters) && c.mejor.clusters.length === c.mejor.k &&
        c.mejor.clusters.every(g => Array.isArray(g.articulos) && g.tamano === g.articulos.length))) {
    throw new Error('El servidor no devolvió una evaluación de Silhouette válida.')
  }
  return datos
}
