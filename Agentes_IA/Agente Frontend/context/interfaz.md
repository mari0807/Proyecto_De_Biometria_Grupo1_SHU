# Interfaz web (GUI)

## ¿Qué es este fichero?
Aquí se describe el diseño de las pantallas de la web: qué se ve, qué se puede hacer y cómo encaja con la identidad visual del producto.

## Qué debe incluir
- Lista de pantallas (login, registro, mapa, panel de administración, etc.)
- Contenido de cada pantalla (campos, botones, tablas, mapa…)
- Identidad visual: nombre del producto, logo, tipografía, paleta de colores
- Comportamiento a alto nivel (qué pasa al pulsar cada botón)
- Diseño responsive si aplica (móvil / escritorio)

## Cómo debe quedar el código de la interfaz
- La interfaz **no** llama al PHP/API directamente.
- La interfaz pide datos y acciones a través de `LogicaProxy`.
- El código de pantallas **no** debe contener HTTP/fetch/WebSocket directo.
- Debe respetarse la identidad visual del producto.
- El código debe coincidir con este diseño.

## Diseño

Identidad ya existente en `Pagina web`: QuéAire, logo, DM Sans / Manrope, paleta de `index.css`.
Estas tres historias solo usan pantallas que ya están: el acceso de portadores en `index.html`
y el panel `admin.html`. No se añade otra página.

La cabecera de `index.html` abre el acceso con el botón **Login**.

### Login (`index.html#portadores`)

Dos formularios:

- **Iniciar sesión :** correo electrónico y contraseña. Al enviar, la GUI llama a `iniciarSesion`. Si falla, el mensaje de error queda en el formulario. Si acierta, redirige según el rol devuelto. Solo hay dos roles: `admin` y `cliente`.
  - `admin` → `admin.html`
  - `cliente` → `index.html`
- **Crear cuenta :** nombre, apellidos, correo y contraseña. La GUI avisa si falta un obligatorio o el correo no tiene forma de email, y enseña el mensaje del servidor si el correo ya existe o el registro se confirma. No inicia sesión sola.

El botón de biometría del prototipo no forma parte de estas historias y no autentica.

### Sesión abierta 

Con sesión, admin y cliente ven en la cabecera un **menú de perfil** (avatar y nombre, más grande y con borde azul; sin el rol). Al abrirlo aparecen:
- **Editar perfil** (implementado): popup con foto, nombre, apellidos, correo y confirmación con contraseña actual.
- **Mis notificaciones** (solo visible, aún no desarrollado).
- **Cerrar sesión**.

Si el rol es `admin`, en la vista ciudadana también aparece el botón **Volver al panel →**. En `admin.html` queda el botón **Ver página ciudadana →**. Ambos usan el azul de acento de la paleta (`#198db1`). No se muestra el texto «Panel de administración» en la cabecera.

`admin.html` no se muestra si `comprobarSesion` falla o si el rol no es `admin`: se vuelve al inicio de sesión. Un `cliente` que abra esa URL vuelve a `index.html` sin perder su sesión.
