export async function obtenerCosenoTfidf(articuloA, articuloB) {
  const parametros = new URLSearchParams({ articuloA, articuloB })
  let respuesta
  try {
    respuesta = await fetch(`/api/coseno-tfidf?${parametros}`)
  } catch {
    throw new Error('No se pudo conectar con el servidor. Comprueba que el backend esté en ejecución.')
  }
  let datos
  try {
    datos = await respuesta.json()
  } catch {
    throw new Error(`El servidor no devolvió una respuesta válida (código ${respuesta.status}).`)
  }
  if (!respuesta.ok) {
    throw new Error(datos?.mensaje || `No se pudo ejecutar Coseno TF-IDF (código ${respuesta.status}).`)
  }
  const resultado = datos?.resultado
  if (!datos?.articuloA?.titulo || !datos?.articuloB?.titulo ||
      !Number.isFinite(datos?.porcentaje) ||
      !['productoPunto', 'normaA', 'normaB', 'similitud'].every(campo => Number.isFinite(resultado?.[campo])) ||
      !Number.isInteger(resultado?.tamanoVocabulario) || !Number.isInteger(resultado?.totalDocumentos)) {
    throw new Error('El servidor no devolvió un resultado válido de Coseno TF-IDF.')
  }
  return datos
}
