package com.yubiniperez.biblioteca.dto;

public record LibroResponse(Long id, String isbn, String titulo, String autor, String categoria,
                            Integer stockTotal, Integer stockDisponible) {
}