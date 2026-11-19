# Lógica de negocio

## ¿Qué es este fichero?
Aquí se diseña la capa de lógica del servidor: las funciones que hacen el trabajo real  
(registrar usuario, iniciar sesión, guardar medición, obtener datos, etc.).

## Rúbrica (Arquitectura y lógica — 20%)
- Esta capa es la que accede a la BBDD.
- **Sin HTTP**, sin códigos de estado (200, 404…), sin frameworks web.
- Independiente del canal de acceso (local/remoto): da igual quién la llame (API, test u otro).
- Debe haber tests automáticos de los métodos críticos (rúbrica Buenas Prácticas 25%).

## Cómo debe quedar el código
- Usa la notación de `Software_Engineering_Spec.md` (tipos `N`, `Z`, `R`, `B`, `Text`, firmas, cajas de clase…).
- Cada método con su diseño entre rayas `--------------------` en el código.
- Alineada con `BBDD.md` (mismas entidades/campos).
- **100% independiente de comunicación:** prohibido `HttpRequest`, `HttpResponse`, status codes, routing, headers, sockets y frameworks web.
- **Cero dependencias** hacia la capa de comunicación (ni imports ni llamadas).
- El código de la lógica debe coincidir con este diseño.

## Diseño

Cubre H10 (registro), H12 (inicio de sesión) y H25 (cierre de sesión).
El resto del dominio sigue pendiente. No hay tabla de sesiones: la credencial es un JWT
(ver `BBDD.md`, Design Clarifications).

```text
UsuarioPublico = (
  id_usuario: N,
  nombre: Text,
  apellidos: Text,
  email: Text,
  rol: Text,
  foto: Text
)

CodigoAuth = {
  OK,
  DATOS_INVALIDOS,
  CORREO_REGISTRADO,
  CREDENCIALES_INVALIDAS,
  CUENTA_INACTIVA,
  SESION_INVALIDA
}

ResultadoAuth = (
  ok: B,
  codigo: CodigoAuth,
  mensaje: Text,
  usuario: UsuarioPublico,
  token: Text
)
```

Si `ok` es falso, `usuario` va vacío (`id_usuario` 0 y textos vacíos) y `token` es texto vacío.
`UsuarioPublico` no incluye `password_hash` ni la columna `foto_perfil`.
El campo `foto` es la URL pública derivada (vacía si no hay imagen).

```text
                 --------- Autenticacion ------------------
                 |
                 | repositorio: RepositorioUsuarios
                 | secreto: Text
                 |
nombre: Text,
apellidos: Text,
email: Text,
password: Text --> registrar() --> ResultadoAuth
                 |
                 |
email: Text,
password: Text --> iniciarSesion() --> ResultadoAuth
                 |
                 |
token: Text   --> cerrarSesion() --> B
                 |
                 |
token: Text   --> comprobarSesion() --> ResultadoAuth
                 |
                 |
token: Text,
nombre: Text,
apellidos: Text,
email: Text,
password: Text,
foto_perfil: Text --> actualizarPerfil() --> ResultadoAuth
                 |
                 ------------------------------------------------
```

### Reglas

1. **Registro (H10).** Obligatorios: `nombre`, `apellidos`, `email`, `password` (los NOT NULL que aporta la persona; `id_rol`, `activo` y `fecha_registro` los pone el sistema). Email con el mismo criterio que el `CHECK` de `USUARIO` (`texto@texto.texto`, sin espacios, máximo 254). `password` no vacía y como máximo 72 bytes (límite de bcrypt). Hash con `password_hash()` de PHP (`PASSWORD_BCRYPT`), columna `password_hash` ≤ 255. Rol por defecto: `usuario`. Email único sin distinguir mayúsculas. Si ya existe: se informa, no se crea otra cuenta. Si todo es válido: mensaje de confirmación.
2. **Inicio de sesión (H12).** Solo `activo = TRUE`. Contraseña con `password_verify`. Si el email no existe o la contraseña no coincide: un único mensaje de error, sin decir cuál de los dos falla. Si la contraseña es correcta y la cuenta está inactiva: se informa de que no está activa. En el acierto se actualiza `ultimo_acceso` en UTC y se emite un token. La respuesta no lleva `password_hash` ni `foto_perfil`.
3. **Cierre de sesión (H25).** `cerrarSesion` comprueba el token y devuelve si era válido. No borra filas: no existe tabla de sesiones. La credencial guardada en el cliente se elimina allí. Un token caducado o de una cuenta inactiva no supera `comprobarSesion`.
4. **Editar perfil.** Obligatorios: `nombre`, `apellidos`, `email` y la contraseña actual. Si la contraseña no coincide: error de credenciales. Si el email nuevo ya existe en otra cuenta: conflicto. `foto_perfil` vacío deja la foto igual; si viene ruta, se guarda esa ruta relativa (el binario lo deja la capa de comunicación en el sistema de ficheros).
5. **Concurrencia.** El alta y la edición usan transacción; el `UNIQUE` de `email` cierra la carrera entre dos correos iguales. No hay estado mutable estático en la lógica. `ultimo_acceso` es un `UPDATE` de una sola fila.

### Design Clarifications

| Decisión | Motivo |
| :--- | :--- |
| El formulario de registro pide apellidos | `USUARIO.apellidos` es NOT NULL. Sin ese dato no se puede crear la cuenta. |
| JWT de 8 horas, sin tabla `SESION` | `BBDD.md` fija autenticación sin sesiones en base de datos. Invalidar el token en servidor exigiría esa tabla. |
| `cerrarSesion` no revoca el JWT en el servidor | Misma restricción. Lo que se invalida es la credencial almacenada en el navegador. |
| `comprobarSesion` relee `activo` | Una baja lógica debe impedir el acceso aunque el token no haya caducado. |
