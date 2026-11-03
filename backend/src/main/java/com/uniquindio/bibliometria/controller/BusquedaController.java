package com.uniquindio.bibliometria.controller;

import com.uniquindio.bibliometria.service.BusquedaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/busqueda")
@CrossOrigin(origins = "*")
public class BusquedaController {
    private final BusquedaService servicio;
    public BusquedaController(BusquedaService servicio) { this.servicio = servicio; }

    @GetMapping("/kmp")
    public ResponseEntity<?> buscar(@RequestParam int articulo, @RequestParam String patron) {
        try { return ResponseEntity.ok(servicio.buscarKmp(articulo, patron)); }
        catch (IllegalArgumentException error) { return ResponseEntity.badRequest().body(Map.of("mensaje", error.getMessage())); }
        catch (IOException error) { return ResponseEntity.internalServerError().body(Map.of("mensaje", "No se pudo leer el JSON del corpus. No se regeneró desde los PDF.")); }
    }

    @GetMapping("/boyer-moore")
    public ResponseEntity<?> buscarBoyerMoore(@RequestParam int articulo, @RequestParam String patron) {
        try { return ResponseEntity.ok(servicio.buscarBoyerMoore(articulo, patron)); }
        catch (IllegalArgumentException error) { return ResponseEntity.badRequest().body(Map.of("mensaje", error.getMessage())); }
        catch (IOException error) { return ResponseEntity.internalServerError().body(Map.of("mensaje", "No se pudo leer el JSON del corpus. No se regeneró desde los PDF.")); }
    }

    @GetMapping("/rabin-karp")
    public ResponseEntity<?> buscarRabinKarp(@RequestParam int articulo, @RequestParam String patron) {
        try { return ResponseEntity.ok(servicio.buscarRabinKarp(articulo, patron)); }
        catch (IllegalArgumentException error) { return ResponseEntity.badRequest().body(Map.of("mensaje", error.getMessage())); }
        catch (IOException error) { return ResponseEntity.internalServerError().body(Map.of("mensaje", "No se pudo leer el JSON del corpus. No se regeneró desde los PDF.")); }
    }
}
