# App Android

## ¿Qué es este fichero?
Aquí está el **diseño ya dado** de la app Android.
El agente **no** inventa pantallas ni clases: implementa según ## Diseño.

## Buenas prácticas
- UI (Activities) + clientes de red/sesión. Sin SQL.
- Misma API que `Comunicacion.md` del Backend.
- BLE/red/UI en hilos distintos; no tocar vistas fuera del hilo principal.
- El código debe coincidir con este diseño (cabeceras `--------------------`).

## Cómo debe quedar el código
- Pantallas: `LoginActivity`, `RegisterActivity`, `MainActivity`, `AdminActivity`.
- Auth/sesión: `AuthRepository` / `AuthRepositoryRemoto`, `SesionManager`.
- Red: `ClienteRest` / `HttpClienteRest`, `ClienteMapa`, `ClienteAdmin`.
- Flujo: login → mapa (`usuario`) o panel (`admin`).

## Diseño

Implementación bajo `android/app/src/main/java/com/grupo1/biometria/`.
Sin trama iBeacon BLE en esta app (el mapa usa la API).

Navegación: `LoginActivity` → `RegisterActivity` (vuelve al login) |
`LoginActivity` → `MainActivity` (rol usuario) | `AdminActivity` (rol admin).

### LoginActivity

```text
                 --------- LoginActivity ------------------
                 |
                 | repositorio: AuthRepository
                 | email: Text
                 | password: Text
                 | servidor: Text
                 | mantener: B
                 |
                 |
                 | onCreate() -->
                 |
                 |
                 | intentar_inicio_sesion() -->
                 |
                 |
                 | comprobar_sesion_guardada() -->
                 |
                 |
                 | ir_a_pantalla_principal() -->
                 |
                 |
                 | mostrar_recuperacion() -->
                 |
                 --------------------------------------
```

### RegisterActivity

```text
                 --------- RegisterActivity ------------------
                 |
                 | repositorio: AuthRepository
                 | nombre: Text
                 | apellidos: Text
                 | email: Text
                 | password: Text
                 |
                 |
                 | onCreate() -->
                 |
                 |
                 | intentar_registro() -->
                 |
                 |
email: Text  --> volver_a_login() -->
                 |
                 --------------------------------------
```

### MainActivity

```text
                 --------- MainActivity ------------------
                 |
                 | repositorio: AuthRepository
                 | cliente: ClienteMapa
                 |
                 |
                 | onCreate() -->
                 |
                 |
                 | onResume() -->
                 |
                 |
                 | onPause() -->
                 |
                 |
                 | onDestroy() -->
                 |
                 |
                 | cargar_puntos() -->
                 |
                 |
id_nodo: N   --> abrir_detalle() -->
                 |
                 |
                 | cerrar_sesion_local() -->
                 |
                 |
                 | volver_a_login() -->
                 |
                 --------------------------------------
```

### AdminActivity

```text
                 --------- AdminActivity ------------------
                 |
                 | repositorio: AuthRepository
                 | cliente: ClienteAdmin
                 |
                 |
                 | onCreate() -->
                 |
                 |
                 | listar_usuarios() -->
                 |
                 |
nombre: Text,
apellidos: Text,
email: Text,
rol: Text    --> crear_usuario() -->
                 |
                 |
id_usuario: N,
nombre: Text,
apellidos: Text,
email: Text,
activo: B,
rol: Text    --> actualizar_usuario() -->
                 |
                 |
id_usuario: N --> borrar_usuario() -->
                 |
                 |
id_usuario: N --> regenerar_password() -->
                 |
                 |
                 | cerrar_sesion() -->
                 |
                 --------------------------------------
```

### AuthRepository

```text
                 --------- AuthRepository ------------------
                 |
                 |
email: Text,
password: Text,
mantener: B  --> iniciar_sesion() -->
respuesta: RespuestaAuth <--
                 |
                 |
nombre: Text,
apellidos: Text,
email: Text,
password: Text --> registrar() -->
respuesta: RespuestaAuth <--
                 |
                 |
                 | cerrar_sesion() -->
respuesta: RespuestaAuth <--
                 |
                 |
                 | comprobar_sesion() -->
respuesta: RespuestaAuth <--
                 |
                 |
              B <-- hay_sesion() <--
                 |
                 |
        Usuario <-- usuario() <--
                 |
                 --------------------------------------
```

