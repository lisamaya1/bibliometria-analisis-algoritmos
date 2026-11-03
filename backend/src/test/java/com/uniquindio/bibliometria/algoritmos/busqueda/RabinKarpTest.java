package com.uniquindio.bibliometria.algoritmos.busqueda;

import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RabinKarpTest {
    private final RabinKarp rk=new RabinKarp();
    private long referenciaHash(int[] texto,int inicio,int longitud) {
        BigInteger hash=BigInteger.ZERO;
        var base=BigInteger.valueOf(RabinKarp.BASE);
        var modulo=BigInteger.valueOf(RabinKarp.MODULO);
        for (int i=inicio;i<inicio+longitud;i++) hash=hash.multiply(base).add(BigInteger.valueOf(texto[i])).mod(modulo);
        return hash.longValueExact();
    }
    @Test void hashesInicialesYPotenciaConReferenciaDeEnterosGrandes() {
        var r=rk.buscar("ABAAABCD","ABC");
        var e=r.estadisticasHash();
        assertEquals(referenciaHash("ABC".codePoints().toArray(),0,3),e.hashPatron());
        assertEquals(referenciaHash("ABA".codePoints().toArray(),0,3),e.hashVentanaInicial());
        assertEquals(BigInteger.valueOf(RabinKarp.BASE).modPow(BigInteger.TWO,BigInteger.valueOf(RabinKarp.MODULO)).longValueExact(),e.potencia());
        assertEquals(3,e.pasosHashPatron());
        assertEquals(3,e.pasosHashInicial());
        assertEquals(2,e.pasosPotencia());
    }
    @Test void rollingDeCadaVentanaCoincideConRecalculoIndependienteYCorrigeRestaNegativa() {
        String texto="ABAAABCD";
        var r=rk.buscarConTraza(texto,"ABC");
        int[] t=texto.codePoints().toArray();
        assertEquals(6,r.pasos().size());
        assertTrue(r.pasos().stream().anyMatch(p->p.restaAntesDeCorregir()!=null && p.restaAntesDeCorregir()<0));
        for (var paso:r.pasos()) {
            assertEquals(referenciaHash(t,paso.alineacion(),3),paso.hashVentana());
            assertTrue(paso.hashVentana()>=0 && paso.hashVentana()<RabinKarp.MODULO);
            if (paso.hashSiguiente()!=null) assertEquals(referenciaHash(t,paso.alineacion()+1,3),paso.hashSiguiente());
        }
        assertEquals(List.of(4),r.resultado().posiciones());
        assertEquals(5,r.resultado().estadisticasHash().actualizacionesRolling());
        assertEquals(3,r.resultado().comparaciones());
    }
    @Test void colisionConLasMismasConstantesNoSeAceptaSinVerificarCaracteres() {
        // BASE*(65+q/BASE)+(65+q%BASE) difiere del hash de AA exactamente en q.
        int a=(int)(65+RabinKarp.MODULO/RabinKarp.BASE);
        int b=(int)(65+RabinKarp.MODULO%RabinKarp.BASE);
        assertTrue(Character.isValidCodePoint(a) && Character.isValidCodePoint(b));
        String texto=new String(new int[]{a,b,65,65},0,4);
        var r=rk.buscarConTraza(texto,"AA");
        assertEquals(List.of(2),r.resultado().posiciones());
        assertTrue(r.pasos().get(0).coincideHash());
        assertTrue(r.pasos().get(0).colision());
        assertNull(r.pasos().get(0).posicionEncontrada());
        assertEquals(2,r.resultado().estadisticasHash().coincidenciasHash());
        assertEquals(1,r.resultado().estadisticasHash().colisiones());
        assertEquals(3,r.resultado().comparaciones());
    }
    @Test void coincidenciasInicioMedioFinalYMultiples() {
        assertEquals(List.of(0),rk.buscar("abcxyz","abc").posiciones());
        assertEquals(List.of(2),rk.buscar("xxabcxx","abc").posiciones());
        assertEquals(List.of(3),rk.buscar("xyzabc","abc").posiciones());
        assertEquals(List.of(0,6,12),rk.buscar("ab xx ab yy ab","ab").posiciones());
    }
    @Test void superposicionesSeVerificanEnTodasLasVentanas() {
        var r=rk.buscar("aaaa","aa");
        assertEquals(List.of(0,1,2),r.posiciones());
        assertEquals(3,r.estadisticasHash().ventanasProcesadas());
        assertEquals(3,r.estadisticasHash().coincidenciasHash());
        assertEquals(6,r.comparaciones());
        assertEquals(0,r.estadisticasHash().colisiones());
    }
    @Test void patronInexistenteNoRealizaVerificacionSiLosHashesSonDistintos() {
        var r=rk.buscar("abcdef","xyz");
        assertFalse(r.encontrado());
        assertEquals(0,r.comparaciones());
        assertEquals(4,r.estadisticasHash().ventanasProcesadas());
    }
    @Test void vacioYPatronMasLargoNoProcesanVentanas() {
        for (String texto:List.of("","a")) {
            var r=rk.buscar(texto,"abc");
            assertEquals(List.of(),r.posiciones());
            assertEquals(0,r.estadisticasHash().ventanasProcesadas());
            assertNull(r.estadisticasHash().hashVentanaInicial());
            assertEquals(0,r.estadisticasHash().pasosHashInicial());
            assertEquals(0,r.comparaciones());
        }
    }
    @Test void textoYPatronIguales() {
        var r=rk.buscar("abc","abc");
        assertEquals(List.of(0),r.posiciones());
        assertEquals(3,r.comparaciones());
        assertEquals(1,r.estadisticasHash().ventanasProcesadas());
        assertEquals(0,r.estadisticasHash().actualizacionesRolling());
    }
    @Test void unicodeSinDesbordamientoYPatronSinTransformar() {
        assertEquals(List.of(1,3),rk.buscar("😀a😀a","a").posiciones());
        assertEquals(List.of(0,2),rk.buscar("😀a😀a","😀").posiciones());
        assertFalse(rk.buscar("chatgpt","ChatGPT").encontrado());
        assertEquals(List.of(1,2),rk.buscar("a  b"," ").posiciones());
        assertEquals(List.of(1),rk.buscar("a.b",".").posiciones());
        String texto=new String(new int[]{0x10ffff,0x1f600,0x10fffe,0x1f600,0x10ffff},0,5);
        String patron=new String(new int[]{0x1f600,0x10fffe},0,2);
        var r=rk.buscarConTraza(texto,patron);
        for (var p:r.pasos()) assertEquals(referenciaHash(texto.codePoints().toArray(),p.alineacion(),2),p.hashVentana());
        assertEquals(List.of(1),r.resultado().posiciones());
    }
    @Test void patronVacioYNulosSeRechazan() {
        assertThrows(IllegalArgumentException.class,()->rk.buscar("abc",""));
        assertThrows(IllegalArgumentException.class,()->rk.buscar("abc",null));
        assertThrows(IllegalArgumentException.class,()->rk.buscar(null,"a"));
    }
    @Test void conteosSeparadosCoincidenConLaTraza() {
        var r=rk.buscarConTraza("ABAAABCD","ABC");
        assertEquals(r.pasos().stream().mapToLong(p->p.comparaciones().size()).sum(),r.resultado().comparaciones());
        assertEquals(0,r.resultado().comparacionesPreparacion());
        assertTrue(r.resultado().tiempoBusquedaNs()>=0 && r.resultado().tiempoPreparacionNs()>=0);
    }
    @Test void contrasteExhaustivoConElBarridoIndependienteExistente() {
        for (int n=0;n<=6;n++) for (int t=0;t<(1<<n);t++) {
            String texto=binario(t,n);
            for (int m=1;m<=4;m++) for (int p=0;p<(1<<m);p++) {
                String patron=binario(p,m);
                assertEquals(KmpTest.referencia(texto,patron),rk.buscar(texto,patron).posiciones(),texto+" / "+patron);
            }
        }
    }
    private String binario(int valor,int longitud) {
        var s=new StringBuilder();
        for (int i=0;i<longitud;i++) s.append((valor&(1<<i))==0?'a':'b');
        return s.toString();
    }
}
