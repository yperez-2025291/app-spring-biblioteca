package com.yubiniperez.biblioteca.service;

import com.yubiniperez.biblioteca.dto.LoginRequest;
import com.yubiniperez.biblioteca.dto.LoginResponse;
import com.yubiniperez.biblioteca.dto.RegisterRequest;
import com.yubiniperez.biblioteca.dto.UsuarioResponse;
import com.yubiniperez.biblioteca.exception.BusinessRuleException;
import com.yubiniperez.biblioteca.exception.ResourceNotFoundException;
import com.yubiniperez.biblioteca.model.EstadoUsuario;
import com.yubiniperez.biblioteca.model.Rol;
import com.yubiniperez.biblioteca.model.Usuario;
import com.yubiniperez.biblioteca.repository.UsuarioRepository;
import com.yubiniperez.biblioteca.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UsuarioResponse registrar(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Ya existe un usuario con el email " + request.email());
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setRol(Rol.LECTOR);                  // rol predeterminado según la matriz
        usuario.setEstado(EstadoUsuario.ACTIVO);

        Usuario guardado = usuarioRepository.save(usuario);
        return new UsuarioResponse(guardado.getId(), guardado.getNombre(), guardado.getEmail(),
                guardado.getRol(), guardado.getEstado());
    }

    public LoginResponse login(LoginRequest request) {
        // Lanza BadCredentialsException si el email o la contraseña no coinciden
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String token = jwtService.generarToken(usuario);
        return new LoginResponse(token, "Bearer", usuario.getEmail(), usuario.getRol().name(),
                jwtService.getExpirationMs());
    }
}