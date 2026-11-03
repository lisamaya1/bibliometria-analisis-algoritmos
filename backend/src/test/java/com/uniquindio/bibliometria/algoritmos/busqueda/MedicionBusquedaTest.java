package com.uniquindio.bibliometria.algoritmos.busqueda;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MedicionBusquedaTest {
    @Test void estadisticasConocidasSinAlterarLasMuestras() {
        long[] muestras={40,10,30,20};
        var r=MedicionBusqueda.resumir(muestras);
        assertEquals(10,r.minimoNs()); assertEquals(40,r.maximoNs());
        assertEquals(25,r.promedioNs()); assertEquals(25,r.medianaNs());
        assertArrayEquals(new long[]{40,10,30,20},muestras);
        assertEquals(20,MedicionBusqueda.resumir(new long[]{10,30,20}).medianaNs());
    }
    @Test void ejecutaCalentamientoYRepeticionesEnLosTresSinPresuponerVelocidad() {
        var r=new MedicionBusqueda().medir("aaaa","aa",3,5);
        assertEquals(3,r.calentamientoPorAlgoritmo());
        assertEquals(5,r.repeticionesPorAlgoritmo());
        assertEquals(3,r.algoritmos().size());
        for (var a:r.algoritmos()) {
            assertEquals(5,a.busqueda().muestras());
            assertTrue(a.busqueda().minimoNs()<=a.busqueda().medianaNs());
            assertTrue(a.busqueda().medianaNs()<=a.busqueda().maximoNs());
            assertTrue(a.metodoCompleto().minimoNs()>=0);
        }
    }
    @Test void rechazaMuestrasYRepeticionesInvalidas() {
        assertThrows(IllegalArgumentException.class,()->MedicionBusqueda.resumir(new long[]{}));
        assertThrows(IllegalArgumentException.class,()->MedicionBusqueda.resumir(new long[]{-1}));
        assertThrows(IllegalArgumentException.class,()->new MedicionBusqueda().medir("a","a",0,5));
        assertThrows(IllegalArgumentException.class,()->new MedicionBusqueda().medir("a","a",1,0));
    }
}
