# Sistema de Gestión de Biblioteca Universitaria

API REST para administrar el catálogo de libros, los usuarios y los préstamos de una biblioteca universitaria. Incluye autenticación con **JWT**, autorización por roles con **Spring Security**, persistencia en **PostgreSQL** y reglas de negocio transaccionales para préstamos, devoluciones y sanciones.


## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 17 o superior |
| Framework | Spring Boot 4.1.1 |
| Seguridad | Spring Security, JWT (JJWT 0.13.0), BCrypt |
| Persistencia | Spring Data JPA, Hibernate, PostgreSQL |
| Validación | Jakarta Bean Validation |
| Vistas (previstas) | Thymeleaf, HTML5, CSS3 |
| Construcción | Maven (con Maven Wrapper) |

---

## Arquitectura

Aplicación única con **arquitectura en capas**. Cada capa tiene una sola responsabilidad:

```text
Cliente (Postman / navegador)
   │   Authorization: Bearer <JWT>
   ▼
JwtAuthenticationFilter   → valida firma y expiración del token, carga el rol
   ▼
SecurityConfig            → decide si ese rol puede acceder a la ruta (401 / 403)
   ▼
Controller                → recibe la petición y valida el body (DTOs)
   ▼
Service                   → reglas de negocio y transacciones (@Transactional)
   ▼
Repository                → acceso a datos (Spring Data JPA)
   ▼
PostgreSQL
```

### Estructura del proyecto

```text
src/main/java/com/yubiniperez/biblioteca/
├── BibliotecaApplication.java
├── controller/    AuthController, LibroController, PrestamoController
├── service/       AuthService, LibroService, PrestamoService
├── repository/    UsuarioRepository, LibroRepository, PrestamoRepository
├── model/         Usuario, Libro, Prestamo y enums (Rol, EstadoUsuario, EstadoPrestamo)
├── dto/           Requests y responses (las entidades nunca se exponen)
├── security/      JwtService, JwtAuthenticationFilter, CustomUserDetailsService, SecurityConfig
└── exception/     Excepciones personalizadas y GlobalExceptionHandler

src/main/resources/
├── application.properties
├── schema.sql     Creación de tablas
└── data.sql       Datos iniciales (usuarios y libros de prueba)
```

---

## Requisitos previos

- **JDK 17** o superior
- **PostgreSQL** (probado con la versión 18)
- **Maven** (opcional: el proyecto incluye `mvnw`)
- **Postman** para probar los endpoints (opcional)

---

## Instalación y ejecución

### 1. Crear la base de datos

En `psql` o en pgAdmin:

```sql
CREATE DATABASE biblioteca_db;
```

Las tablas y los datos iniciales se crean automáticamente al arrancar la aplicación (`schema.sql` y `data.sql`).

### 2. Configurar `src/main/resources/application.properties`

```properties
server.port=8081

spring.datasource.url=jdbc:postgresql://localhost:5432/biblioteca_db
spring.datasource.username=postgres
spring.datasource.password=TU_CONTRASEÑA

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.sql.init.mode=always

# El secreto debe tener 32 caracteres o más
jwt.secret=CAMBIA_ESTE_SECRETO_POR_UNO_PROPIO_DE_32_CARACTERES_O_MAS
jwt.expiration-ms=7200000
```

> Cambia `spring.datasource.password` y `jwt.secret` por valores propios. No subas credenciales reales al repositorio.

### 3. Ejecutar

```bash
# Linux / macOS / Git Bash
./mvnw spring-boot:run

# Windows (CMD o PowerShell)
mvnw.cmd spring-boot:run
```

También puede ejecutarse desde IntelliJ IDEA corriendo `BibliotecaApplication`.

La API queda disponible en `http://localhost:8081`.

---

## Usuarios de prueba

Se crean automáticamente al iniciar. Son datos de prueba para desarrollo.

| Rol | Email | Contraseña | Observación |
|---|---|---|---|
| ADMIN | `admin@biblioteca.com` | `Admin123*` | |
| BIBLIOTECARIO | `biblio@biblioteca.com` | `Biblio123!` | |
| LECTOR | `lector1@biblioteca.com` | `Lector123!` | Sin préstamos |
| LECTOR | `lector2@biblioteca.com` | `Lector123!` | Tiene un préstamo vencido (para probar la sanción) |

Todas las contraseñas se almacenan con **BCrypt**.

---

## Seguridad

