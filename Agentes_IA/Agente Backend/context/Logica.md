# Lógica de negocio

## ¿Qué es este fichero?
Aquí está el **diseño ya dado** de la lógica del servidor.
El agente **no** inventa métodos: implementa según ## Diseño.
Notación: `Software_Engineering_Spec.md`.

## Buenas prácticas
- Esta capa accede a la BBDD.
- **Sin HTTP**, sin códigos de estado, sin frameworks web.
- Independiente del canal (local/remoto).
- Tests de métodos críticos.

## Cómo debe quedar el código
- Un fichero/clase: `servidor/src/business_logic/Logica.php`.
- Cabeceras `--------------------` por método.
- Alineada con `BBDD.md`.
- Cero imports hacia comunicación.
- El código debe coincidir con este diseño.

## Diseño

Implementación: `Logica.php` (+ `ResultadoAuth.php`, `Token.php`).
JWT sin tabla `SESION`. TTL por defecto: **600 s** en `servidor/config.php`.

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
  SESION_INVALIDA,
  SIN_PERMISO
}

ResultadoAuth = (
  ok: B,
  codigo: CodigoAuth,
  mensaje: Text,
  usuario: UsuarioPublico,
  token: Text
)

ResultadoListaUsuarios = (
  ok: B,
  codigo: CodigoAuth,
  mensaje: Text,
  usuarios: [ UsuarioPublico ]
)

ResultadoPasswordAdmin = (
  ok: B,
  codigo: CodigoAuth,
  mensaje: Text,
  password_temporal: Text,
  usuario: UsuarioPublico
)

ResultadoMapa = (
  ok: B,
  codigo: CodigoAuth,
  mensaje: Text,
  puntos: [ PuntoMapa ]
)

ResultadoZona = (
  ok: B,
  codigo: CodigoAuth,
  mensaje: Text,
  zona: ZonaDetalle
)
```

Si `ok` es falso: `usuario` vacío y `token` vacío. Sin `password_hash` ni columna `foto_perfil` en `UsuarioPublico`.

### Logica

```text
                 --------- Logica ------------------
                 |
                 | repositorio: RepositorioUsuarios
                 | mediciones: RepositorioMediciones
                 | secreto: Text
                 | token_ttl: N
                 | historial_maximo: N
                 |
                 |
repositorio: RepositorioUsuarios,
secreto: Text,
token_ttl: N,
mediciones: RepositorioMediciones
             --> Logica() -->
                 |
                 |
nombre: Text,
apellidos: Text,
email: Text,
password: Text --> registrar() -->
resultado: ResultadoAuth <--
                 |
                 |
email: Text,
password: Text --> iniciarSesion() -->
resultado: ResultadoAuth <--
                 |
                 |
token: Text   --> cerrarSesion() -->
ok: B        <--
                 |
                 |
token: Text   --> comprobarSesion() -->
resultado: ResultadoAuth <--
                 |
                 |
token: Text,
nombre: Text,
apellidos: Text,
email: Text,
password: Text,
foto_perfil: Text,
password_nueva: Text,
quitar_foto: B --> actualizarPerfil() -->
resultado: ResultadoAuth <--
                 |
                 |
token: Text,
password: Text --> borrarCuenta() -->
resultado: ResultadoAuth <--
                 |
                 |
token: Text   --> listarUsuarios() -->
resultado: ResultadoListaUsuarios <--
                 |
                 |
token: Text,
nombre: Text,
apellidos: Text,
email: Text,
rol: Text    --> crearUsuarioAdmin() -->
resultado: ResultadoPasswordAdmin <--
                 |
                 |
token: Text,
id_usuario: N,
nombre: Text,
apellidos: Text,
email: Text,
activo: B,
rol: Text    --> actualizarUsuarioAdmin() -->
resultado: ResultadoAuth <--
                 |
                 |
token: Text,
id_usuario: N --> borrarUsuarioAdmin() -->
resultado: ResultadoAuth <--
                 |
                 |
token: Text,
id_usuario: N --> generarPasswordAdmin() -->
resultado: ResultadoPasswordAdmin <--
                 |
                 |
                 | puntos() -->
resultado: ResultadoMapa <--
                 |
                 |
id_nodo: N   --> detalleZona() -->
resultado: ResultadoZona <--
                 |
                 --------------------------------------
```

### Reglas

1. **Registro.** Obligatorios nombre/apellidos/email/password; email único; hash bcrypt; rol `usuario`.
2. **Login.** Solo `activo = TRUE`; JWT; actualiza `ultimo_acceso` (UTC).
3. **cerrarSesion.** Valida token; no borra filas (sin `SESION`).
4. **actualizarPerfil.** Exige password actual; foto vacía = no cambia; `quitar_foto` borra; binario lo guarda communication.
5. **borrarCuenta.** Soft delete (`activo = FALSE`).
6. **Admin.** Solo rol `admin`; `rol` ∈ {admin, usuario}; no puede bajarse/quitarse rol/borrarse a sí mismo; `password_temporal` al crear/regenerar.
7. **puntos.** Última medición de cada nodo activo + umbral/color.
8. **detalleZona.** Nodo + actual + historial (máx. `historial_maximo`); si no existe → no encontrado.
