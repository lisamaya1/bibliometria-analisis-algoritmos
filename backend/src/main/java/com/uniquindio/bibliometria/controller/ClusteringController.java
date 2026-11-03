package com.uniquindio.bibliometria.controller;

import com.uniquindio.bibliometria.service.ClusteringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/clustering")
@CrossOrigin(origins = "*")
public class ClusteringController {
    private final ClusteringService servicio;
    public ClusteringController(ClusteringService servicio) { this.servicio = servicio; }

    @GetMapping("/single")
    public ResponseEntity<?> single() {
        try { return ResponseEntity.ok(servicio.single()); }
        catch (IllegalArgumentException error) { return ResponseEntity.badRequest().body(Map.of("mensaje", error.getMessage())); }
        catch (IOException error) { return ResponseEntity.internalServerError().body(Map.of("mensaje", "No se pudo leer el corpus de artículos.")); }
    }

    @GetMapping("/complete")
    public ResponseEntity<?> complete() {
        try { return ResponseEntity.ok(servicio.complete()); }
        catch (IllegalArgumentException error) { return ResponseEntity.badRequest().body(Map.of("mensaje", error.getMessage())); }
        catch (IOException error) { return ResponseEntity.internalServerError().body(Map.of("mensaje", "No se pudo leer el corpus de artículos.")); }
    }

    @GetMapping("/average")
    public ResponseEntity<?> average() {
        try { return ResponseEntity.ok(servicio.average()); }
        catch (IllegalArgumentException error) { return ResponseEntity.badRequest().body(Map.of("mensaje", error.getMessage())); }
        catch (IOException error) { return ResponseEntity.internalServerError().body(Map.of("mensaje", "No se pudo leer el corpus de artículos.")); }
    }

    @GetMapping("/evaluacion")
    public ResponseEntity<?> evaluar() {
        try { return ResponseEntity.ok(servicio.evaluar()); }
        catch (IllegalArgumentException error) { return ResponseEntity.badRequest().body(Map.of("mensaje", error.getMessage())); }
        catch (IOException error) { return ResponseEntity.internalServerError().body(Map.of("mensaje", "No se pudo leer el corpus de artículos.")); }
    }
}
