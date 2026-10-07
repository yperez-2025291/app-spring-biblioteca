package com.yubiniperez.biblioteca.service;

import com.yubiniperez.biblioteca.dto.PrestamoRequest;
import com.yubiniperez.biblioteca.dto.PrestamoResponse;
import com.yubiniperez.biblioteca.exception.BusinessRuleException;
import com.yubiniperez.biblioteca.exception.ResourceNotFoundException;
import com.yubiniperez.biblioteca.exception.SancionException;
import com.yubiniperez.biblioteca.model.EstadoPrestamo;
import com.yubiniperez.biblioteca.model.EstadoUsuario;
import com.yubiniperez.biblioteca.model.Libro;
import com.yubiniperez.biblioteca.model.Prestamo;
import com.yubiniperez.biblioteca.model.Rol;
import com.yubiniperez.biblioteca.model.Usuario;
import com.yubiniperez.biblioteca.repository.LibroRepository;
import com.yubiniperez.biblioteca.repository.PrestamoRepository;
import com.yubiniperez.biblioteca.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PrestamoService {

    private static final int MAX_PRESTAMOS_ACTIVOS = 3;
    private static final int DIAS_PRESTAMO = 14;

    private final PrestamoRepository prestamoRepository;
    private final LibroRepository libroRepository;
    private final UsuarioRepository usuarioRepository;

    public PrestamoService(PrestamoRepository prestamoRepository,
                           LibroRepository libroRepository,
                           UsuarioRepository usuarioRepository) {
        this.prestamoRepository = prestamoRepository;
        this.libroRepository = libroRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // noRollbackFor: si se lanza SancionException, los cambios (SANCIONADO y ATRASADO)
    // SÍ se guardan. Cualquier otra excepción hace rollback normal.
    @Transactional(noRollbackFor = SancionException.class)
    public PrestamoResponse registrar(PrestamoRequest request) {
        LocalDate hoy = LocalDate.now();

        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id " + request.usuarioId()));

        // Con bloqueo: evita que dos préstamos simultáneos dejen el stock en negativo
        Libro libro = libroRepository.findActivoParaActualizar(request.libroId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Libro no encontrado con id " + request.libroId()));

        if (usuario.getRol() != Rol.LECTOR) {
            throw new BusinessRuleException("Solo se pueden registrar préstamos a usuarios con rol LECTOR.");
        }

        if (usuario.getEstado() == EstadoUsuario.SANCIONADO) {
            throw new BusinessRuleException(
                    "El usuario está SANCIONADO: debe devolver sus libros vencidos antes de pedir otro préstamo.");
        }

        // Regla 4: préstamos vencidos -> ATRASADO + usuario SANCIONADO + rechazo
        List<Prestamo> vencidos = prestamoRepository.findVencidosDeUsuario(usuario.getId(), hoy);
        if (!vencidos.isEmpty()) {
            vencidos.forEach(p -> p.setEstado(EstadoPrestamo.ATRASADO));
            prestamoRepository.saveAll(vencidos);
            usuario.setEstado(EstadoUsuario.SANCIONADO);
            usuarioRepository.save(usuario);
            throw new SancionException("El usuario tiene " + vencidos.size()
                    + " préstamo(s) vencido(s). Fue marcado como SANCIONADO y no se registró el préstamo.");
        }

        // Regla 2: máximo 3 préstamos activos
        long activos = prestamoRepository.countByUsuarioIdAndEstadoNot(usuario.getId(), EstadoPrestamo.DEVUELTO);
        if (activos >= MAX_PRESTAMOS_ACTIVOS) {
            throw new BusinessRuleException("El usuario ya tiene " + MAX_PRESTAMOS_ACTIVOS + " préstamos activos.");
        }

        // Regla 1: disponibilidad
        if (libro.getStockDisponible() <= 0) {
            throw new BusinessRuleException("No hay ejemplares disponibles de \"" + libro.getTitulo() + "\".");
        }

        // Regla 3: plazo de 14 días, calculado automáticamente
        Prestamo prestamo = new Prestamo();
        prestamo.setUsuario(usuario);
        prestamo.setLibro(libro);
        prestamo.setFechaPrestamo(hoy);
        prestamo.setFechaDevolucionEsperada(hoy.plusDays(DIAS_PRESTAMO));
        prestamo.setEstado(EstadoPrestamo.ACTIVO);

        libro.setStockDisponible(libro.getStockDisponible() - 1);

        libroRepository.save(libro);
        return toResponse(prestamoRepository.save(prestamo), hoy);
    }

    @Transactional
    public PrestamoResponse devolver(Long id) {
        LocalDate hoy = LocalDate.now();

        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo no encontrado con id " + id));

        if (prestamo.getEstado() == EstadoPrestamo.DEVUELTO) {
            throw new BusinessRuleException("El préstamo " + id + " ya fue devuelto.");
        }

        Libro libro = libroRepository.findByIdParaActualizar(prestamo.getLibro().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Libro del préstamo no encontrado"));

        prestamo.setFechaDevolucionReal(hoy);
        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        libro.setStockDisponible(libro.getStockDisponible() + 1);

        prestamoRepository.saveAndFlush(prestamo);
        libroRepository.save(libro);

        // Sanción tipo A: si ya no le quedan préstamos vencidos, el lector vuelve a ACTIVO
        Usuario usuario = prestamo.getUsuario();
        if (usuario.getEstado() == EstadoUsuario.SANCIONADO
                && prestamoRepository.findVencidosDeUsuario(usuario.getId(), hoy).isEmpty()) {
            usuario.setEstado(EstadoUsuario.ACTIVO);
            usuarioRepository.save(usuario);
        }

        return toResponse(prestamo, hoy);
    }

    @Transactional(readOnly = true)
    public List<PrestamoResponse> misPrestamos(String email) {
        LocalDate hoy = LocalDate.now();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return prestamoRepository.findByUsuarioIdOrderByFechaPrestamoDesc(usuario.getId())
                .stream()
                .map(p -> toResponse(p, hoy))
                .toList();
    }

    // No es readOnly: al detectar préstamos vencidos se guardan como ATRASADO
    @Transactional
    public List<PrestamoResponse> atrasados() {
        LocalDate hoy = LocalDate.now();
        List<Prestamo> lista = prestamoRepository.findAtrasados(hoy);
        lista.forEach(p -> p.setEstado(EstadoPrestamo.ATRASADO));
        prestamoRepository.saveAll(lista);

        return lista.stream().map(p -> toResponse(p, hoy)).toList();
    }

    // ---------- auxiliares ----------

    // Si no está devuelto y ya pasó la fecha esperada, se muestra como ATRASADO
    private EstadoPrestamo estadoEfectivo(Prestamo p, LocalDate hoy) {
        if (p.getEstado() != EstadoPrestamo.DEVUELTO && p.getFechaDevolucionEsperada().isBefore(hoy)) {
            return EstadoPrestamo.ATRASADO;
        }
        return p.getEstado();
    }

    private PrestamoResponse toResponse(Prestamo p, LocalDate hoy) {
        return new PrestamoResponse(
                p.getId(),
                p.getUsuario().getId(),
                p.getUsuario().getNombre(),
                p.getLibro().getId(),
                p.getLibro().getTitulo(),
                p.getFechaPrestamo(),
                p.getFechaDevolucionEsperada(),
                p.getFechaDevolucionReal(),
                estadoEfectivo(p, hoy));
    }
}