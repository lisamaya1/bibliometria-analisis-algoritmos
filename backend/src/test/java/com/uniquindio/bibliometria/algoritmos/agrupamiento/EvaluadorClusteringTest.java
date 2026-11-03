package com.uniquindio.bibliometria.algoritmos.agrupamiento;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EvaluadorClusteringTest {
    @Test void eligeElMaximoYEnEmpateElMenorKSinDependerDelOrden() {
        var e=new EvaluadorClustering();
        var k2=new EvaluadorClustering.PorK(2,.6,List.of(2,2));
        var k3=new EvaluadorClustering.PorK(3,.6000000000005,List.of(2,1,1));
        assertEquals(k2,e.elegirMejor(List.of(k3,k2)));
        assertEquals(k2,e.elegirMejor(List.of(k2,k3)));
        assertEquals(k3,e.elegirMejor(List.of(new EvaluadorClustering.PorK(2,.5,List.of(2,2)),k3)));
        assertThrows(IllegalArgumentException.class,()->e.elegirMejor(List.of()));
    }
    @Test void evaluaVariosKYTresCriteriosYConservaEmpatesEntreCriterios() {
        var m=new MatrizDistancias(List.of(1,2,3,4),new double[][]{
                {0,.1,.6,.8},{.1,0,.3,.7},{.6,.3,0,.4},{.8,.7,.4,0}});
        var r=new EvaluadorClustering().evaluar(m);
        assertEquals(3,r.criterios().size());
        for (var c:r.criterios()) {
            assertEquals(List.of(2,3),c.resultados().stream().map(EvaluadorClustering.PorK::k).toList());
            assertEquals(1,c.resultados().stream().filter(x->x.k()==3).count());
        }
        assertEquals(List.of("average","complete"),r.criteriosMasCoherentes());
        assertEquals((6.0/7+.8+1.0/9+7.0/15)/4,r.maximoSilhouette(),1e-12);
        assertEquals(2,r.criterios().get(1).mejor().k());
        assertEquals(2,r.criterios().get(2).mejor().k());
    }
    @Test void rechazaEvaluacionSinKAplicable() {
        assertThrows(IllegalArgumentException.class,()->new EvaluadorClustering().evaluar(
                new MatrizDistancias(List.of(1,2),new double[][]{{0,.1},{.1,0}})));
    }
}
