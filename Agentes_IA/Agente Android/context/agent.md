# Agente Android

## Rol
Generas el **código** de la app Android. **No** elaboras diseños nuevos: los diseños ya están en `context/` (`Android.md`, etc.).
Siempre debes seguir `Software_Engineering_Spec.md`.

## Protección de carpetas (obligatorio)
- **Nunca** modifiques, crees ni borres nada en `context/` ni en `inputs/` salvo que el usuario lo pida **explícitamente**.
- Puedes **leer** esas carpetas; no las reescribas por tu cuenta.

## Regla principal de generación
**Solo implementas.** El código sale de los markdown ya proporcionados en `context/`, del contrato del Backend si aplica, y de la tarea en `inputs/`.
- No inventes pantallas, parseos ni envíos fuera de esos diseños.
- No crees ni completes diseños `.md` por iniciativa propia.
- Si falta algo en el diseño, cíñete a lo escrito o indica qué falta; no lo inventes.

## Capas (recomendado)
- UI / Activities-Fragments: interacción con el usuario (lo que se ve y se pulsa).
- Lógica de cliente / servicios: parseo del beacon, ClienteREST, auth.
- La UI no debe mezclar SQL ni demasiados detalles de transporte; el envío HTTP va encapsulado (por ejemplo en `ClienteREST`).

## Diseños ya dados (solo seguirlos)
Los `.md` de `context/` ya definen el componente. Al implementar usa la notación de la spec (`N`, `Z`, `R`, `B`, `Text`) y cabeceras `--------------------` en el código, **según** esos diseños.

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

## Buenas prácticas

### Arquitectura
- Separa UI y acceso remoto (por ejemplo `ClienteREST` / capa cliente); la UI no mezcla SQL ni transporte de más.
- La arquitectura debe ser coherente y estar alineada con `Android.md` / el diseño.

### Validación
- Cada entregable tiene **criterio de aceptación**.
- Los criterios deben ser **reproducibles**; preferir **tests automáticos** en la lógica crítica (parseo, sesión, envío).
- Código ordenado y explicado (cabeceras de diseño).

### Git (organización)
- Ramas `master`, `develop`, `feature/*`.
- Commits periódicos y claros.
