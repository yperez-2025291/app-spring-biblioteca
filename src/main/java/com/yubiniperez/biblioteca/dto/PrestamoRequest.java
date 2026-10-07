package com.yubiniperez.biblioteca.dto;

import jakarta.validation.constraints.NotNull;

public record PrestamoRequest(
        @NotNull(message = "El usuarioId es obligatorio") Long usuarioId,
        @NotNull(message = "El libroId es obligatorio") Long libroId) {
}