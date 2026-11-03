package com.uniquindio.bibliometria.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Salud del servidor, sin leer el corpus ni ejecutar modelos o algoritmos. */
@RestController
public class HealthController {
    @GetMapping("/api/health")
    public Map<String, String> salud() {
        return Map.of("status", "ok");
    }
}
