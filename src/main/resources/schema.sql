CREATE TABLE IF NOT EXISTS usuarios (
    id        BIGSERIAL PRIMARY KEY,
    nombre    VARCHAR(100) NOT NULL,
    email     VARCHAR(150) NOT NULL UNIQUE,
    password  VARCHAR(100) NOT NULL,
    estado    VARCHAR(20)  NOT NULL,
    rol       VARCHAR(20)  NOT NULL
);

CREATE TABLE IF NOT EXISTS libros (
    id               BIGSERIAL PRIMARY KEY,
    isbn             VARCHAR(20)  NOT NULL UNIQUE,
    titulo           VARCHAR(200) NOT NULL,
    autor            VARCHAR(150) NOT NULL,
    categoria        VARCHAR(100) NOT NULL,
    stock_total      INTEGER      NOT NULL,
    stock_disponible INTEGER      NOT NULL,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS prestamos (
    id                        BIGSERIAL PRIMARY KEY,
    usuario_id                BIGINT      NOT NULL REFERENCES usuarios(id),
    libro_id                  BIGINT      NOT NULL REFERENCES libros(id),
    fecha_prestamo            DATE        NOT NULL,
    fecha_devolucion_esperada DATE        NOT NULL,
    fecha_devolucion_real     DATE,
    estado                    VARCHAR(20) NOT NULL
);