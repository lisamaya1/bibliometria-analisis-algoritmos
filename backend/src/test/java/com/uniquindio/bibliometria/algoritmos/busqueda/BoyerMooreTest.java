package com.uniquindio.bibliometria.algoritmos.busqueda;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class BoyerMooreTest {
    private final BoyerMoore bm = new BoyerMoore();
    @Test void tablaConservaLaUltimaAparicionYCuentaActualizaciones() {
        var tabla = bm.preparar("ABCA");
        assertEquals(Map.of((int)'A',3,(int)'B',1,(int)'C',2),tabla.posiciones());
        assertEquals(4,tabla.actualizaciones());
        assertThrows(UnsupportedOperationException.class,()->tabla.posiciones().clear());
    }
    @Test void ejemploControladoComparaPorLaDerechaYSaltosExplicitos() {
        var r = bm.buscarConTraza("ABAAABCD","ABC");
        assertEquals(List.of(4),r.resultado().posiciones());
        assertEquals(List.of(0,2,4,5),r.pasos().stream().map(BoyerMoore.Paso::alineacion).toList());
        assertEquals(List.of(2,2,1,3),r.pasos().stream().map(BoyerMoore.Paso::desplazamiento).toList());
        assertEquals(List.of(2,1,0),r.pasos().get(2).comparaciones().stream().map(BoyerMoore.Comparacion::indicePatron).toList());
        assertEquals(6,r.resultado().comparaciones());
        assertEquals(0,r.resultado().comparacionesPreparacion());
        assertEquals(3,r.preparacion().actualizaciones());
        assertEquals(Integer.valueOf(0),r.pasos().get(0).ultimaAparicion());
        assertEquals(Integer.valueOf(-1),r.pasos().get(3).ultimaAparicion());
        assertEquals(Integer.valueOf(4),r.pasos().get(2).posicionEncontrada());
    }
    @Test void caracterInexistenteDesplazaPatronCompleto() {
        var r = bm.buscarConTraza("ZZZABC","ABC");
        assertEquals(-1,r.pasos().get(0).ultimaAparicion());
        assertEquals(3,r.pasos().get(0).desplazamiento());
        assertEquals(List.of(3),r.resultado().posiciones());
        assertEquals(4,r.resultado().comparaciones());
    }
    @Test void coincidenciasAlInicioMedioYFinal() {
        assertEquals(List.of(0),bm.buscar("abcxyz","abc").posiciones());
        assertEquals(List.of(2),bm.buscar("xxabcxx","abc").posiciones());
        assertEquals(List.of(3),bm.buscar("xyzabc","abc").posiciones());
    }
    @Test void multiplesCoincidencias() {
        var r = bm.buscar("ab xx ab yy ab","ab");
        assertEquals(List.of(0,6,12),r.posiciones());
        assertEquals(3,r.ocurrencias());
        assertTrue(r.encontrado());
    }
    @Test void superposicionesYComparacionesTrasCoincidenciaCompleta() {
        var r = bm.buscar("aaaa","aa");
        assertEquals(List.of(0,1,2),r.posiciones());
        assertEquals(6,r.comparaciones());
        assertEquals(List.of(0,2,4),bm.buscar("abababa","aba").posiciones());
    }
    @Test void inexistenteMasLargoYTextoVacio() {
        assertFalse(bm.buscar("abcdef","xyz").encontrado());
        assertEquals(List.of(),bm.buscar("a","abc").posiciones());
        assertEquals(0,bm.buscar("a","abc").comparaciones());
        assertEquals(0,bm.buscar("","a").comparaciones());
    }
    @Test void textoIgualAlPatron() {
        var r=bm.buscar("abc","abc");
        assertEquals(List.of(0),r.posiciones());
        assertEquals(3,r.comparaciones());
    }
    @Test void rechazaPatronVacioYEntradasNulas() {
        assertThrows(IllegalArgumentException.class,()->bm.buscar("abc",""));
        assertThrows(IllegalArgumentException.class,()->bm.buscar("abc",null));
        assertThrows(IllegalArgumentException.class,()->bm.buscar(null,"a"));
        assertThrows(IllegalArgumentException.class,()->bm.preparar(""));
    }
    @Test void unicodeMayusculasEspaciosYPuntuacionSonExactos() {
        assertEquals(List.of(1,3),bm.buscar("😀a😀a","a").posiciones());
        assertEquals(List.of(0,2),bm.buscar("😀a😀a","😀").posiciones());
        assertEquals(4,bm.buscar("😀a😀a","a").longitudTexto());
        assertEquals(2,bm.preparar("😀a😀").posiciones().get(0x1f600));
        assertFalse(bm.buscar("chatgpt","ChatGPT").encontrado());
        assertEquals(List.of(1,2),bm.buscar("a  b"," ").posiciones());
        assertEquals(List.of(1),bm.buscar("a.b",".").posiciones());
    }
    @Test void trazaYResultadoNormalConservanConteosYPosiciones() {
        var r=bm.buscar("ABAAABCD","ABC");
        var traza=bm.buscarConTraza("ABAAABCD","ABC");
        assertEquals(r.posiciones(),traza.resultado().posiciones());
        assertEquals(r.comparaciones(),traza.pasos().stream().mapToLong(p->p.comparaciones().size()).sum());
        assertTrue(r.tiempoPreparacionNs()>=0 && r.tiempoBusquedaNs()>=0);
    }
    @Test void contrasteExhaustivoReutilizaLaReferenciaIndependienteDeKmp() {
        for (int n=0;n<=6;n++) for (int t=0;t<(1<<n);t++) {
            String texto=binario(t,n);
            for (int m=1;m<=4;m++) for (int p=0;p<(1<<m);p++) {
                String patron=binario(p,m);
                assertEquals(KmpTest.referencia(texto,patron),bm.buscar(texto,patron).posiciones(),texto+" / "+patron);
            }
        }
    }
    private String binario(int valor,int longitud) {
        var s=new StringBuilder();
        for (int i=0;i<longitud;i++) s.append((valor&(1<<i))==0?'a':'b');
        return s.toString();
    }
}
