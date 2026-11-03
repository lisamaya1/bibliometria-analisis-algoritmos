export async function obtenerBge(articuloA, articuloB) {
  const parametros = new URLSearchParams({ articuloA, articuloB })
  let respuesta
  try {
    respuesta = await fetch(`/api/ia/bge?${parametros}`)
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
    throw new Error(datos?.mensaje || `No se pudo ejecutar BGE (código ${respuesta.status}).`)
  }
  const resultado = datos?.resultado
  if (!datos?.articuloA?.titulo || !datos?.articuloB?.titulo ||
      !['sumaCuadrados', 'normaA', 'normaB', 'distancia'].every(campo => Number.isFinite(resultado?.[campo])) ||
      !Number.isInteger(datos?.dimension) || !datos?.modelo ||
      ![datos.articuloA, datos.articuloB].every(articulo => Number.isInteger(articulo.tokensContenido) &&
        Number.isInteger(articulo.fragmentos) && Array.isArray(articulo.tokensPorFragmento))) {
    throw new Error('El servidor no devolvió un resultado válido de BGE.')
  }
  return datos
}
