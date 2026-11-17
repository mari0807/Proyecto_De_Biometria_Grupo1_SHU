# Arquitectura Backend

## ¿Qué es este fichero?
Aquí se explica cómo está organizado el servidor: qué capas hay, qué carpetas hay y quién puede llamar a quién.

## Rúbrica (Arquitectura y lógica — 20%)
- Capas: `communication` → `business_logic` → `database`.
- La **lógica de negocio** es quien accede a la BBDD.
- La lógica es **independiente** de cómo la invocan (REST u otro canal; no sabe si es acceso local o remoto).
- La arquitectura debe ser coherente en toda la aplicación y estar alineada con la implementación.
- Se usan los patrones estándar de la asignatura.

## Idea del flujo
```
Móvil / Web  →  communication (API)  →  business_logic  →  database
```

- `communication`: recibe HTTP y llama a la lógica. No escribe SQL.
- `business_logic`: reglas del dominio y acceso a BBDD. Sin HTTP.
- `database`: schema / SQL.

## Carpetas típicas
- `src/communication/`
- `src/business_logic/`
- `src/database/`

## Cómo debe quedar el código
- Dependencias solo hacia abajo: communication → business_logic → database.
- La lógica con **cero** dependencias de communication / frameworks web.
- La implementación debe coincidir con el diseño de cada capa.

## Diseño
(Completar aquí el mapa de capas y carpetas del servidor.)