### AuthRepositoryRemoto

```text
                 --------- AuthRepositoryRemoto ------------------
                 |
                 | cliente: ClienteRest
                 | sesion: SesionManager
                 |
                 |
cliente: ClienteRest,
sesion: SesionManager
             --> AuthRepositoryRemoto() -->
                 |
                 |
email: Text,
password: Text,
mantener: B  --> iniciar_sesion() -->
respuesta: RespuestaAuth <--
                 |
                 |
nombre: Text,
apellidos: Text,
email: Text,
password: Text --> registrar() -->
respuesta: RespuestaAuth <--
                 |
                 |
                 | cerrar_sesion() -->
respuesta: RespuestaAuth <--
                 |
                 |
                 | comprobar_sesion() -->
respuesta: RespuestaAuth <--
                 |
                 |
              B <-- hay_sesion() <--
                 |
                 |
        Usuario <-- usuario() <--
                 |
                 --------------------------------------
```

### SesionManager

```text
                 --------- SesionManager ------------------
                 |
                 | almacen: AlmacenSesion
                 | reloj: Reloj
                 | token: Text
                 | usuario: Usuario
                 |
                 |
almacen: AlmacenSesion,
reloj: Reloj --> SesionManager() -->
                 |
                 |
token: Text,
usuario: Usuario,
mantener: B  --> abrir() -->
                 |
                 |
usuario: Usuario --> actualizar_usuario() -->
                 |
                 |
                 | cerrar() -->
                 |
                 |
              B <-- hay_sesion() <--
                 |
                 |
           Text <-- token() <--
                 |
                 |
        Usuario <-- usuario() <--
                 |
                 --------------------------------------
```

### ClienteRest

```text
                 --------- ClienteRest ------------------
                 |
                 |
ruta: Text,
cuerpo: Text,
token: Text  --> post_json() -->
respuesta: RespuestaHttp <--
                 |
                 |
ruta: Text,
token: Text  --> get_json() -->
respuesta: RespuestaHttp <--
                 |
                 --------------------------------------
```

### HttpClienteRest

```text
                 --------- HttpClienteRest ------------------
                 |
                 | url_base: Text
                 |
                 |
url_base: Text --> HttpClienteRest() -->
                 |
                 |
ruta: Text,
cuerpo: Text,
token: Text  --> post_json() -->
respuesta: RespuestaHttp <--
                 |
                 |
ruta: Text,
token: Text  --> get_json() -->
respuesta: RespuestaHttp <--
                 |
                 --------------------------------------
```

### ClienteMapa

```text
                 --------- ClienteMapa ------------------
                 |
                 | url_base: Text
                 | token: Text
                 |
                 |
url_base: Text,
token: Text  --> ClienteMapa() -->
                 |
                 |
                 | puntos() -->
puntos: [ PuntoMapa ] <--
                 |
                 |
id_nodo: N   --> zona() -->
zona: ZonaDetalle <--
                 |
                 |
                 | cerrar() -->
                 |
                 --------------------------------------
```

### ClienteAdmin

```text
                 --------- ClienteAdmin ------------------
                 |
                 | url_base: Text
                 | token: Text
                 |
                 |
url_base: Text,
token: Text  --> ClienteAdmin() -->
                 |
                 |
                 | listar() -->
usuarios: [ UsuarioAdmin ] <--
                 |
                 |
nombre: Text,
apellidos: Text,
email: Text,
rol: Text    --> crear() -->
password_temporal: Text <--
                 |
                 |
id_usuario: N,
nombre: Text,
apellidos: Text,
email: Text,
activo: B,
rol: Text    --> actualizar() -->
mensaje: Text <--
                 |
                 |
id_usuario: N --> borrar() -->
mensaje: Text <--
                 |
                 |
id_usuario: N --> nueva_password() -->
password_temporal: Text <--
                 |
                 --------------------------------------
```
