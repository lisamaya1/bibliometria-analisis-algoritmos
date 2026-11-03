export async function obtenerComparacion(topK, filtrarStopwords) {
  const parametros = new URLSearchParams({ topK, filtrarStopwords })
  let respuesta
  try {
    respuesta = await fetch(`/api/comparacion-ordenamientos?${parametros}`)
  } catch {
    throw new Error('No se pudo conectar con el servidor. Comprueba que el backend esté en ejecución.')
  }
  const datos = await respuesta.json()
  if (!respuesta.ok) {
    throw new Error(datos.mensaje || 'No se pudo ejecutar la comparación.')
  }
  if (!Array.isArray(datos.algoritmos) || datos.algoritmos.length === 0) {
    throw new Error('El servidor no devolvió resultados de la comparación.')
  }
  return datos
}
