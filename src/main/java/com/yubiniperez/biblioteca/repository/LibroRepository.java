package com.yubiniperez.biblioteca.repository;

import com.yubiniperez.biblioteca.model.Libro;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    boolean existsByIsbn(String isbn);

    Optional<Libro> findByIdAndActivoTrue(Long id);

    // Bloqueo pesimista: evita que dos préstamos simultáneos dejen el stock en negativo
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Libro l WHERE l.id = :id AND l.activo = true")
    Optional<Libro> findActivoParaActualizar(@Param("id") Long id);

    @Query("""
           SELECT l FROM Libro l
           WHERE l.activo = true
             AND (:titulo IS NULL OR LOWER(l.titulo) LIKE LOWER(CONCAT('%', CAST(:titulo AS string), '%')))
             AND (:categoria IS NULL OR LOWER(l.categoria) LIKE LOWER(CONCAT('%', CAST(:categoria AS string), '%')))
           """)
    Page<Libro> buscar(@Param("titulo") String titulo,
                       @Param("categoria") String categoria,
                       Pageable pageable);
}