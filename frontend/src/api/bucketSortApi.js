/**
 * api/bucketSortApi.js
 *
 * Módulo de comunicación con el backend Spring Boot.
 * Todo dato proviene exclusivamente de los endpoints REST del servidor.
 * No contiene lógica de ejecución ni ordenamiento local en el frontend.
 */

const ENDPOINT_BUCKET_SORT = '/api/bucket-sort'
const ENDPOINT_ARTICULOS = '/api/articulos'

/**
 * Consulta al backend la lista de artículos científicos preprocesados.
 *
 * @returns {Promise<Array>} Lista de artículos retornada por GET /api/articulos
 */
export async function obtenerArticulos() {
  const respuesta = await fetch(ENDPOINT_ARTICULOS)

  if (!respuesta.ok) {
    throw new Error(
      `No se pudo obtener los artículos del backend (código ${respuesta.status}: ${respuesta.statusText}). Verifica que el servidor Spring Boot esté ejecutándose en http://localhost:8080.`
    )
  }

  const datos = await respuesta.json()
  if (!Array.isArray(datos)) {
    throw new Error('El backend no retornó una lista válida de artículos.')
  }

  return datos
}

/**
 * Solicita al backend la ejecución del algoritmo Bucket Sort sobre el corpus.
 *
 * @param {number} topK - Cantidad de términos más frecuentes a retornar.
 * @param {boolean} filtrarStopwords - Si el backend debe excluir palabras vacías.
 * @returns {Promise<Object>} Resultado analítico y ranking generado por el backend.
 */
export async function obtenerResultadoBucketSort(topK, filtrarStopwords) {
  const url = `${ENDPOINT_BUCKET_SORT}?topK=${topK}&filtrarStopwords=${filtrarStopwords}`
  const respuesta = await fetch(url)

  if (!respuesta.ok) {
    throw new Error(
      `Error al ejecutar Bucket Sort en el servidor (código ${respuesta.status}: ${respuesta.statusText}). Verifica que el backend Spring Boot esté activo.`
    )
  }

  const json = await respuesta.json()
  if (!json || !Array.isArray(json.terminos)) {
    throw new Error('El backend no retornó una estructura válida para Bucket Sort.')
  }

  return json
}
