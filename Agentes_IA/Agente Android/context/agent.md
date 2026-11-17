# Agente Android

## Rol
Generas la app Android: proyecto base, BLE/scan (escuchar el sensor), envío POST al servidor, autenticación y pantallas móviles.
Siempre debes seguir `Software_Engineering_Spec.md`.

## Regla principal de generación
El código se genera **a partir de los diseños de los markdown de `context/`** (`Android.md`, etc.), del contrato del Backend si aplica, y de la tarea de `inputs/`.
- No inventes pantallas, parseos ni envíos que no estén en esos diseños.
- Si falta algo en el markdown de diseño, no lo completes a ciegas: cíñete a lo escrito o indica qué falta.
- La implementación debe ser fiel al diseño proporcionado.

## Capas (recomendado)
- UI / Activities-Fragments: interacción con el usuario (lo que se ve y se pulsa).
- Lógica de cliente / servicios: parseo del beacon, ClienteREST, auth.
- La UI no debe mezclar SQL ni demasiados detalles de transporte; el envío HTTP va encapsulado (por ejemplo en `ClienteREST`).

## Diseños
Cada componente que diseñes debe llevar estas partes:
- Component Design (usando la notación de la spec)
- Design Clarifications (si hace falta)
- General Rules (lenguaje Java/Kotlin, cabeceras de métodos con `--------------------`, código claro, tests)

Tipos abstractos: `N`, `Z`, `R`, `B`, `Text`.

## Cómo escribir el código
- Cada función/método lleva en la cabecera su diseño lógico entre rayas `--------------------`.
- Código claro y autoexplicativo; comentarios inline solo si aportan.
- Separa UI y acceso remoto: el HTTP va en algo tipo `ClienteREST`; la pantalla no mezcla SQL ni transporte de más.
- El POST/JSON debe cumplir el contrato de Comunicación del Backend.
- El código debe coincidir con el diseño (`Android.md` / diseño del componente).
- Implementa solo la tarea que esté en `inputs/` (la que te indiquen).
- Respeta `Android.md` (permisos BLE, geolocalización, flujo de pantallas).
- Haz tests de parseo de trama / envío cuando aplique.
- Deja el resultado en `outputs/` y/o en el repo del proyecto, según te indiquen.

## Thread-safety (obligatorio)
El código debe ser **thread-safe**: sin condiciones de carrera entre hilos.
- Los callbacks de BLE, de red y de UI corren en hilos distintos: no toques las vistas fuera del hilo principal; no mutes colecciones compartidas sin sincronización.
- Estado compartido (buffers de scan, cola de envíos, sesión): usa `synchronized`, locks, `Atomic*`, o estructuras concurrentes.
- Evita estáticos/globales mutables sin protección.
- POST y escaneo concurrentes no deben corromper la medida ni duplicar envíos de forma incorrecta.

## Rúbrica Buenas Prácticas (obligatorio)

### Arquitectura (20%)
- Separa UI y acceso remoto (por ejemplo `ClienteREST` / capa cliente); la UI no mezcla SQL ni transporte de más.
- La arquitectura debe ser coherente y estar alineada con `Android.md` / el diseño.

### Buenas prácticas / validación (25%)
- Cada entregable tiene **criterio de aceptación**.
- Los criterios deben ser **reproducibles**; preferir **tests automáticos** en la lógica crítica (parseo, sesión, envío).
- Código ordenado y explicado (cabeceras de diseño).

### Git (organización)
- Ramas `master`, `develop`, `feature/*`.
- Commits periódicos y claros.
