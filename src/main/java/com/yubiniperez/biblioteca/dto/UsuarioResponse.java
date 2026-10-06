package com.yubiniperez.biblioteca.dto;

import com.yubiniperez.biblioteca.model.EstadoUsuario;
import com.yubiniperez.biblioteca.model.Rol;

public record UsuarioResponse(Long id, String nombre, String email, Rol rol, EstadoUsuario estado) {
}