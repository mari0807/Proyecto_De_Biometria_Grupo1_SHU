# App Android — Proyecto de Biometría (Grupo 1)

Aplicación móvil del proyecto **QueAire**: **inicio de sesión** y **registro**
contra el backend real del repositorio (`servidor/`) y, tras autenticarse, el
**mapa de calidad del aire** (H16, portado de la primera app del grupo), con
cierre de sesión.

| Tarjeta Trello | Pantalla | Fichero principal |
|---|---|---|
| **H11** — Iniciar sesión | `LoginActivity` | `app/src/main/java/com/grupo1/biometria/ui/LoginActivity.java` |
| **H29** — Registro | `RegisterActivity` | `app/src/main/java/com/grupo1/biometria/ui/RegisterActivity.java` |
| **H16** — Mapa de calidad del aire | `MainActivity` + paquete `mapa/` | `app/src/main/java/com/grupo1/biometria/ui/MainActivity.java` |
| **Admin** — Gestión de usuarios | `AdminActivity` + `data/ClienteAdmin` | `app/src/main/java/com/grupo1/biometria/ui/AdminActivity.java` |

## Requisitos

- Android Studio (o JDK 17+ y Android SDK con `compileSdk 36`).
- El backend del proyecto en marcha (ver más abajo).
- Móvil y PC en la **misma red WiFi**.

## Compilar y ejecutar

```powershell
cd android
$env:JAVA_HOME = "C:\Users\david\.jdks\jbr-17.0.14"   # si no está definido
.\gradlew.bat test              # tests unitarios (52)
.\gradlew.bat assembleDebug     # genera app\build\outputs\apk\debug\app-debug.apk
```

Instalar en un dispositivo conectado por USB (con depuración activada):

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Poner en marcha el backend

1. Ejecutar como **administrador** `activar-xampp.bat` (raíz del repo): crea los
   alias de Apache a través de un enlace ASCII (`C:\xampp\queaire_repo`, necesario
   si la ruta del repo lleva acentos), los enlaces de `htdocs` y la regla de
   firewall del puerto 80.
2. En el panel de XAMPP: **Start Apache** y **Start MySQL**.
3. Importar `bd/schema.sql` y `bd/seed.sql` si aún no lo está (ver `bd/README.md`).

Con eso la API queda en `http://localhost/api` y, desde el móvil en la misma
WiFi, en `http://TU_IP/api` (la IP del PC se ve con `ipconfig`).

Alternativa sin Apache, con el servidor PHP integrado (puerto 8080):

```powershell
C:\xampp\php\php.exe -S 0.0.0.0:8080 -t servidor\public
```

### Configurar el servidor en la app

La pantalla de inicio de sesión tiene un campo **Servidor**: cada miembro del
equipo escribe la URL del backend de **su** PC y la app la recuerda (no hay que
recompilar; el registro usa la misma URL):

- **Móvil físico (misma WiFi):** la IP del PC, p. ej. `http://192.168.0.200`.
- **Emulador con Apache (puerto 80):** `http://10.0.2.2` (valor por defecto).
- **Servidor PHP integrado (8080):** las mismas URLs terminadas en `:8080`.

## Usuarios de prueba

De `bd/seed.sql` (contraseña en texto plano, el backend guarda el hash bcrypt):

| Correo | Contraseña |
|---|---|
| `admin@email.com` | `1234` |
| `cliente@email.com` | `1234` |

> Nota: `1234` sirve para iniciar sesión porque la política de contraseña
> (8+, mayúscula, minúscula, número y signo) solo se aplica **al registrarse**.

## Estructura

