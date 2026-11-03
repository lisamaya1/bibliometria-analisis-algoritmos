package com.uniquindio.bibliometria.controller;

import com.uniquindio.bibliometria.service.JaccardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class JaccardController {
    private final JaccardService servicio;

    public JaccardController(JaccardService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/jaccard")
    public ResponseEntity<?> comparar(@RequestParam int articuloA, @RequestParam int articuloB) {
        try {
            return ResponseEntity.ok(servicio.comparar(articuloA, articuloB));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", error.getMessage()));
        } catch (IOException error) {
            return ResponseEntity.internalServerError().body(Map.of("mensaje", "No se pudo leer el corpus de artículos."));
        }
    }
}
