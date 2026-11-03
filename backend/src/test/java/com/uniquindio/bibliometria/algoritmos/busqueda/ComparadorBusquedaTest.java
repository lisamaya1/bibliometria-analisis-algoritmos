package com.uniquindio.bibliometria.algoritmos.busqueda;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ComparadorBusquedaTest {
    @Test void multiplesPatronesIncluidosSuperpuestosAusenteYUnico() {
        for (var caso:List.of(new String[]{"aaaa","aa"},new String[]{"abc","abc"},
                new String[]{"abcxyz","xyz"},new String[]{"abc","no"},new String[]{"","a"})) {
            var resultados=new ComparadorBusqueda().comparar(caso[0],caso[1]);
            assertEquals(3,resultados.size());
            for (var r:resultados) {
                assertEquals(KmpTest.referencia(caso[0],caso[1]),r.posiciones());
                assertEquals(r.posiciones().size(),r.ocurrencias());
            }
        }
    }
    @Test void mismoUnicodeMayusculasEspaciosYPuntuacion() {
        for (var caso:List.of(new String[]{"😀a😀a","a"},new String[]{"a  b"," "},
                new String[]{"ChatGPT chatgpt","chatgpt"},new String[]{"a.b","."})) {
            var resultados=new ComparadorBusqueda().comparar(caso[0],caso[1]);
            for (var r:resultados) assertEquals(KmpTest.referencia(caso[0],caso[1]),r.posiciones());
        }
    }
    @Test void unaDiscrepanciaFuncionalSeRechaza() {
        var kmp=new Kmp();
        assertThrows(IllegalStateException.class,()->ComparadorBusqueda.comprobarIgualdad(kmp.buscar("aaa","a"),kmp.buscar("baa","a")));
        assertThrows(IllegalArgumentException.class,()->new ComparadorBusqueda().comparar("abc",""));
    }
}
