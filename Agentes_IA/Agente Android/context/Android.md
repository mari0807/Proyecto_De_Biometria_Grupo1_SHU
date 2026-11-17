# App Android

## ¿Qué es este fichero?
Aquí se describe el diseño de la aplicación móvil Android del proyecto.

## Flujo típico del sistema
1. El usuario abre la app (registro / inicio de sesión si hace falta).
2. El móvil escucha el nodo sensor por Bluetooth LE (BLE).
3. Parsea la trama / beacon (nombre, uuid, major, minor, etc.).
4. Envía la medida al servidor con un POST JSON (según `Comunicacion.md` del Backend).
5. Muestra pantallas: mapa, datos del sensor, perfil, tips, etc.

## Capas recomendadas
- UI (Activities / Fragments): lo que ve y pulsa el usuario.
- Cliente / servicios: parseo BLE, `ClienteREST`, autenticación / sesión.
- La UI no debe mezclar SQL ni demasiados detalles de transporte.

## Qué documentar aquí
- Pantallas de la app y navegación
- Permisos (Bluetooth, ubicación, etc.)
- Cómo se parsea el beacon / trama
- Contrato de envío al servidor (misma API que Backend)
- Datos de demo / valores de prueba si los hay
- Thread-safety: BLE, red y UI en hilos distintos; no tocar vistas fuera del hilo principal

## Diseño
(Completar aquí el diseño real de la app.)