```
app/src/main/java/com/grupo1/biometria/
├── ui/                     # Pantallas (Activities, XML en res/layout/)
│   ├── LoginActivity.java      # H11
│   ├── RegisterActivity.java   # H29
│   ├── MainActivity.java       # Destino de rol "usuario" + logout
│   └── AdminActivity.java      # Destino de rol "admin" + logout
├── data/                   # Capa de datos y transporte (sin Android en la lógica)
│   ├── AuthRepository.java         # Contrato: cambiar aquí para otra fuente
│   ├── AuthRepositoryRemoto.java   # Implementación contra la API REST
│   ├── AuthRepositoryProvider.java # Holder perezoso thread-safe (singleton)
│   ├── HttpClienteRest.java        # HttpURLConnection + JSON
│   ├── ClienteRest.java            # Interfaz → doble de pruebas en los tests
│   ├── SesionManager.java          # Sesión en memoria + SharedPreferences
│   ├── PreferenciasAlmacenSesion.java / AlmacenSesion.java
│   ├── ClienteAdmin.java / AdminJson.java / UsuarioAdmin.java   # Gestión de usuarios del admin
│   ├── RespuestaAuth.java / EstadoAuth.java   # Mapeo HTTP → estados de la app
│   ├── RespuestaHttp.java / Usuario.java
│   └── Reloj.java                  # Reloj inyectable (tests de caducidad)
├── mapa/                   # H16: mapa de calidad del aire (portado de la 1ª app)
│   ├── ClienteMapa.java            # GET /api/mapa y /api/mapa/zona/{id} con Bearer
│   ├── MapaJson.java / Formato.java / Paleta.java
│   ├── model/                      # PuntoMapa, ZonaDetalle, Medicion
│   ├── CalorOverlay.java / PuntosOverlay.java   # Capas de osmdroid
│   ├── LeyendaView.java / GraficaEvolucionView.java
│   └── DetalleZonaBottomSheet.java # Panel inferior de zona (H18)
└── util/
    ├── Validador.java    # Reglas de campos = reglas del backend
    ├── Rol.java          # usuario.rol == "admin" → panel de administración
    └── JwtUtil.java      # Lee `exp` del token (igual que logicaProxy.js)
```

**Separación UI ↔ datos:** las Activities solo hablan con `AuthRepository`;
el transporte (`ClienteRest`) va por detrás, de forma que sustituirlo o doblarlo
en tests no toca la interfaz. La red se ejecuta siempre en un hilo de fondo y
el callback se entrega en el hilo principal (Handler), para no bloquear la UI.

## Contrato con la API

Implementado según `servidor/src/communication/ApiAuth.php`:

| Endpoint | Método | Cuerpo | Éxito | Errores |
|---|---|---|---|---|
| `/api/registro` | POST | `nombre, apellidos, email, password` | 200 `{ok, mensaje, usuario}` | 400 datos, 409 correo registrado |
| `/api/login` | POST | `email, password` | 200 `{ok, mensaje, usuario, token}` | 400 datos, 401 credenciales |
| `/api/logout` | POST | cabecera `Bearer` | 200 | — |
| `/api/sesion` | GET | cabecera `Bearer` | 200 `{ok, mensaje, usuario}` | 401 sesión inválida |
| `/api/usuarios` | GET | cabecera `Bearer` (admin) | 200 `{ok, usuarios}` | 401 sesión, 403 no admin |
| `/api/usuarios` | POST | `nombre, apellidos, email, rol` | 200 `{ok, mensaje, password_temporal, usuario}` | 400 datos, 403 no admin, 409 correo |
| `/api/usuarios/{id}` | PUT | `nombre, apellidos, email, activo, rol` | 200 `{ok, mensaje, usuario}` | 400 datos (incl. autorreglas), 403, 409 |
| `/api/usuarios/{id}` | DELETE | cabecera `Bearer` | 200 | 400 (p. ej. propia cuenta), 403 |
| `/api/usuarios/{id}/password` | POST | cabecera `Bearer` | 200 `{ok, mensaje, password_temporal}` | 400, 403 |

- **Política de contraseña** (idéntica a `password.js`): 8–72 caracteres,
  mayúscula, minúscula, número y signo especial.
- **Sesión:** el backend emite un JWT con TTL de 10 minutos. La app guarda
  `token + usuario` (solo en memoria, salvo que la casilla «Mantener sesión
  iniciada» esté marcada, en cuyo caso va a SharedPreferences), comprueba la
  caducidad localmente con `JwtUtil` y la valida contra el servidor con
  `GET /api/sesion` al arrancar. Al cerrar sesión borra lo local **antes** de
  avisar al servidor (mismo criterio que `logicaProxy.js`).

## Diseño y detalle de los formularios

La interfaz reproduce las capturas de `Carpetas/Capturas` (`Iniciar sesion app.png`
y `Registro app.png`, que corresponden a las figuras 6 y 7 del PDF): fondo blanco,
flecha de retroceso, campos gris claro `#FAFAFA` con borde `#E8EDF0`, texto azul
oscuro `#193C55` y botón principal `#145A91`. Se han conservado los apartados
añadidos al diseño original y se ha **eliminado el botón de huella digital** del
inicio de sesión, tal como se pidió.

Textos del diseño: «Te damos la bienvenida» / «Consulta tu actividad y conecta
con tu sensor personal.», «Crea tu cuenta» / «Participa y guarda las mediciones
de tu sensor», «¿Has olvidado tu contraseña?», «¿Aún no tienes cuenta? Regístrate»,
«¿Ya tienes una cuenta? Inicia sesión» y «Tus datos se almacenan de forma segura».

