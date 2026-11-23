# Interfaz web (GUI)

## ¿Qué es este fichero?
Aquí está el **diseño ya dado** de las pantallas de la web.
El agente **no** inventa pantallas: implementa según ## Diseño.
Notación lógica: `Software_Engineering_Spec.md`. La GUI solo llama a `LogicaProxy`.

## Buenas prácticas
- Sin HTTP/`fetch` en pantallas.
- Identidad visual del producto.
- El código debe coincidir con este diseño.

## Cómo debe quedar el código
- Pantallas → `LogicaProxy` → API.

## Diseño

Identidad: QuéAire, logo, DM Sans / Manrope, `#155b91` / `#198db1`.
Entrada: `index.html` / `landing.html` redirigen a `user.html`.

### PaginaUsuario (`user.html`)

```text
                 --------- PaginaUsuario ------------------
                 |
                 | proxy: LogicaProxy
                 |
                 |
                 | mostrar_acceso() -->
                 |
                 |
email: Text,
password: Text --> iniciar_sesion() -->
                 |
                 |
nombre: Text,
apellidos: Text,
email: Text,
password: Text --> registrar() -->
                 |
                 |
                 | mostrar_exito_registro() -->
                 |
                 |
                 | mostrar_recuperacion() -->
                 |
                 |
                 | abrir_menu_perfil() -->
                 |
                 |
                 | ir_a_perfil() -->
                 |
                 |
token: Text   --> cerrar_sesion() -->
                 |
                 |
                 | obtener_puntos_mapa() -->
                 |
                 |
id_nodo: N   --> obtener_detalle_zona() -->
                 |
                 |
                 | ir_a_admin() -->
                 |
                 --------------------------------------
```

Login OK → `admin.html` si rol `admin`, si no sigue en `user.html`.
Registro OK → pantalla «¡Te has registrado con éxito!» y entra.
Recuperar: solo aviso a soporte (sin API). Biometría visible, no autentica.

### PaginaPerfil (`perfil.html`)

```text
                 --------- PaginaPerfil ------------------
                 |
                 | proxy: LogicaProxy
                 | desde: Text
                 |
                 |
                 | cargar() -->
                 |
                 |
nombre: Text,
apellidos: Text,
email: Text,
password: Text,
password_nueva: Text,
foto_base64: Text,
quitar_foto: B --> guardar() -->
                 |
                 |
password: Text --> borrar_cuenta() -->
                 |
                 |
token: Text   --> cerrar_sesion() -->
                 |
                 |
                 | volver() -->
                 |
                 --------------------------------------
```

Exige sesión. `desde` = `user` | `admin`.

### PaginaAdmin (`admin.html`)

```text
                 --------- PaginaAdmin ------------------
                 |
                 | proxy: LogicaProxy
                 |
                 |
token: Text   --> comprobar_acceso() -->
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
id_usuario: N --> generar_password() -->
                 |
                 |
                 | ver_pagina_ciudadana() -->
                 |
                 |
token: Text   --> cerrar_sesion() -->
                 |
                 --------------------------------------
```

Solo rol `admin`; si no → `user.html#login` o `user.html`.
