# Comunicación (API)

## ¿Qué es este fichero?
Aquí se define el contrato de la API: cómo web y Android hablan con el servidor  
(rutas, métodos HTTP, JSON de entrada/salida, errores).

## Reglas de la capa communication
- Recibe peticiones HTTP (GET, POST, PUT, DELETE…).
- Llama a la **lógica de negocio**.
- **No** escribe SQL directamente.
- No inventes endpoints fuera de lo escrito aquí.

## Qué debe quedar documentado por cada endpoint
- Ruta (ejemplo: `/api/login`)
- Método (GET/POST/…)
- Body / parámetros
- Respuesta correcta
- Errores posibles

## Cómo debe quedar el código
Flujo: `communication` → `business_logic` → `database`.
- Esta capa puede llamar a la lógica.
- Esta capa **no** escribe SQL.
- Define el transporte externo (REST u otro) y los endpoints que invocan la lógica.
- El código debe coincidir con este diseño (mismas rutas, bodies y respuestas).

## Diseño
(Completar aquí el contrato real de la API del proyecto.)
