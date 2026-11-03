package com.uniquindio.bibliometria.controller;

import com.uniquindio.bibliometria.service.ComparacionClasicaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/similitud")
@CrossOrigin(origins = "*")
public class ComparacionClasicaController {
    private final ComparacionClasicaService servicio;

    public ComparacionClasicaController(ComparacionClasicaService servicio) { this.servicio = servicio; }

    @GetMapping("/clasicos")
    public ResponseEntity<?> comparar(@RequestParam List<Integer> articulos) {
        try {
            return ResponseEntity.ok(servicio.comparar(articulos));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", error.getMessage()));
        } catch (IOException error) {
            return ResponseEntity.internalServerError().body(Map.of("mensaje", "No se pudo leer el corpus de artículos."));
        }
    }
}
