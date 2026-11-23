# Arquitectura Backend

## ¿Qué es este fichero?
Aquí está el **diseño ya dado** de capas y dependencias del servidor.
El agente **no** inventa capas: implementa según ## Diseño.

## Buenas prácticas
- Capas: `communication` → `business_logic` → `database`.
- La lógica es quien accede a la BBDD.
- La lógica no conoce HTTP ni frameworks web.

## Cómo debe quedar el código
- Dependencias solo hacia abajo.
- Cero imports de communication en business_logic.

## Diseño

```text
cliente (Web | Android)
        |
        v
--------- communication ------------------
|
| ApiAuth
| ApiMapa
|
| (HTTP in / out; llama a lógica; no SQL)
|
------------------------------------------
        |
        v
--------- business_logic ------------------
|
| Logica
|
| (dominio + BBDD; sin HTTP)
|
------------------------------------------
        |
        v
--------- database ------------------
|
| schema / SQL  (BBDD.md)
|
--------------------------------------
```

Carpetas: `servidor/src/communication/`, `servidor/src/business_logic/`, `bd/` (schema + seed).

Diseños de cada capa: `Comunicacion.md`, `Logica.md`, `BBDD.md`.
