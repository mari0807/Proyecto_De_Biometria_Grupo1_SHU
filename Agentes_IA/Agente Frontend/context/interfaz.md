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
(Completar aquí wireframes / descripción de pantallas.)
