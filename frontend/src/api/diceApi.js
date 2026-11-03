export async function obtenerDice(articuloA, articuloB) {
  const parametros = new URLSearchParams({ articuloA, articuloB })
  let respuesta
  try {
    respuesta = await fetch(`/api/dice?${parametros}`)
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
    throw new Error(datos?.mensaje || `No se pudo ejecutar Dice (código ${respuesta.status}).`)
  }
  const resultado = datos?.resultado
  if (!datos?.articuloA?.titulo || !datos?.articuloB?.titulo ||
      !Number.isInteger(resultado?.cardinalidadA) || !Number.isInteger(resultado?.cardinalidadB) ||
      !Number.isInteger(resultado?.cardinalidadInterseccion) || !Number.isFinite(datos?.porcentaje) || !Number.isFinite(resultado?.similitud) ||
      !['conjuntoA', 'conjuntoB', 'interseccion'].every(campo =>
        Array.isArray(resultado?.[campo]) && resultado[campo].every(palabra => typeof palabra === 'string'))) {
    throw new Error('El servidor no devolvió un resultado válido de Dice.')
  }
  return datos
}
