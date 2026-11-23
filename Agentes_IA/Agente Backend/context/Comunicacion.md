# Comunicación (API)

## ¿Qué es este fichero?
Aquí está el **diseño ya dado** del contrato API.
El agente **no** inventa endpoints: implementa según ## Diseño.
Capas: `communication` → `business_logic` → `database`. Notación lógica: `Software_Engineering_Spec.md` (tipos de `Logica.md`).

## Buenas prácticas
- Recibe HTTP y llama a la lógica.
- **No** escribe SQL.
- No inventes rutas fuera de lo escrito aquí.

## Cómo debe quedar el código
- Puede llamar a la lógica; no SQL.
- Traduce `CodigoAuth` → HTTP.
- El código debe coincidir con este diseño.

## Diseño

Implementación: `servidor/src/communication/` (`ApiAuth.php`, `ApiMapa.php`) + `servidor/public/index.php`.
`usuario` en JSON: `UsuarioPublico` (sin `password_hash` / sin columna `foto_perfil`).

```text
CodigoAuth → HTTP
  OK → 200
  DATOS_INVALIDOS → 400
  CORREO_REGISTRADO → 409
  CREDENCIALES_INVALIDAS | CUENTA_INACTIVA | SESION_INVALIDA → 401
  SIN_PERMISO → 403
  zona no encontrada → 404
```

### ApiAuth

```text
                 --------- ApiAuth ------------------
                 |
                 | logica: Logica
                 |
                 |
logica: Logica --> ApiAuth() -->
                 |
                 |
nombre: Text,
apellidos: Text,
email: Text,
password: Text --> registro() -->
resultado: ResultadoAuth <--
                 |  POST /api/registro
                 |
email: Text,
password: Text --> login() -->
resultado: ResultadoAuth <--
                 |  POST /api/login
                 |
token: Text   --> logout() -->
ok: B        <--
                 |  POST /api/logout
                 |
token: Text   --> sesion() -->
resultado: ResultadoAuth <--
                 |  GET /api/sesion
                 |
token: Text,
nombre: Text,
apellidos: Text,
email: Text,
password: Text,
password_nueva: Text,
foto_base64: Text,
quitar_foto: B --> perfil() -->
resultado: ResultadoAuth <--
                 |  PUT /api/perfil
                 |  (foto: communication guarda fichero y pasa ruta a lógica)
                 |
token: Text,
password: Text --> cuenta() -->
resultado: ResultadoAuth <--
                 |  DELETE /api/cuenta
                 |
token: Text   --> listar_usuarios() -->
resultado: ResultadoListaUsuarios <--
                 |  GET /api/usuarios  (solo admin)
                 |
token: Text,
nombre: Text,
apellidos: Text,
email: Text,
rol: Text    --> crear_usuario() -->
resultado: ResultadoPasswordAdmin <--
                 |  POST /api/usuarios
                 |
token: Text,
id_usuario: N,
nombre: Text,
apellidos: Text,
email: Text,
activo: B,
rol: Text    --> actualizar_usuario() -->
resultado: ResultadoAuth <--
                 |  PUT /api/usuarios/{id}
                 |
token: Text,
id_usuario: N --> borrar_usuario() -->
resultado: ResultadoAuth <--
                 |  DELETE /api/usuarios/{id}
                 |
token: Text,
id_usuario: N --> generar_password() -->
resultado: ResultadoPasswordAdmin <--
                 |  POST /api/usuarios/{id}/password
                 |
                 --------------------------------------
```

`rol` ∈ {`usuario`, `admin`}. Un admin no puede darse de baja, quitarse el rol ni borrarse.

### ApiMapa

```text
                 --------- ApiMapa ------------------
                 |
                 | logica: Logica
                 |
                 |
logica: Logica --> ApiMapa() -->
                 |
                 |
token: Text   --> puntos() -->
resultado: ResultadoMapa <--
                 |  GET /api/mapa
                 |
token: Text,
id_nodo: N   --> zona() -->
resultado: ResultadoZona <--
                 |  GET /api/mapa/zona/{id}
                 |
                 --------------------------------------
```
