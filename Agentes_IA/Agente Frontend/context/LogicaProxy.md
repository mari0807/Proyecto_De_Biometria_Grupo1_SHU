# LogicaProxy (lógica fake del cliente)

## ¿Qué es este fichero?
Aquí se diseña la capa intermedia de la web (también llamada lógica fake / proxy).

La página web **no** debe llamar al servidor directamente.  
En medio hay una capa con funciones parecidas a las del backend (por ejemplo `iniciarSesion`).  
Por fuera parece lógica local; por dentro hace las llamadas HTTP.

## Rúbrica (Arquitectura y lógica — 20%)
- Debe existir una capa **fake/proxy** que la GUI invoca.
- Misma interfaz lógica que el backend (puede ser un subconjunto: no hace falta copiarlo todo).
- La GUI **no** hace HTTP directo; la proxy encapsula la comunicación.
- De cara a la interfaz, es independiente del detalle de transporte (HTTP, URLs, etc.).

## Cómo debe quedar el código
- Firmas iguales que en la lógica del backend (nombre, parámetros, retornos).
- Solo los métodos que use la GUI (subconjunto).
- Usa la notación de `Software_Engineering_Spec.md`.
- Cada método con cabecera de diseño entre `--------------------`.
- Tests de los métodos críticos de la proxy.
- De cara a la GUI es interfaz limpia de dominio; por debajo encapsula las llamadas al backend (implementación fake/proxy).
- La GUI nunca hace HTTP/fetch/WebSocket directo.
- El código debe coincidir con este diseño.

## Diseño

Subconjunto de `Autenticacion` que usan las pantallas de H10, H12 y H25.
Mismos nombres, parámetros y retornos que la lógica del backend. Por dentro llama a
`POST /api/registro`, `POST /api/login`, `POST /api/logout` y `GET /api/sesion`.
La GUI no ve URLs ni `fetch`.

```text
nombre: Text, apellidos: Text, email: Text, password: Text --> registrar() --> ResultadoAuth
email: Text, password: Text --> iniciarSesion() --> ResultadoAuth
token: Text --> cerrarSesion() --> B
token: Text --> comprobarSesion() --> ResultadoAuth
nombre: Text, apellidos: Text, email: Text, password: Text, foto_base64: Text --> actualizarPerfil() --> ResultadoAuth
```

`iniciarSesion` guarda en el almacenamiento de la pestaña el token y el `UsuarioPublico`.
`cerrarSesion` borra esa credencial antes de avisar al servidor, y devuelve verdadero
cuando la credencial local ha quedado eliminada.
`actualizarPerfil` llama a `PUT /api/perfil` y, si acierta, actualiza el usuario guardado en la sesión local.
Si otra llamada de autenticación termina después, su respuesta se descarta y no pisa la sesión.
