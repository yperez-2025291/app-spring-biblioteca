INSERT INTO usuarios (nombre, email, password, estado, rol) VALUES
('Administrador',  'admin@biblioteca.com',   '$2b$10$6SVlZlW3Hc2MRGm10Xsh7eBfS98VjioiRHcR4OV5EAipYtl2APtTi', 'ACTIVO', 'ADMIN'),
('Bibliotecario',  'biblio@biblioteca.com',  '$2b$10$YGC2I4eIcmRnKOpUyTpqK.tjI7t9991WcXHWV610IkBiSYXaXCYVe', 'ACTIVO', 'BIBLIOTECARIO'),
('Lector Uno',     'lector1@biblioteca.com', '$2b$10$7zKtR9I6p2sbzjkrga8AsuD1DXHcnk2M/b4ul.wJXGIDxJvK0hjlO', 'ACTIVO', 'LECTOR'),
('Lector Dos',     'lector2@biblioteca.com', '$2b$10$7zKtR9I6p2sbzjkrga8AsuD1DXHcnk2M/b4ul.wJXGIDxJvK0hjlO', 'ACTIVO', 'LECTOR')
ON CONFLICT (email) DO NOTHING;

INSERT INTO libros (isbn, titulo, autor, categoria, stock_total, stock_disponible, activo) VALUES
('9780132350884', 'Clean Code',                  'Robert C. Martin', 'Programación', 5, 5, TRUE),
('9780134685991', 'Effective Java',              'Joshua Bloch',     'Programación', 3, 3, TRUE),
('9780596009205', 'Head First Design Patterns',  'Eric Freeman',     'Programación', 2, 2, TRUE),
('9788437604947', 'Cien años de soledad',        'Gabriel García Márquez', 'Novela', 3, 2, TRUE),
('9780307474728', 'El principito',               'Antoine de Saint-Exupéry', 'Novela', 1, 1, TRUE)
ON CONFLICT (isbn) DO NOTHING;

-- Préstamo vencido de prueba para la regla de sanción (lector2 con "Cien años de soledad")
INSERT INTO prestamos (usuario_id, libro_id, fecha_prestamo, fecha_devolucion_esperada, estado)
SELECT u.id, l.id, CURRENT_DATE - 20, CURRENT_DATE - 6, 'ACTIVO'
FROM usuarios u, libros l
WHERE u.email = 'lector2@biblioteca.com'
  AND l.isbn = '9788437604947'
  AND NOT EXISTS (SELECT 1 FROM prestamos);