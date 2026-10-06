package com.yubiniperez.biblioteca.service;

import com.yubiniperez.biblioteca.dto.LibroRequest;
import com.yubiniperez.biblioteca.dto.LibroResponse;
import com.yubiniperez.biblioteca.exception.BusinessRuleException;
import com.yubiniperez.biblioteca.exception.ResourceNotFoundException;
import com.yubiniperez.biblioteca.model.Libro;
import com.yubiniperez.biblioteca.repository.LibroRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibroService {

    private final LibroRepository libroRepository;

    public LibroService(LibroRepository libroRepository) {
        this.libroRepository = libroRepository;
    }

    @Transactional(readOnly = true)
    public PagedModel<LibroResponse> listar(String titulo, String categoria, int page, int size) {
        int pagina = Math.max(page, 0);
        int tamano = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by("id"));

        Page<LibroResponse> resultado = libroRepository
                .buscar(normalizar(titulo), normalizar(categoria), pageable)
                .map(this::toResponse);
        return new PagedModel<>(resultado);
    }

    @Transactional(readOnly = true)
    public LibroResponse obtener(Long id) {
        return toResponse(buscarActivo(id));
    }

    @Transactional
    public LibroResponse crear(LibroRequest request) {
        if (libroRepository.existsByIsbn(request.isbn())) {
            throw new BusinessRuleException("Ya existe un libro con el ISBN " + request.isbn());
        }
        Libro libro = new Libro();
        copiarDatos(libro, request);
        libro.setStockTotal(request.stockTotal());
        libro.setStockDisponible(request.stockTotal());   // al crear, todo el stock está disponible
        libro.setActivo(true);
        return toResponse(libroRepository.save(libro));
    }

    @Transactional
    public LibroResponse actualizar(Long id, LibroRequest request) {
        // Con bloqueo para no pisar un préstamo que se esté registrando al mismo tiempo
        Libro libro = libroRepository.findActivoParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id " + id));

        if (!libro.getIsbn().equals(request.isbn()) && libroRepository.existsByIsbn(request.isbn())) {
            throw new BusinessRuleException("Ya existe un libro con el ISBN " + request.isbn());
        }

        // stockDisponible se ajusta por la diferencia entre el total nuevo y el anterior
        int diferencia = request.stockTotal() - libro.getStockTotal();
        int nuevoDisponible = libro.getStockDisponible() + diferencia;
        if (nuevoDisponible < 0) {
            int prestados = libro.getStockTotal() - libro.getStockDisponible();
            throw new BusinessRuleException("No se puede dejar el stock total en " + request.stockTotal()
                    + ": hay " + prestados + " ejemplar(es) prestado(s).");
        }

        copiarDatos(libro, request);
        libro.setStockTotal(request.stockTotal());
        libro.setStockDisponible(nuevoDisponible);
        return toResponse(libroRepository.save(libro));
    }

    @Transactional
    public void eliminar(Long id) {
        Libro libro = buscarActivo(id);
        libro.setActivo(false);   // eliminación lógica: conserva el historial de préstamos
        libroRepository.save(libro);
    }

    // ---------- auxiliares ----------

    private Libro buscarActivo(Long id) {
        return libroRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id " + id));
    }

    private void copiarDatos(Libro libro, LibroRequest request) {
        libro.setIsbn(request.isbn().trim());
        libro.setTitulo(request.titulo().trim());
        libro.setAutor(request.autor().trim());
        libro.setCategoria(request.categoria().trim());
    }

    private String normalizar(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private LibroResponse toResponse(Libro l) {
        return new LibroResponse(l.getId(), l.getIsbn(), l.getTitulo(), l.getAutor(), l.getCategoria(),
                l.getStockTotal(), l.getStockDisponible());
    }
}