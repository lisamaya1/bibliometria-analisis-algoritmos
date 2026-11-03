package com.uniquindio.bibliometria.algoritmos.agrupamiento;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Silhouette explícito sobre la misma matriz de disimilitud de los enlaces. */
public final class Silhouette {
    public record PromedioExterno(int cluster, double promedio) { }
    public record Individual(int articulo, int cluster, double a, double b, double silhouette,
                             List<PromedioExterno> promediosExternos) {
        public Individual { promediosExternos = List.copyOf(promediosExternos); }
    }
    public record Resultado(double global, List<Individual> individuales) {
        public Resultado { individuales = List.copyOf(individuales); }
    }

    public Resultado calcular(MatrizDistancias matriz, List<Particiones.Grupo> grupos) {
        if (matriz == null || grupos == null || grupos.size()<2 || grupos.size()>=matriz.tamano())
            throw new IllegalArgumentException("Silhouette requiere 2 <= k <= n-1.");
        var pertenencia = new HashMap<Integer, Particiones.Grupo>();
        var ids = new java.util.HashSet<Integer>();
        for (var grupo : grupos) {
            if (grupo == null || grupo.articulos().isEmpty() || grupo.tamano()!=grupo.articulos().size() || !ids.add(grupo.id()))
                throw new IllegalArgumentException("Clusters vacíos, repetidos o con tamaño inválido.");
            for (int id : grupo.articulos()) {
                matriz.distancia(id,id);
                if (pertenencia.put(id,grupo)!=null) throw new IllegalArgumentException("Un artículo pertenece a más de un cluster.");
            }
        }
        if (pertenencia.size()!=matriz.tamano()) throw new IllegalArgumentException("La partición debe cubrir todos los artículos.");
        var individuales = new ArrayList<Individual>();
        double total=0;
        for (int i : matriz.articulos()) {
            var propio = pertenencia.get(i);
            double a=0;
            if (propio.tamano()>1) {
                for (int j : propio.articulos()) if (i!=j) a+=matriz.distancia(i,j);
                a/=propio.tamano()-1;
            }
            double b=Double.POSITIVE_INFINITY;
            var externos = new ArrayList<PromedioExterno>();
            for (var otro : grupos) {
                if (otro.id()==propio.id()) continue;
                double promedio=0;
                for (int j : otro.articulos()) promedio+=matriz.distancia(i,j);
                promedio/=otro.tamano();
                externos.add(new PromedioExterno(otro.id(),promedio));
                if (promedio<b) b=promedio;
            }
            // Singleton: convención estándar. Si a=b=0 también se asigna cero para evitar 0/0.
            double divisor=Math.max(a,b);
            double s=propio.tamano()==1 || divisor==0 ? 0 : (b-a)/divisor;
            individuales.add(new Individual(i,propio.id(),a,b,s,externos));
            total+=s;
        }
        return new Resultado(total/matriz.tamano(),individuales);
    }
}
