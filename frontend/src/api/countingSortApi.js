/**
 * api/countingSortApi.js
 *
 * Módulo para interactuar con el endpoint de Counting Sort del backend Spring Boot.
 * No contiene lógica de ejecución local en el frontend.
 */

const ENDPOINT_COUNTING_SORT = '/api/counting-sort'

/**
 * Consulta el endpoint /api/counting-sort del backend Spring Boot.
 *
 * @param {number}  topK              - Cantidad de términos a incluir en el ranking.
 * @param {boolean} filtrarStopwords  - Exclusión de palabras funcionales.
 * @returns {Promise<Object>}         - Resultado formal retornado por el backend.
 */
export async function obtenerResultadoCountingSort(topK, filtrarStopwords) {
  const url = `${ENDPOINT_COUNTING_SORT}?topK=${topK}&filtrarStopwords=${filtrarStopwords}`
  const respuesta = await fetch(url)

  if (!respuesta.ok) {
    throw new Error(
      `Error al consultar Counting Sort en el servidor (${respuesta.status}: ${respuesta.statusText}). Verifica que el backend Spring Boot esté activo.`
    )
  }

  const json = await respuesta.json()
  if (!json || !Array.isArray(json.terminos)) {
    throw new Error('El backend no retornó una estructura válida para Counting Sort.')
  }

  return json
}
