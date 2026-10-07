package com.yubiniperez.biblioteca.controller;

import com.yubiniperez.biblioteca.dto.PrestamoRequest;
import com.yubiniperez.biblioteca.dto.PrestamoResponse;
import com.yubiniperez.biblioteca.service.PrestamoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/prestamos")
public class PrestamoController {

    private final PrestamoService prestamoService;

    public PrestamoController(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
    }

    @PostMapping
    public ResponseEntity<PrestamoResponse> registrar(@Valid @RequestBody PrestamoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prestamoService.registrar(request));
    }

    @PatchMapping("/{id}/devolucion")
    public ResponseEntity<PrestamoResponse> devolver(@PathVariable Long id) {
        return ResponseEntity.ok(prestamoService.devolver(id));
    }

    @GetMapping("/mis-prestamos")
    public ResponseEntity<List<PrestamoResponse>> misPrestamos(Authentication authentication) {
        // El filtro JWT guardó el email como nombre de la autenticación
        return ResponseEntity.ok(prestamoService.misPrestamos(authentication.getName()));
    }

    @GetMapping("/atrasados")
    public ResponseEntity<List<PrestamoResponse>> atrasados() {
        return ResponseEntity.ok(prestamoService.atrasados());
    }
}