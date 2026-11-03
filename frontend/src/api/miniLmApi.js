export async function obtenerMiniLm(articuloA, articuloB) {
  const parametros = new URLSearchParams({ articuloA, articuloB })
  let respuesta
  try {
    respuesta = await fetch(`/api/ia/minilm?${parametros}`)
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
    throw new Error(datos?.mensaje || `No se pudo ejecutar MiniLM (código ${respuesta.status}).`)
  }
  const resultado = datos?.resultado
  if (!datos?.articuloA?.titulo || !datos?.articuloB?.titulo ||
      !Number.isFinite(datos?.porcentaje) ||
      !['productoPunto', 'normaA', 'normaB', 'similitud'].every(campo => Number.isFinite(resultado?.[campo])) ||
      !Number.isInteger(datos?.dimension) || !datos?.modelo ||
      ![datos.articuloA, datos.articuloB].every(articulo => Number.isInteger(articulo.tokensContenido) &&
        Number.isInteger(articulo.fragmentos) && Array.isArray(articulo.tokensPorFragmento))) {
    throw new Error('El servidor no devolvió un resultado válido de MiniLM.')
  }
  return datos
}
