package com.yubiniperez.biblioteca.dto;

public record LoginResponse(String token, String tipo, String email, String rol, long expiraEnMs) {
}