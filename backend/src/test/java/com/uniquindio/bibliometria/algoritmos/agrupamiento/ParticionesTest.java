package com.uniquindio.bibliometria.algoritmos.agrupamiento;

import com.uniquindio.bibliometria.algoritmos.agrupamiento.enlace.SingleLinkage;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ParticionesTest {
    private final MatrizDistancias m=new MatrizDistancias(List.of(1,2,3,4),new double[][]{
            {0,.1,.6,.8},{.1,0,.3,.7},{.6,.3,0,.4},{.8,.7,.4,0}});
    @Test void reproduceElCortePorNumeroDeFusiones() {
        var historial=new SingleLinkage().agrupar(m).fusiones();
        var k3=Particiones.cortar(m.articulos(),historial,3);
        assertEquals(List.of(3,4,5),k3.stream().map(Particiones.Grupo::id).toList());
        assertEquals(List.of(1,2),k3.get(2).articulos());
        assertEquals(2,Particiones.cortar(m.articulos(),historial,2).size());
        assertEquals(4,Particiones.cortar(m.articulos(),historial,1).get(0).tamano());
        assertEquals(4,Particiones.cortar(m.articulos(),historial,4).size());
    }
    @Test void rechazaCortesFueraDeRangoEHistorialInsuficiente() {
        assertThrows(IllegalArgumentException.class,()->Particiones.cortar(m.articulos(),List.of(),0));
        assertThrows(IllegalArgumentException.class,()->Particiones.cortar(m.articulos(),List.of(),5));
        assertThrows(IllegalArgumentException.class,()->Particiones.cortar(m.articulos(),List.of(),2));
    }
    @Test void rechazaHistorialQueNoCorrespondeASusMiembros() {
        var f=new SingleLinkage.Fusion(5,1,2,List.of(1,3),.1,2);
        assertThrows(IllegalArgumentException.class,()->Particiones.cortar(m.articulos(),List.of(f),3));
    }
}
