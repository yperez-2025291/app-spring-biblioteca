package com.yubiniperez.biblioteca.dto;

import com.yubiniperez.biblioteca.model.EstadoPrestamo;

import java.time.LocalDate;

public record PrestamoResponse(Long id,
                               Long usuarioId,
                               String usuarioNombre,
                               Long libroId,
                               String libroTitulo,
                               LocalDate fechaPrestamo,
                               LocalDate fechaDevolucionEsperada,
                               LocalDate fechaDevolucionReal,
                               EstadoPrestamo estado) {
}
