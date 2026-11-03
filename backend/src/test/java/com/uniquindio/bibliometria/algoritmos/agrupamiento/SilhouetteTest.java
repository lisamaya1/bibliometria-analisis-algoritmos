package com.uniquindio.bibliometria.algoritmos.agrupamiento;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SilhouetteTest {
    private MatrizDistancias matriz() {
        return new MatrizDistancias(List.of(1,2,3,4),new double[][]{
                {0,.1,.6,.8},{.1,0,.3,.7},{.6,.3,0,.4},{.8,.7,.4,0}});
    }
    private Particiones.Grupo g(int id,Integer... miembros) {
        return new Particiones.Grupo(id,List.of(miembros),miembros.length);
    }
    @Test void pruebaControladaCompletaCohesionSeparacionIndividualYGlobal() {
        var r=new Silhouette().calcular(matriz(),List.of(g(5,1,2),g(6,3,4)));
        double[] a={.1,.1,.4,.4},b={.7,.5,.45,.75},s={6.0/7,.8,1.0/9,7.0/15};
        for (int i=0;i<4;i++) {
            var x=r.individuales().get(i);
            assertEquals(a[i],x.a(),1e-12);
            assertEquals(b[i],x.b(),1e-12);
            assertEquals(b[i],x.promediosExternos().get(0).promedio(),1e-12);
            assertEquals(s[i],x.silhouette(),1e-12);
        }
        assertEquals((6.0/7+.8+1.0/9+7.0/15)/4,r.global(),1e-12);
    }
    @Test void bEsMinimoDePromediosExternosYSingletonEsCero() {
        var r=new Silhouette().calcular(matriz(),List.of(g(5,1,2),g(3,3),g(4,4)));
        assertEquals(.6,r.individuales().get(0).b());
        assertEquals(.3,r.individuales().get(1).b());
        assertEquals(.4,r.individuales().get(2).b());
        assertEquals(2,r.individuales().get(0).promediosExternos().size());
        assertEquals(0,r.individuales().get(2).silhouette());
        assertEquals(0,r.individuales().get(3).silhouette());
        assertEquals(.375,r.global(),1e-12);
    }
    @Test void admiteValoresNegativosYRespetaRango() {
        var r=new Silhouette().calcular(matriz(),List.of(g(5,1,3),g(6,2,4)));
        assertEquals(-.25,r.individuales().get(0).silhouette(),1e-12);
        for (var i:r.individuales()) assertTrue(i.silhouette()>=-1 && i.silhouette()<=1);
        assertTrue(r.global()<0);
    }
    @Test void distanciasTodasCeroNoProducenNaN() {
        var m=new MatrizDistancias(List.of(1,2,3,4),new double[4][4]);
        var r=new Silhouette().calcular(m,List.of(g(5,1,2),g(6,3,4)));
        assertEquals(0,r.global());
        for (var i:r.individuales()) assertEquals(0,i.silhouette());
    }
    @Test void rechazaParticionesIncompletasDuplicadasOConKInvalido() {
        var s=new Silhouette();
        assertThrows(IllegalArgumentException.class,()->s.calcular(matriz(),List.of(g(5,1,2,3,4))));
        assertThrows(IllegalArgumentException.class,()->s.calcular(matriz(),List.of(g(1,1),g(2,2),g(3,3),g(4,4))));
        assertThrows(IllegalArgumentException.class,()->s.calcular(matriz(),List.of(g(5,1,2),g(6,2,3,4))));
        assertThrows(IllegalArgumentException.class,()->s.calcular(matriz(),List.of(g(5,1,2),g(6,3))));
        assertThrows(IllegalArgumentException.class,()->s.calcular(matriz(),List.of(g(5,1,2),g(6,3,99))));
    }
}
