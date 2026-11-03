function MensajeError({ mensaje }) {
  return (
    <section className="error-card-morada">
      <div className="error-icon">⚠️</div>
      <div className="error-body">
        <h4>No se pudo conectar con el servidor backend</h4>
        <p className="error-texto">{mensaje}</p>
        <div className="error-ayuda">
          <strong>Sugerencia:</strong> Asegúrate de que el backend Spring Boot esté ejecutándose en el puerto <code>8080</code> (ej. ejecutando la clase <code>BibliometriaApplication</code>).
        </div>
      </div>
    </section>
  )
}

export default MensajeError
