package com.yubiniperez.biblioteca.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LibroRequest(
        @NotBlank(message = "El ISBN es obligatorio") @Size(max = 20, message = "El ISBN admite máximo 20 caracteres") String isbn,
        @NotBlank(message = "El título es obligatorio") @Size(max = 200) String titulo,
        @NotBlank(message = "El autor es obligatorio") @Size(max = 150) String autor,
        @NotBlank(message = "La categoría es obligatoria") @Size(max = 100) String categoria,
        @NotNull(message = "El stock total es obligatorio") @Min(value = 0, message = "El stock total no puede ser negativo") Integer stockTotal,
        // Se acepta por compatibilidad con clientes que lo envían, pero se IGNORA:
        // el sistema calcula el stock disponible a partir del stock total y los préstamos.
        Integer stockDisponible) {
}