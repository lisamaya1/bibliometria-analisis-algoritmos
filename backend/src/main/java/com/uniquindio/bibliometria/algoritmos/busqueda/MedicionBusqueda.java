package com.uniquindio.bibliometria.algoritmos.busqueda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Comparación empírica local: calentamiento, rondas secuenciales y orden rotatorio. */
public final class MedicionBusqueda {
    private static volatile long consumo;
    public record Estadistica(int muestras,long minimoNs,double promedioNs,double medianaNs,long maximoNs) { }
    public record PorAlgoritmo(String algoritmo,Estadistica preparacion,Estadistica busqueda,Estadistica metodoCompleto) { }
    public record Resultado(int calentamientoPorAlgoritmo,int repeticionesPorAlgoritmo,String orden,
                             List<PorAlgoritmo> algoritmos) {
        public Resultado { algoritmos=List.copyOf(algoritmos); }
    }

    public static Estadistica resumir(long[] tiempos) {
        if (tiempos==null || tiempos.length==0) throw new IllegalArgumentException("No hay muestras de tiempo.");
        long[] ordenados=tiempos.clone();
        double suma=0;
        for (long t:tiempos) {
            if (t<0) throw new IllegalArgumentException("Los tiempos no pueden ser negativos.");
            suma+=t;
        }
        Arrays.sort(ordenados);
        int medio=ordenados.length/2;
        double mediana=ordenados.length%2==0 ? ordenados[medio-1]/2.0+ordenados[medio]/2.0 : ordenados[medio];
        return new Estadistica(tiempos.length,ordenados[0],suma/tiempos.length,mediana,ordenados[ordenados.length-1]);
    }

    public Resultado medir(String texto,String patron,int calentamiento,int repeticiones) {
        if (calentamiento<1 || repeticiones<1 || calentamiento>1_000_000 || repeticiones>1_000_000)
            throw new IllegalArgumentException("Usa entre 1 y 1000000 repeticiones y al menos una de calentamiento.");
        var comparador=new ComparadorBusqueda();
        var referencia=comparador.comparar(texto,patron);
        var algoritmos=comparador.algoritmos();
        for (int ronda=0;ronda<calentamiento;ronda++) for (int paso=0;paso<algoritmos.size();paso++) {
            int i=(ronda+paso)%algoritmos.size();
            var r=algoritmos.get(i).buscar(texto,patron);
            ComparadorBusqueda.comprobarIgualdad(referencia.get(i),r);
            consumo^=r.comparaciones()+r.posiciones().hashCode();
        }
        long[][] preparacion=new long[3][repeticiones],busqueda=new long[3][repeticiones],total=new long[3][repeticiones];
        for (int ronda=0;ronda<repeticiones;ronda++) for (int paso=0;paso<algoritmos.size();paso++) {
            int i=(ronda+paso)%algoritmos.size();
            long inicio=System.nanoTime();
            var r=algoritmos.get(i).buscar(texto,patron);
            long duracion=System.nanoTime()-inicio;
            total[i][ronda]=duracion;
            preparacion[i][ronda]=r.tiempoPreparacionNs();
            busqueda[i][ronda]=r.tiempoBusquedaNs();
            // Comprobación y consumo fuera del intervalo cronometrado.
            ComparadorBusqueda.comprobarIgualdad(referencia.get(i),r);
            consumo^=r.comparaciones()+r.posiciones().hashCode();
        }
        var resultados=new ArrayList<PorAlgoritmo>();
        for (int i=0;i<algoritmos.size();i++) resultados.add(new PorAlgoritmo(referencia.get(i).algoritmo(),
                resumir(preparacion[i]),resumir(busqueda[i]),resumir(total[i])));
        return new Resultado(calentamiento,repeticiones,"secuencial rotatorio: KMP/BM/RK, BM/RK/KMP, RK/KMP/BM",resultados);
    }
}
