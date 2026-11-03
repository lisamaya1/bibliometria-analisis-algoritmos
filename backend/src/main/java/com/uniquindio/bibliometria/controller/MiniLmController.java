package com.uniquindio.bibliometria.controller;

import com.uniquindio.bibliometria.service.MiniLmService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/ia")
@CrossOrigin(origins = "*")
public class MiniLmController {
    private final MiniLmService servicio;
    public MiniLmController(MiniLmService servicio) { this.servicio = servicio; }

    @GetMapping("/minilm")
    public ResponseEntity<?> comparar(@RequestParam int articuloA, @RequestParam int articuloB) {
        try { return ResponseEntity.ok(servicio.comparar(articuloA, articuloB)); }
        catch (IllegalArgumentException error) { return ResponseEntity.badRequest().body(Map.of("mensaje", error.getMessage())); }
        catch (IllegalStateException error) { return ResponseEntity.status(503).body(Map.of("mensaje", error.getMessage())); }
        catch (IOException error) { return ResponseEntity.internalServerError().body(Map.of("mensaje", "No se pudo leer el corpus de artículos.")); }
    }
}
