package com.uniquindio.bibliometria.controller;

import com.uniquindio.bibliometria.service.ComparacionOrdenamientosService;
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
public class ComparacionOrdenamientosController {
    private final ComparacionOrdenamientosService comparacion;

    public ComparacionOrdenamientosController(ComparacionOrdenamientosService comparacion) {
        this.comparacion = comparacion;
    }

    @GetMapping("/comparacion-ordenamientos")
    public ResponseEntity<?> comparar(@RequestParam(defaultValue = "15") int topK,
                                      @RequestParam(defaultValue = "true") boolean filtrarStopwords) {
        if (topK < 1 || topK > 1000) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La cantidad de resultados debe estar entre 1 y 1000."));
        }
        try {
            return ResponseEntity.ok(comparacion.comparar(topK, filtrarStopwords));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", error.getMessage()));
        } catch (IOException error) {
            return ResponseEntity.internalServerError().body(Map.of("mensaje", "No se pudo leer el corpus de artículos."));
        }
    }
}
