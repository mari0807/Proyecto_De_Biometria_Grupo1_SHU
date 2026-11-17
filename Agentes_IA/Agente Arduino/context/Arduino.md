# Sensor / Arduino

## ¿Qué es este fichero?
Aquí se describe el diseño del programa del nodo sensor (placa / firmware).

## Flujo típico
1. La placa lee o simula una medida de contaminante.
2. Aplica correcciones si el diseño lo pide (offset, ganancia, temperatura/humedad…).
3. Emite por Bluetooth LE / iBeacon para que el móvil la capture.
4. No habla con el servidor ni con la web: eso lo hace el móvil.

## Alcance
- Firmware del nodo
- Emisión BLE
- Sin HTTP, sin BBDD, sin UI web/Android

## Qué documentar aquí
- Gas / valor que se emite
- UUID, major, minor (o el formato que uséis)
- Si emite una vez o cada X tiempo
- Componentes del diseño (emisora, medidor, LED, puerto serie, etc.)
- Qué hay que quitar de la plantilla porque no se usa
- Concurrencia: cuidado con `loop`, interrupciones y callbacks BLE si comparten variables/buffers

## Diseño
(Completar aquí el diseño real del nodo.)
