package com.uniquindio.bibliometria.algoritmos.agrupamiento;

import com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace.*;
import java.util.ArrayList;
import java.util.List;

public final class EvaluadorClustering {
    public static final double TOLERANCIA_EMPATE=1e-12;
    public record PorK(int k, double silhouette, List<Integer> tamanos) {
        public PorK { tamanos=List.copyOf(tamanos); }
    }
    public record Mejor(int k, double silhouette, List<Particiones.Grupo> clusters) {
        public Mejor { clusters=List.copyOf(clusters); }
    }
    public record PorCriterio(String criterio, List<PorK> resultados, Mejor mejor) {
        public PorCriterio { resultados=List.copyOf(resultados); }
    }
    public record Resultado(int cantidadArticulos, String distanciaBase, double toleranciaEmpate,
                             List<PorCriterio> criterios, double maximoSilhouette, List<String> criteriosMasCoherentes) {
        public Resultado { criterios=List.copyOf(criterios); criteriosMasCoherentes=List.copyOf(criteriosMasCoherentes); }
    }

    /** Primero obtiene el máximo; después resuelve empates contra ese máximo, sin encadenar tolerancias. */
    public PorK elegirMejor(List<PorK> resultados) {
        if (resultados==null || resultados.isEmpty()) throw new IllegalArgumentException("No hay particiones evaluadas.");
        double maximo=Double.NEGATIVE_INFINITY;
        for (var r : resultados) {
            if (!Double.isFinite(r.silhouette()) || r.silhouette() < -1 || r.silhouette()>1)
                throw new IllegalArgumentException("Silhouette inválido.");
            if (r.silhouette()>maximo) maximo=r.silhouette();
        }
        PorK mejor=null;
        for (var r : resultados)
            if (maximo-r.silhouette()<=TOLERANCIA_EMPATE && (mejor==null || r.k()<mejor.k())) mejor=r;
        return mejor;
    }

    public Resultado evaluar(MatrizDistancias matriz) {
        if (matriz==null || matriz.tamano()<3) throw new IllegalArgumentException("La evaluación necesita al menos tres artículos.");
        String[] nombres={"single","complete","average"};
        AglomeradorJerarquico[] algoritmos={new SingleLinkage(),new CompleteLinkage(),new AverageLinkage()};
        var criterios=new ArrayList<PorCriterio>();
        var silhouette=new Silhouette();
        double maximo=Double.NEGATIVE_INFINITY;
        for (int i=0; i<algoritmos.length; i++) {
            var historial=algoritmos[i].agrupar(matriz).fusiones();
            var resultados=new ArrayList<PorK>();
            for (int k=2; k<matriz.tamano(); k++) {
                var grupos=Particiones.cortar(matriz.articulos(),historial,k);
                var valores=silhouette.calcular(matriz,grupos);
                var tamanos=new ArrayList<Integer>();
                for (var grupo : grupos) tamanos.add(grupo.tamano());
                resultados.add(new PorK(k,valores.global(),tamanos));
                if (valores.global()>maximo) maximo=valores.global();
            }
            var elegido=elegirMejor(resultados);
            criterios.add(new PorCriterio(nombres[i],resultados,new Mejor(elegido.k(),elegido.silhouette(),
                    Particiones.cortar(matriz.articulos(),historial,elegido.k()))));
        }
        var ganadores=new ArrayList<String>();
        for (var criterio : criterios) {
            // Un criterio comparte el máximo si alguna de sus particiones está dentro de la tolerancia.
            for (var r : criterio.resultados()) if (maximo-r.silhouette()<=TOLERANCIA_EMPATE) {
                ganadores.add(criterio.criterio());
                break;
            }
        }
        ganadores.sort(String::compareTo);
        return new Resultado(matriz.tamano(),"1 - coseno TF-IDF",TOLERANCIA_EMPATE,criterios,maximo,ganadores);
    }
}
