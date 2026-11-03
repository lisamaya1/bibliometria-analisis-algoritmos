export async function compararBusqueda(articulo, patron, signal) {
  const parametros = new URLSearchParams({ articulo: String(articulo), patron })
  let respuesta
  try {
    respuesta = await fetch(`/api/busqueda/comparar?${parametros}`, { signal })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new Error('No se pudo conectar con el servidor. Comprueba que el backend esté en ejecución.')
  }
  let datos
  try { datos = await respuesta.json() } catch {
    throw new Error(`El servidor no devolvió JSON válido (código ${respuesta.status}).`)
  }
  if (!respuesta.ok) throw new Error(datos?.mensaje || `No se pudo ejecutar la búsqueda (código ${respuesta.status}).`)
  if (!datos?.articulo || !Array.isArray(datos.resultados) || datos.resultados.length !== 3 ||
      !['kmp', 'boyer-moore', 'rabin-karp'].every(nombre =>
        datos.resultados.filter(r => r.algoritmo === nombre).length === 1) ||
      !datos.resultados.every(r => Array.isArray(r.posiciones) && Number.isInteger(r.ocurrencias) &&
        Number.isInteger(r.longitudTexto) && Number.isInteger(r.longitudPatron) && typeof r.patron === 'string')) {
    throw new Error('El servidor no devolvió una comparación de búsqueda válida.')
  }
  return datos
}
