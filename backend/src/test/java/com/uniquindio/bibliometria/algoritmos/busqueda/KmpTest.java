package com.uniquindio.bibliometria.algoritmos.busqueda;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class KmpTest {
    private final Kmp kmp = new Kmp();
    @Test void coincidenciaAlInicioMedioYFinal() {
        assertEquals(List.of(0), kmp.buscar("abcxyz", "abc").posiciones());
        assertEquals(List.of(2), kmp.buscar("xxabcxx", "abc").posiciones());
        assertEquals(List.of(3), kmp.buscar("xyzabc", "abc").posiciones());
    }
    @Test void multiplesCoincidencias() {
        var r = kmp.buscar("ab xx ab yy ab", "ab");
        assertEquals(List.of(0,6,12), r.posiciones());
        assertEquals(3, r.ocurrencias());
        assertTrue(r.encontrado());
    }
    @Test void conservaSuperposiciones() {
        var r = kmp.buscar("aaaa", "aa");
        assertEquals(List.of(0,1,2), r.posiciones());
        assertEquals(4, r.comparaciones());
        assertEquals(1, r.comparacionesPreparacion());
        assertEquals(List.of(0,2,4), kmp.buscar("abababa", "aba").posiciones());
    }
    @Test void patronInexistente() {
        var r = kmp.buscar("abcdef", "xyz");
        assertFalse(r.encontrado());
        assertEquals(0, r.ocurrencias());
        assertEquals(List.of(), r.posiciones());
    }
    @Test void patronMasLargoYTextoVacioNoComparanTexto() {
        assertEquals(0, kmp.buscar("ab", "abc").comparaciones());
        assertEquals(List.of(), kmp.buscar("", "a").posiciones());
        assertEquals(0, kmp.buscar("", "a").comparaciones());
    }
    @Test void patronIgualAlTexto() {
        assertEquals(List.of(0), kmp.buscar("abc", "abc").posiciones());
        assertEquals(3, kmp.buscar("abc", "abc").comparaciones());
    }
    @Test void rechazaPatronVacioYNulos() {
        assertThrows(IllegalArgumentException.class, () -> kmp.buscar("abc", ""));
        assertThrows(IllegalArgumentException.class, () -> kmp.buscar("abc", null));
        assertThrows(IllegalArgumentException.class, () -> kmp.buscar(null, "a"));
        assertThrows(IllegalArgumentException.class, () -> kmp.construirLps(""));
    }
    @Test void textoYPatronNoSeNormalizanYLosIndicesSonUnicode() {
        assertEquals(List.of(1,3), kmp.buscar("😀a😀a", "a").posiciones());
        assertEquals(4, kmp.buscar("😀a😀a", "a").longitudTexto());
        assertEquals(List.of(0,2), kmp.buscar("😀a😀a", "😀").posiciones());
        assertFalse(kmp.buscar("chatgpt", "ChatGPT").encontrado());
        assertEquals(List.of(1,2), kmp.buscar("a  b", " ").posiciones());
    }
    @Test void tablaLpsConocidaYConPrefijosReutilizados() {
        assertEquals(List.of(0,0,1,2,0,1,2,3,4), kmp.construirLps("ABABCABAB"));
        assertEquals(List.of(0,1,2,3), kmp.construirLps("aaaa"));
        assertEquals(List.of(0,0,0), kmp.construirLps("abc"));
    }
    @Test void ejemploControladoTrazaLosRetrocesosSinRetrocederTexto() {
        var r = kmp.buscarConTraza("ABABDABACDABABCABAB", "ABABCABAB");
        assertEquals(List.of(10), r.resultado().posiciones());
        assertEquals(r.pasos().size(), r.resultado().comparaciones());
        assertTrue(r.pasos().stream().anyMatch(p -> !p.iguales() && p.siguienteTexto()==p.indiceTexto() && p.siguientePatron()<p.indicePatron()));
        for (var p : r.pasos()) {
            assertTrue(p.siguienteTexto() >= p.indiceTexto());
            assertEquals(p.caracterTexto()==p.caracterPatron(), p.iguales());
        }
        assertEquals(List.of(0,0,1,2,0,1,2,3,4), r.lps());
    }
    @Test void contadorCuentaUnaEvaluacionPorParejaInclusoDespuesDeRetroceso() {
        assertEquals(5, kmp.buscar("abac", "ab").comparaciones());
        var r = kmp.buscar("aaaa", "aaaa");
        assertEquals(4, r.comparaciones());
        assertEquals(3, r.comparacionesPreparacion());
        assertTrue(r.tiempoBusquedaNs() >= 0);
        assertTrue(r.tiempoPreparacionNs() >= 0);
        assertThrows(UnsupportedOperationException.class, () -> r.posiciones().add(9));
    }
    @Test void contrasteExhaustivoConBarridoIndependienteEnTextosBinariosPequenos() {
        for (int n=0; n<=6; n++) for (int t=0; t<(1<<n); t++) {
            String texto = binario(t,n);
            for (int m=1; m<=4; m++) for (int p=0; p<(1<<m); p++) {
                String patron = binario(p,m);
                assertEquals(referencia(texto,patron), kmp.buscar(texto,patron).posiciones(), texto + " / " + patron);
            }
        }
    }
    private String binario(int valor,int longitud) {
        var s = new StringBuilder();
        for (int i=0; i<longitud; i++) s.append((valor & (1<<i))==0 ? 'a' : 'b');
        return s.toString();
    }
    public static List<Integer> referencia(String texto, String patron) {
        int[] t=texto.codePoints().toArray(), p=patron.codePoints().toArray();
        var posiciones=new ArrayList<Integer>();
        for (int i=0; i<=t.length-p.length; i++) {
            int j=0;
            while (j<p.length && t[i+j]==p[j]) j++;
            if (j==p.length) posiciones.add(i);
        }
        return posiciones;
    }
}