- **Autenticación stateless** con JWT: el servidor no guarda sesiones (`SessionCreationPolicy.STATELESS`).
- **Token:** firmado con HS256, expira en 2 horas. Incluye el email (`sub`), el `userId` y el `rol`.
- **`JwtAuthenticationFilter`** (`OncePerRequestFilter`): valida el token en cada petición y carga el rol en el contexto de seguridad.
- **`SecurityFilterChain`** con CSRF deshabilitado (no aplica en una API stateless) y autorización por ruta y rol.
- **`PasswordEncoder`:** `BCryptPasswordEncoder`.
- Respuestas de error en JSON: **401** (token ausente, inválido o expirado) y **403** (rol sin permiso).
- El registro público **siempre crea usuarios con rol `LECTOR`**: el body no acepta un campo `rol`.

### Cómo autenticarse

1. Hacer login en `POST /api/v1/auth/login`.
2. Copiar el valor de `token` de la respuesta.
3. Enviarlo en cada petición protegida:

```text
Authorization: Bearer <token>
```

---

## Endpoints

Base URL: `http://localhost:8081`

### Autenticación y registro (`/api/v1/auth`)

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Público | Registra un usuario con rol predeterminado `LECTOR` |
| POST | `/api/v1/auth/login` | Público | Autentica credenciales y retorna el token JWT |

### Gestión de libros (`/api/v1/libros`)

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/v1/libros` | Autenticado | Lista libros con filtros opcionales por título y categoría, y paginación |
| GET | `/api/v1/libros/{id}` | Autenticado | Detalle de un libro |
| POST | `/api/v1/libros` | ADMIN | Registra un libro |
| PUT | `/api/v1/libros/{id}` | ADMIN | Actualiza datos o stock de un libro |
| DELETE | `/api/v1/libros/{id}` | ADMIN | Elimina un libro (eliminación lógica) |

Parámetros de `GET /api/v1/libros` (todos opcionales): `titulo`, `categoria`, `page` (por defecto 0) y `size` (por defecto 10, máximo 100).

### Gestión de préstamos (`/api/v1/prestamos`)

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/v1/prestamos` | BIBLIOTECARIO, ADMIN | Registra la salida de un libro (valida stock y límite de 3) |
| PATCH | `/api/v1/prestamos/{id}/devolucion` | BIBLIOTECARIO, ADMIN | Registra la entrega, actualiza el stock y marca como `DEVUELTO` |
| GET | `/api/v1/prestamos/mis-prestamos` | LECTOR | Historial y préstamos activos del usuario autenticado |
| GET | `/api/v1/prestamos/atrasados` | BIBLIOTECARIO, ADMIN | Lista los préstamos que superaron su fecha de devolución |

### Ejemplos

**Login**

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "admin@biblioteca.com",
  "password": "Admin123*"
}
```

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "email": "admin@biblioteca.com",
  "rol": "ADMIN",
  "expiraEnMs": 7200000
}
```

**Crear libro** (ADMIN)

```http
POST /api/v1/libros
Authorization: Bearer <token>
Content-Type: application/json

{
  "isbn": "9781234567890",
  "titulo": "Libro de Prueba",
  "autor": "Autor Prueba",
  "categoria": "Pruebas",
  "stockTotal": 4
}
```

El `stockDisponible` lo calcula el sistema; si el cliente lo envía, se ignora.

**Registrar préstamo** (BIBLIOTECARIO o ADMIN)

```http
POST /api/v1/prestamos
Authorization: Bearer <token>
Content-Type: application/json

{
  "usuarioId": 3,
  "libroId": 1
}
```

```json
{
  "id": 10,
  "usuarioId": 3,
  "usuarioNombre": "Lector Uno",
  "libroId": 1,
  "libroTitulo": "Clean Code",
  "fechaPrestamo": "2026-10-07",
  "fechaDevolucionEsperada": "2026-10-21",
  "fechaDevolucionReal": null,
  "estado": "ACTIVO"
}
```

### Códigos de respuesta

| Código | Cuándo |
|---|---|
| 200 | Consulta o actualización correcta |
| 201 | Recurso creado |
| 204 | Eliminación correcta (sin cuerpo) |
| 400 | Validación fallida o regla de negocio incumplida |
| 401 | Token ausente, inválido o expirado; o credenciales incorrectas |
| 403 | El rol no tiene permiso para la acción |
| 404 | Recurso inexistente |

---

## Reglas de negocio

| # | Regla | Comportamiento |
|---|---|---|
| 1 | **Disponibilidad** | No se presta un libro con `stockDisponible = 0`. Al prestar baja en 1; al devolver sube en 1 |
| 2 | **Máximo de préstamos** | Un lector no puede tener más de **3 préstamos activos** a la vez |
| 3 | **Duración** | Cada préstamo dura **14 días**; la fecha esperada de devolución se calcula automáticamente |
| 4 | **Sanciones** | Si un lector con un préstamo vencido intenta pedir otro, sus préstamos vencidos pasan a `ATRASADO`, el lector pasa a `SANCIONADO` y el préstamo se rechaza |

