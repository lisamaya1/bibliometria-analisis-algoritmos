package com.uniquindio.bibliometria.modelo;

import java.util.Objects;


public final class TerminoFrecuencia implements Comparable<TerminoFrecuencia> {
    private final String termino;
    private final int frecuencia;

    public TerminoFrecuencia(String termino, int frecuencia) {
        this.termino = Objects.requireNonNull(termino, "termino no puede ser nulo");
        this.frecuencia = frecuencia;
    }

    public String getTermino() {
        return termino;
    }

    public int getFrecuencia() {
        return frecuencia;
    }

    @Override
    public int compareTo(TerminoFrecuencia otro) {
        // Orden natural: por frecuencia descendente (mayor a menor)
        int comp = Integer.compare(otro.frecuencia, this.frecuencia);
        if (comp != 0) {
            return comp;
        }
        return this.termino.compareTo(otro.termino);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TerminoFrecuencia that = (TerminoFrecuencia) o;
        return frecuencia == that.frecuencia && termino.equals(that.termino);
    }

    @Override
    public int hashCode() {
        return Objects.hash(termino, frecuencia);
    }

    @Override
    public String toString() {
        return String.format("%-25s : %d", termino, frecuencia);
    }
}
