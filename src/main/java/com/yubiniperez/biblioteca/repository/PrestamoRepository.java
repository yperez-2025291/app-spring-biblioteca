package com.yubiniperez.biblioteca.repository;

import com.yubiniperez.biblioteca.model.EstadoPrestamo;
import com.yubiniperez.biblioteca.model.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    // Préstamos no devueltos (ACTIVO o ATRASADO) de un usuario
    long countByUsuarioIdAndEstadoNot(Long usuarioId, EstadoPrestamo estado);

    // Préstamos no devueltos y vencidos de un usuario
    @Query("""
           SELECT p FROM Prestamo p
           WHERE p.usuario.id = :usuarioId
             AND p.estado <> com.yubiniperez.biblioteca.model.EstadoPrestamo.DEVUELTO
             AND p.fechaDevolucionEsperada < :hoy
           """)
    List<Prestamo> findVencidosDeUsuario(@Param("usuarioId") Long usuarioId, @Param("hoy") LocalDate hoy);

    // Todos los vencidos (endpoint /atrasados)
    @Query("""
           SELECT p FROM Prestamo p
           WHERE p.estado <> com.yubiniperez.biblioteca.model.EstadoPrestamo.DEVUELTO
             AND p.fechaDevolucionEsperada < :hoy
           ORDER BY p.fechaDevolucionEsperada
           """)
    List<Prestamo> findAtrasados(@Param("hoy") LocalDate hoy);

    List<Prestamo> findByUsuarioIdOrderByFechaPrestamoDesc(Long usuarioId);
}