- **Login (H11):** el contenido está centrado verticalmente en la pantalla.
  Aviso de error justo debajo de la contraseña y encima de
  «Mantener sesión iniciada», para que el teclado no lo tape. Cuando el servidor
  rechaza las credenciales se borra la contraseña y se cierra el teclado; el correo
  se conserva. Sin flecha de retroceso ni acceso con huella.
- **Olvidar contraseña:** igual que la sección `#recuperar` de `user.html`: abre
  un diálogo «¿Olvidaste tu contraseña?» con el correo `shu@gmail.com`, el botón
  «Escribir a soporte» (abre el cliente de correo con el asunto preparado) y
  «Volver a iniciar sesión». El backend no tiene endpoint de recuperación.
- **Registro (H29):** aparte de correo y contraseña del diseño, se mantienen los
  apartados añadidos: nombre, apellidos, confirmación de contraseña, requisitos en
  vivo (✓ / ○) con los mismos textos que `password.js`, casilla **«He leído y
  acepto los términos de uso»** obligatoria y enlace «Ver términos de uso» que
  abre un diálogo con la opción de aceptarlos. Igual que `acceso.js`, tras crear
  la cuenta se **inicia sesión automáticamente** y se abre el panel según el rol.
- `android:windowSoftInputMode="adjustResize"` en ambas Activities para que el
  `ScrollView` deje sitio al teclado.

## Comprobación del rol

El backend incluye `usuario.rol` en la respuesta del login (valores `admin` y
`usuario`, ver `Autenticacion.php`). La app aplica el mismo criterio que
`destino()` en `Pagina web/js/acceso.js`:

- `admin` → `AdminActivity` (panel de administración).
- `usuario` → `MainActivity`.

`Rol.esAdmin()` centraliza la comparación (sin distinguir mayúsculas) y se usa
tanto al entrar por login como al restaurar una sesión con «Mantener sesión
iniciada». `MainActivity` y `AdminActivity` se protegen entre sí para que un
usuario con el rol equivocado no pueda ver el panel ajeno.

## Tests (70)

| Fichero | Cubre |
|---|---|
| `ValidadorTest` (16) | Vacíos, formato de correo, política de contraseña y estados de sus 5 requisitos |
| `JwtUtilTest` (6) | Lectura de `exp`, tokens malformados, caducidad |
| `RespuestaAuthTest` (10) | Mapeo de respuestas HTTP → estados (login/registro/sesión/red) |
| `SesionManagerTest` (9) | Mantener sesión, restauración, caducidad, datos corruptos, cierre |
| `AuthRepositoryRemotoTest` (11) | Rutas y cuerpos de la API, sesión, cierre sin red |
| `RolTest` (4) | Enrutamiento `admin`/`usuario`, mayúsculas, espacios y valores ausentes |
| `PaletaTest` (3), `FormatoTest` (3), `MapaJsonTest` (4) | Colores, formatos y parsing del mapa (portados con el H16) |
| `AdminJsonTest` (4) | Listado de usuarios, contraseña temporal y mensajes de la administración |

## Comprobación web ↔ app

Ambas clientes usan el mismo backend (`http://127.0.0.1:8080`) y el mismo
contrato JSON, así que comparten cuentas, contraseñas y roles. Para probar la web
en local se puede servir su carpeta con:

```powershell
C:\xampp\php\php.exe -S 127.0.0.1:8081 -t "..\Pagina web"
```

Prueba cruzada realizada (usuarios de prueba creados en la BD):

| Sentido | Cuenta | Resultado |
|---|---|---|
| Registro en **web** → login en **app** | `webapp1@ejemplo.com` / `WebApp1!` | La web creó el usuario (id 8) e inició sesión; la app entró en `MainActivity` como «Web App». |
| Registro en **app** → login en **web** | `appweb1@ejemplo.com` / `AppWeb1!` | La app creó el usuario (id 9) e inició sesión; la web entró en `user.html` con la misma cuenta. |

También se comprobó el diálogo de recuperación de la app contra la sección
`#recuperar` de `user.html` (mismo texto y mismo correo de soporte).

## Pendiente

- **H17** (perfil) y demás pantallas del tablero.
- `servidor/src/business_logic/Token.php` emite tokens con TTL corto (600 s);
  si se amplía, solo hay que revalidar el comportamiento de `SesionManager`.
