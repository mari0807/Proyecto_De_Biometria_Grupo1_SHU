# LogicaProxy (lógica fake del cliente)

## ¿Qué es este fichero?
Aquí está el **diseño ya dado** de la capa intermedia de la web.
La GUI **no** llama al servidor: pasa por esta capa. El agente **no** inventa métodos: implementa según ## Diseño.
Notación: `Software_Engineering_Spec.md` (mismos tipos que `Logica.md`).

## Buenas prácticas
- La GUI solo invoca la proxy; la proxy encapsula el HTTP.
- Misma interfaz lógica que el backend (subconjunto).
- De cara a la UI, sin URLs/`fetch`.

## Cómo debe quedar el código
- Firmas iguales que el backend en lo expuesto.
- Cabeceras `--------------------`.
- Tests de métodos críticos.
- El código debe coincidir con este diseño.

## Diseño

Implementación: `Pagina web/js/logicaProxy.js`.
Por dentro llama a `Comunicacion.md`. Tipos `UsuarioPublico`, `ResultadoAuth`, etc. = `Logica.md`.

```text
ResultadoListaProxy = (
  ok: B,
  mensaje: Text,
  usuarios: [ UsuarioPublico ]
)

ResultadoPasswordProxy = (
  ok: B,
  mensaje: Text,
  password_temporal: Text,
  usuario: UsuarioPublico
)

ResultadoUsuarioProxy = (
  ok: B,
  mensaje: Text,
  usuario: UsuarioPublico
)

ResultadoOkProxy = (
  ok: B,
  mensaje: Text
)

ResultadoMapaProxy = (
  ok: B,
  mensaje: Text,
  puntos: [ PuntoMapa ]
)

ResultadoZonaProxy = (
  ok: B,
  mensaje: Text,
  zona: ZonaDetalle
)
```

### LogicaProxy

```text
                 --------- LogicaProxy ------------------
                 |
                 | token: Text
                 | usuario: UsuarioPublico
                 |
                 |
                 | sesionActual() -->
usuario: UsuarioPublico <--
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
nombre: Text,
apellidos: Text,
email: Text,
password: Text,
password_nueva: Text,
foto_base64: Text,
quitar_foto: B --> actualizarPerfil() -->
resultado: ResultadoAuth <--
                 |
                 |
password: Text --> borrarCuenta() -->
resultado: ResultadoAuth <--
                 |
                 |
                 | listarUsuarios() -->
resultado: ResultadoListaProxy <--
                 |
                 |
nombre: Text,
apellidos: Text,
email: Text,
rol: Text    --> crearUsuarioAdmin() -->
resultado: ResultadoPasswordProxy <--
                 |
                 |
id_usuario: N,
nombre: Text,
apellidos: Text,
email: Text,
activo: B,
rol: Text    --> actualizarUsuarioAdmin() -->
resultado: ResultadoUsuarioProxy <--
                 |
                 |
id_usuario: N --> borrarUsuarioAdmin() -->
resultado: ResultadoOkProxy <--
                 |
                 |
id_usuario: N --> generarPasswordAdmin() -->
resultado: ResultadoPasswordProxy <--
                 |
                 |
                 | obtenerPuntosMapa() -->
resultado: ResultadoMapaProxy <--
                 |
                 |
id_nodo: N   --> obtenerDetalleZona() -->
resultado: ResultadoZonaProxy <--
                 |
                 |
ruta: Text   --> urlRecurso() -->
url: Text    <--
                 |
                 --------------------------------------
```

### Comportamiento

- `iniciarSesion` guarda token + `UsuarioPublico` en `sessionStorage`.
- `cerrarSesion` / `comprobarSesion` reciben `token` (paridad con backend).
- `cerrarSesion` borra credencial local antes de avisar al servidor.
- Admin y mapa usan Bearer de la sesión local.
- Respuestas de auth obsoletas no pisan la sesión.

### Tests

`Pagina web/js/tests/logicaProxyTest.mjs` — login, sesión, logout, `urlRecurso`.
