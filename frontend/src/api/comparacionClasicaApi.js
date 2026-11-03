export async function obtenerComparacionClasica(articulos) {
  const parametros = new URLSearchParams({ articulos: articulos.join(',') })
  let respuesta
  try {
    respuesta = await fetch(`/api/similitud/clasicos?${parametros}`)
  } catch {
    throw new Error('No se pudo conectar con el servidor. Comprueba que el backend esté en ejecución.')
  }
  let datos
  try {
    datos = await respuesta.json()
  } catch {
    throw new Error(`El servidor no devolvió una respuesta válida (código ${respuesta.status}).`)
  }
  if (!respuesta.ok) throw new Error(datos?.mensaje || 'No se pudo ejecutar la comparación clásica.')
  if (!Array.isArray(datos?.parejas) || datos.parejas.length === 0 ||
      datos.totalParejas !== datos.parejas.length || !Array.isArray(datos.articulosSeleccionados) ||
      !datos.parejas.every(pareja => pareja.articuloA?.titulo && pareja.articuloB?.titulo &&
        ['jaccard', 'dice', 'levenshtein', 'cosenoTfidf'].every(nombre =>
          Number.isFinite(pareja[nombre]?.similitud) && Number.isFinite(pareja[nombre]?.porcentaje)) &&
        Number.isInteger(pareja.levenshtein.distancia))) {
    throw new Error('El servidor no devolvió resultados válidos de la comparación clásica.')
  }
  return datos
}
