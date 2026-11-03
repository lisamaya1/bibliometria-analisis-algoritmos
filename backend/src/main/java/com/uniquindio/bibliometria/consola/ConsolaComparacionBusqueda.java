package com.uniquindio.bibliometria.consola;

import com.uniquindio.bibliometria.algoritmos.busqueda.MedicionBusqueda;
import com.uniquindio.bibliometria.service.ComparacionBusquedaService;
import tools.jackson.databind.json.JsonMapper;

/** Ejecutable local: no inicia Spring, servidor HTTP ni modelos IA. */
public final class ConsolaComparacionBusqueda {
    public record Informe(String alcance,String java,String sistema,String arquitectura,int procesadores,
                          ComparacionBusquedaService.Respuesta funcional,MedicionBusqueda.Resultado tiempos) { }
    public static void main(String[] args) throws Exception {
        if (args.length!=4) throw new IllegalArgumentException("Uso: articulo patron calentamiento repeticiones");
        int articulo=Integer.parseInt(args[0]);
        int calentamiento=Integer.parseInt(args[2]);
        int repeticiones=Integer.parseInt(args[3]);
        var servicio=new ComparacionBusquedaService();
        var entrada=servicio.cargarEntrada(articulo,args[1]);
        var funcional=servicio.comparar(entrada);
        var tiempos=new MedicionBusqueda().medir(entrada.texto(),entrada.patron(),calentamiento,repeticiones);
        var informe=new Informe("comparación empírica local, no benchmark científico universal",System.getProperty("java.version"),
                System.getProperty("os.name"),System.getProperty("os.arch"),Runtime.getRuntime().availableProcessors(),funcional,tiempos);
        System.out.println(new JsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(informe));
    }
}