Detalles de la regla 4:

- La sanción **se guarda aunque el préstamo sea rechazado**: el servicio usa `@Transactional(noRollbackFor = SancionException.class)` para que la excepción no deshaga el cambio de estado.
- Un lector `SANCIONADO` no puede recibir nuevos préstamos.
- **La sanción se levanta sola:** al devolver un libro, si al lector ya no le quedan préstamos vencidos, vuelve a `ACTIVO`.

Otras validaciones al registrar un préstamo: el usuario y el libro deben existir, el libro debe estar activo, y **solo los usuarios con rol `LECTOR` pueden recibir préstamos**.

### Transacciones y concurrencia

- El préstamo y el descuento de stock se guardan en **una sola transacción** (`@Transactional`): se confirman juntos o no se guarda nada.
- El libro se consulta con **bloqueo pesimista** al prestar, devolver y actualizar. Así, dos préstamos simultáneos del último ejemplar no dejan el stock en negativo.

---

## Modelo de datos

```text
usuarios (id, nombre, email UNIQUE, password, estado, rol)
    │ 1
    │
    │ N
prestamos (id, usuario_id, libro_id, fecha_prestamo,
           fecha_devolucion_esperada, fecha_devolucion_real, estado)
    │ N
    │
    │ 1
libros (id, isbn UNIQUE, titulo, autor, categoria,
        stock_total, stock_disponible, activo)
```

| Entidad | Valores permitidos |
|---|---|
| `Rol` | `ADMIN`, `BIBLIOTECARIO`, `LECTOR` |
| `EstadoUsuario` | `ACTIVO`, `SANCIONADO` |
| `EstadoPrestamo` | `ACTIVO`, `DEVUELTO`, `ATRASADO` |

Relaciones JPA: `Prestamo` → `Usuario` y `Prestamo` → `Libro` (`@ManyToOne`), con los lados inversos `@OneToMany(mappedBy)` en `Usuario` y `Libro`.

---

## Manejo de errores

Un `@RestControllerAdvice` global responde siempre con la misma estructura:

```json
{
  "timestamp": "2026-10-07T14:55:12.8995619",
  "status": 400,
  "error": "Business Rule",
  "message": "El usuario ya tiene 3 préstamos activos."
}
```

Excepciones personalizadas: `ResourceNotFoundException` (404), `BusinessRuleException` (400) y `SancionException` (400, hereda de `BusinessRuleException`). Los errores de validación de los DTOs responden `400` con el detalle por campo.

---

## Pruebas

### Colección de Postman

Importar `app-spring-biblioteca.postman_collection.json` (carpeta `postman/`). Contiene tres carpetas: **autenticacion y registro**, **gestion de libros** y **gestion de prestamos**.

- Los requests de cada carpeta cubren los 11 endpoints de la matriz.
- La subcarpeta **pruebas adicionales** repite esos mismos endpoints con otros tokens o datos para validar autorización (401 y 403), validaciones y las cuatro reglas de negocio.
- Los tokens y los IDs se guardan automáticamente en variables de colección; no hay que pegarlos a mano.
- Ejecutar con el **Collection Runner**, de arriba hacia abajo.

### Script de pruebas funcionales y de estrés (`test-api6.sh`)

Requisitos: **Git Bash** (en Windows), **`jq`** instalado y la aplicación corriendo en `http://localhost:8081`.

```bash
chmod +x test-api6.sh
./test-api6.sh
```

El script registra un lector, obtiene tokens JWT, crea un libro, consulta el catálogo, valida el `403` de un LECTOR y lanza una prueba de concurrencia sobre el catálogo.

> Al repetir el script, el registro del lector y el libro creado darán `400` por duplicado. Es el comportamiento esperado.

### Restablecer los datos de prueba

Si se ejecutaron pruebas de préstamos y se quiere volver al estado inicial, correr en la base `biblioteca_db`:

```sql
DELETE FROM prestamos;
UPDATE libros SET stock_disponible = stock_total;
UPDATE usuarios SET estado = 'ACTIVO';

INSERT INTO prestamos (usuario_id, libro_id, fecha_prestamo, fecha_devolucion_esperada, estado)
SELECT u.id, l.id, CURRENT_DATE - 20, CURRENT_DATE - 6, 'ACTIVO'
FROM usuarios u, libros l
WHERE u.email = 'lector2@biblioteca.com' AND l.isbn = '9788437604947';

UPDATE libros SET stock_disponible = stock_total - 1 WHERE isbn = '9788437604947';
```