package com.uniquindio.bibliometria.controller;

import com.uniquindio.bibliometria.service.ComparacionBusquedaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/busqueda")
@CrossOrigin(origins="*")
public class ComparacionBusquedaController {
    private final ComparacionBusquedaService servicio;
    public ComparacionBusquedaController(ComparacionBusquedaService servicio) { this.servicio=servicio; }
    @GetMapping("/comparar")
    public ResponseEntity<?> comparar(@RequestParam int articulo,@RequestParam String patron) {
        try { return ResponseEntity.ok(servicio.comparar(articulo,patron)); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("mensaje",e.getMessage())); }
        catch (IllegalStateException e) { return ResponseEntity.internalServerError().body(Map.of("mensaje",e.getMessage())); }
        catch (IOException e) { return ResponseEntity.internalServerError().body(Map.of("mensaje","No se pudo leer el JSON del corpus. No se regeneró desde los PDF.")); }
    }
}
