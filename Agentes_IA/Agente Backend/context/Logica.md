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
(Completar aquí el diseño lógico: métodos, entradas, salidas y reglas.)
