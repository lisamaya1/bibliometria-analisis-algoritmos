package com.uniquindio.bibliometria.algoritmos.busqueda;

import java.util.ArrayList;
import java.util.List;

/** Hash polinómico rodante con verificación explícita de todas las coincidencias de hash. */
public final class RabinKarp implements BusquedaExacta {
    public static final long BASE = 0x110000L;
    public static final long MODULO = 1_000_000_007L;
    public record Comparacion(int indiceTexto, int indicePatron, int caracterTexto, int caracterPatron, boolean iguales) { }
    public record Paso(int alineacion, long hashVentana, boolean coincideHash, List<Comparacion> comparaciones,
                       boolean colision, Integer posicionEncontrada, Long hashSiguiente, Long restaAntesDeCorregir) {
        public Paso { comparaciones = List.copyOf(comparaciones); }
    }
    public record Ejecucion(ResultadoBusqueda resultado, List<Paso> pasos) {
        public Ejecucion { pasos = List.copyOf(pasos); }
    }
    private record Actualizacion(long hash, long restaAntesDeCorregir) { }

    @Override public ResultadoBusqueda buscar(String texto, String patron) { return ejecutar(texto,patron,false).resultado(); }
    public Ejecucion buscarConTraza(String texto, String patron) { return ejecutar(texto,patron,true); }

    private Actualizacion actualizar(long anterior, int saliente, int entrante, long potencia) {
        // Primero reduce saliente*h. Cada producto queda por debajo de 1,12e15, dentro de long.
        long retirado = ((long) saliente * potencia) % MODULO;
        long resta = anterior - retirado;
        long restante = resta % MODULO;
        if (restante < 0) restante += MODULO;
        long nuevo = (BASE * restante + entrante) % MODULO;
        return new Actualizacion(nuevo,resta);
    }

    private Ejecucion ejecutar(String texto, String patron, boolean registrar) {
        if (texto == null) throw new IllegalArgumentException("El texto no puede ser nulo.");
        if (patron == null || patron.isEmpty()) throw new IllegalArgumentException("El patrón no puede ser nulo ni vacío.");
        int[] t = texto.codePoints().toArray();
        int[] p = patron.codePoints().toArray();
        long inicio = System.nanoTime();
        long potencia=1, hashPatron=0, hashVentana=0;
        long pasosPotencia=0, pasosPatron=0, pasosInicial=0;
        for (int i=1; i<p.length; i++) { potencia=(BASE*potencia)%MODULO; pasosPotencia++; }
        for (int valor:p) { hashPatron=(BASE*hashPatron+valor)%MODULO; pasosPatron++; }
        if (p.length<=t.length)
            for (int i=0; i<p.length; i++) { hashVentana=(BASE*hashVentana+t[i])%MODULO; pasosInicial++; }
        long tiempoPreparacion=System.nanoTime()-inicio;
        Long hashInicial=p.length<=t.length ? hashVentana : null;
        var posiciones=new ArrayList<Integer>();
        var pasos=new ArrayList<Paso>();
        long ventanas=0, coincidenciasHash=0, colisiones=0, comparaciones=0, rolling=0;
        inicio=System.nanoTime();
        for (int s=0; s<=t.length-p.length; s++) {
            ventanas++;
            boolean coincide=hashVentana==hashPatron;
            boolean colision=false;
            Integer encontrada=null;
            List<Comparacion> detalle=registrar ? new ArrayList<>() : null;
            if (coincide) {
                coincidenciasHash++;
                int j=0;
                while (j<p.length) {
                    comparaciones++;
                    boolean iguales=t[s+j]==p[j];
                    if (registrar) detalle.add(new Comparacion(s+j,j,t[s+j],p[j],iguales));
                    if (!iguales) break;
                    j++;
                }
                if (j==p.length) { posiciones.add(s); encontrada=s; }
                else { colisiones++; colision=true; }
            }
            Long siguiente=null, resta=null;
            if (s<t.length-p.length) {
                var actualizacion=actualizar(hashVentana,t[s],t[s+p.length],potencia);
                siguiente=actualizacion.hash();
                resta=actualizacion.restaAntesDeCorregir();
                rolling++;
            }
            if (registrar) pasos.add(new Paso(s,hashVentana,coincide,detalle,colision,encontrada,siguiente,resta));
            if (siguiente!=null) hashVentana=siguiente;
        }
        long tiempoBusqueda=System.nanoTime()-inicio;
        var estadisticas=new EstadisticasHash(BASE,MODULO,hashPatron,hashInicial,potencia,ventanas,
                coincidenciasHash,colisiones,rolling,pasosPatron,pasosInicial,pasosPotencia);
        var resultado=new ResultadoBusqueda("rabin-karp",patron,t.length,p.length,posiciones,posiciones.size(),
                !posiciones.isEmpty(),comparaciones,0,tiempoPreparacion,tiempoBusqueda,estadisticas);
        return new Ejecucion(resultado,pasos);
    }
}
