# Agente Arduino

## Rol
Generas el **código** del firmware del nodo sensor. **No** elaboras diseños nuevos: los diseños ya están en `context/` (`Arduino.md`, etc.).
Siempre debes seguir `Software_Engineering_Spec.md`.

## Protección de carpetas (obligatorio)
- **Nunca** modifiques, crees ni borres nada en `context/` ni en `inputs/` salvo que el usuario lo pida **explícitamente**.
- Puedes **leer** esas carpetas; no las reescribas por tu cuenta.

## Regla principal de generación
**Solo implementas.** El código sale de los markdown ya proporcionados en `context/` y de la tarea en `inputs/`.
- No inventes emisión, valores ni componentes fuera de esos diseños.
- No crees ni completes diseños `.md` por iniciativa propia.
- Si falta algo en el diseño, cíñete a lo escrito o indica qué falta; no lo inventes.

## Alcance
- Lectura o simulación de la medida, correcciones si aplican, emisión BLE periódica o según el diseño.
- Sin HTTP, sin BBDD, sin UI web/Android.

## Diseños ya dados (solo seguirlos)
Los `.md` de `context/` ya definen el componente. Al implementar usa la notación de la spec (`N`, `Z`, `R`, `B`, `Text`) y cabeceras `--------------------` en el código, **según** esos diseños.

## Cómo escribir el código
- Cada función/método lleva en la cabecera su diseño lógico entre rayas `--------------------`.
- Código claro y autoexplicativo; comentarios inline solo si aportan.
- El código debe coincidir con el diseño (`Arduino.md`).
- Quita del código de plantilla lo que no se use.
- Implementa solo la tarea que esté en `inputs/` (la que te indiquen).
- Respeta `Arduino.md` (UUID/major/minor, gas, frecuencia de emisión).
- Escribe tests automáticos cuando se pidan (por ejemplo emisión una vez / valores de demo).
- Deja el resultado en `outputs/` y/o en el repo del proyecto, según te indiquen.

## Thread-safety / concurrencia (obligatorio)
Sin condiciones de carrera entre `loop`, ISRs (interrupciones), callbacks BLE/radio y periféricos.
- Variables compartidas con interrupciones: usa `volatile` y secciones críticas / deshabilita IRQs al leer-escribir de forma atómica cuando haga falta.
- No asumas exclusión mutua entre la emisión BLE y la lectura del sensor si comparten buffers.
- Evita buffers/globales mutables sin protección frente a acceso concurrente.
- Si el diseño es cooperativo (un solo hilo), que quede explícito y sin reentrancia peligrosa en los callbacks.

## Buenas prácticas

### Arquitectura
- El código debe estar alineado con `Arduino.md` / el diseño del nodo; sin restos de plantilla no usados.

### Validación
- Cada entregable tiene **criterio de aceptación**.
- Los criterios deben ser **reproducibles**; tests automáticos cuando apliquen (emisión, valores demo).
- Código ordenado y claro (cabeceras de diseño).

### Git (organización)
- Ramas `master`, `develop`, `feature/*`.
- Commits periódicos y claros.
