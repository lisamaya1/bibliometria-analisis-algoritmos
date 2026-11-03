# Plan pendiente del proyecto final

Fuente principal: `Proyecto Análisis de Algoritmos - 2026-2.pdf`, documento oficial
revisado de tres páginas. `Seguimiento 2026-2.pdf` corresponde al avance de
frecuencias y ordenamientos, que se conserva como módulo existente.

La auditoría está cerrada y Jaccard corresponde a la fase 2. El trabajo restante
se realiza por fases; esta lista no autoriza implementar anticipadamente módulos
ni realizar operaciones de publicación en Git.

1. **Fase 3:** implementar y probar Sørensen-Dice.
2. **Fase 4:** implementar y probar Levenshtein.
3. **Fase 5:** implementar y probar Coseno TF-IDF con cálculos explícitos.
4. **Fase 6:** integrar backend y frontend para comparar los cuatro métodos clásicos
   con selección dinámica de artículos del corpus.
5. **Fase 7:** proponer dos enfoques de embeddings y esperar aprobación antes de
   integrarlos. Explicar dependencias, conexión, claves y condiciones de ejecución.
6. **Fase 8:** comparar clásicos frente a IA con las mismas entradas.
7. **Fase 9:** completar la demostración matemática común para los artículos 15 y 19.
   Jaccard vigente: 33/241 ≈ 13,69 %; 31/243 es únicamente histórico.
8. **Fases 10–13:** implementar infraestructura común de agrupamiento jerárquico y
   Single, Complete y Average Linkage sobre los 20 abstracts (requerimiento 2).
9. **Fase 14:** proponer una métrica matemática interna; implementarla desde cero
   después de su aprobación y comparar la coherencia de los agrupamientos.
10. **Fases 15–16:** generar los tres dendrogramas e integrar clustering al frontend.
    Justificar cualquier biblioteca nueva antes de incorporarla.
11. **Requerimiento 3, antes del cierre de pruebas:** implementar desde cero KMP,
    Boyer-Moore y Rabin-Karp para búsqueda exacta de un patrón ingresado por el
    usuario sobre los abstracts. Incorporar backend y frontend, pruebas y un caso
    común paso a paso: prefijos/sufijos, tablas de desplazamiento y rolling hash,
    según cada algoritmo. Comparar teoría y mediciones empíricas, distinguiendo
    preprocesamiento y búsqueda. No usar `find` o `indexOf` para resolver el algoritmo.
12. **Fase 17:** ejecutar pruebas completas del backend, frontend y flujo integral,
    incluyendo regresión del módulo de ordenamientos.
13. **Fase 18:** presentar el código y auxiliares, acordar costos y realizar el conteo
    T(n) con la metodología del profesor; después obtener el orden de complejidad.
    Incluir la comparación asintótica solicitada para búsqueda de patrones.
14. **Fases 19–20:** completar documentación técnica y documento final con
    arquitectura, implementación de cada requerimiento, resultados reales, pruebas,
    limitaciones y declaración explícita y verificable del uso de IA.
15. **Fase 21:** proponer y acordar una plataforma antes de configurar el despliegue
    de Spring Boot, React, JSON y los enfoques de embeddings aprobados.
16. **Fase 22:** validar la aplicación desplegada y las instrucciones de uso.

El **requerimiento 4** exige despliegue y documentación técnica. El documento
oficial revisado no contiene requerimientos 5 ni 6; no se les asignan funciones.

Pendientes técnicos detectados en la auditoría: unificar con pruebas los dos
formatos de escritura del JSON, recuperar/adaptar pruebas documentales y actualizar
la presentación individual y la documentación que todavía describe el seguimiento.
Se realizarán como ajustes acotados, conservando funcionalidades y datos originales.
