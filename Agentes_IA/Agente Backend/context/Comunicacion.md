# Comunicación (API)

## ¿Qué es este fichero?
Aquí se define el contrato de la API: cómo web y Android hablan con el servidor  
(rutas, métodos HTTP, JSON de entrada/salida, errores).

## Reglas de la capa communication
- Recibe peticiones HTTP (GET, POST, PUT, DELETE…).
- Llama a la **lógica de negocio**.
- **No** escribe SQL directamente.
- No inventes endpoints fuera de lo escrito aquí.

## Qué debe quedar documentado por cada endpoint
- Ruta (ejemplo: `/api/login`)
- Método (GET/POST/…)
- Body / parámetros
- Respuesta correcta
- Errores posibles

## Cómo debe quedar el código
Flujo: `communication` → `business_logic` → `database`.
- Esta capa puede llamar a la lógica.
- Esta capa **no** escribe SQL.
- Define el transporte externo (REST u otro) y los endpoints que invocan la lógica.
- El código debe coincidir con este diseño (mismas rutas, bodies y respuestas).

## Diseño

Contrato de H10, H12, H25 y edición de perfil. La capa traduce `CodigoAuth` a HTTP y no escribe SQL.
El cuerpo de error y de acierto es JSON. `usuario` nunca incluye `password_hash` ni la columna `foto_perfil`; sí puede incluir `foto` (URL pública).

| CodigoAuth | HTTP |
| :--- | :--- |
| `OK` | 200 |
| `DATOS_INVALIDOS` | 400 |
| `CORREO_REGISTRADO` | 409 |
| `CREDENCIALES_INVALIDAS`, `CUENTA_INACTIVA`, `SESION_INVALIDA` | 401 |

### POST `/api/registro`

Body: `{ "nombre": Text, "apellidos": Text, "email": Text, "password": Text }`

Acierto 200: `{ "ok": true, "mensaje": "Cuenta creada correctamente.", "usuario": UsuarioPublico }`

Errores: 400 si faltan campos, el email no tiene formato o la contraseña no es válida; 409 si el correo ya está registrado. `ok` es `false` y `mensaje` explica el motivo.

### POST `/api/login`

Body: `{ "email": Text, "password": Text }`

Acierto 200: `{ "ok": true, "mensaje": Text, "token": Text, "usuario": UsuarioPublico }`

Error 401: `{ "ok": false, "mensaje": Text }` (credenciales incorrectas o cuenta inactiva).

### POST `/api/logout`

Cabecera opcional: `Authorization: Bearer <token>`

Acierto 200: `{ "ok": true, "mensaje": "Sesión cerrada." }`

El cliente borra la credencial guardada aunque la respuesta tarde. El servidor no almacena la sesión.

### GET `/api/sesion`

Cabecera: `Authorization: Bearer <token>`

Acierto 200: `{ "ok": true, "usuario": UsuarioPublico }`

Error 401 si no hay token, está caducado o la cuenta ya no está activa. Sirve para no abrir páginas privadas sin autenticación (H25).

### PUT `/api/perfil`

Cabecera: `Authorization: Bearer <token>`

Body: `{ "nombre": Text, "apellidos": Text, "email": Text, "password": Text, "foto_base64": Text }`

`foto_base64` es opcional. Si viene, es un `data:image/png|jpeg|webp;base64,...`. La comunicación guarda el fichero en `public/perfiles/` y pasa la ruta relativa a la lógica.

Acierto 200: `{ "ok": true, "mensaje": "Perfil actualizado correctamente.", "usuario": UsuarioPublico }`

Errores: 400 datos inválidos; 401 sesión inválida o contraseña incorrecta; 409 correo ya registrado.

### Gestión de usuarios (solo rol `admin`)

Implementados en `ApiAuth.php` y usados por la web (`adminUsuarios.js`) y por la
app Android (`ClienteAdmin`). Cabecera en todos: `Authorization: Bearer <token>`
de una cuenta con rol `admin`; si no, 403 `sin_permiso`.

| Ruta | Método | Body | Acierto | Errores |
| :--- | :--- | :--- | :--- | :--- |
| `/api/usuarios` | GET | — | 200 `{ok, mensaje, usuarios: [{id_usuario, nombre, apellidos, email, rol, activo, foto}]}` | 401 sesión, 403 |
| `/api/usuarios` | POST | `{nombre, apellidos, email, rol}` | 200 `{ok, mensaje, password_temporal, usuario}` | 400 datos, 403, 409 correo |
| `/api/usuarios/{id}` | PUT | `{nombre, apellidos, email, activo, rol}` | 200 `{ok, mensaje, usuario}` | 400 datos, 403, 409 |
| `/api/usuarios/{id}` | DELETE | — | 200 `{ok, mensaje}` | 400, 403 |
| `/api/usuarios/{id}/password` | POST | — | 200 `{ok, mensaje, password_temporal, usuario}` | 400, 403 |

`rol` solo admite `usuario` y `admin`. Autorreglas de `Autenticacion.php`: un
admin no puede darse de baja (`activo=false`), quitarse su propio rol ni borrar
su propia cuenta. `password_temporal` solo se devuelve al crear o regenerar:
no existe recuperación (solo hay hash).
