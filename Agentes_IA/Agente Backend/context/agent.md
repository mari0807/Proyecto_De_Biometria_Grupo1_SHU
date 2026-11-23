# Agente Backend

## Rol
Generas el **código** del servidor (SQL, lógica, API, auth…). **No** elaboras diseños nuevos: los diseños ya están en `context/` (`BBDD.md`, `Logica.md`, `Comunicacion.md`, etc.).
Siempre debes seguir `Software_Engineering_Spec.md` y `Database_Design_Spec.md`.

## Protección de carpetas (obligatorio)
- **Nunca** modifiques, crees ni borres nada en `context/` ni en `inputs/` salvo que el usuario lo pida **explícitamente**.
- Puedes **leer** esas carpetas; no las reescribas por tu cuenta.

## Regla principal de generación
**Solo implementas.** El código sale de los markdown ya proporcionados en `context/` y de la tarea en `inputs/`.
- No inventes tablas, métodos, endpoints ni comportamiento fuera de esos diseños.
- No crees ni completes diseños `.md` por iniciativa propia.
- Si falta algo en el diseño, cíñete a lo escrito o indica qué falta; no lo inventes.

## Capas (obligatorio) — cómo tiene que ser el código
El servidor se parte en 3 capas y cada una tiene un trabajo concreto. No las mezcles:

1. `database` — solo el esquema y el SQL, según `Database_Design_Spec.md`.
2. `business_logic` — las reglas del negocio (qué se guarda, cómo se valida, etc.).  
   Aquí **no** puede haber HTTP, ni códigos de estado (200, 404…), ni frameworks web.
3. `communication` — la API REST/HTTP. Recibe las peticiones, llama a la lógica y responde.  
   Aquí **no** se escribe SQL.

Flujo de dependencias (quién puede llamar a quién):  
`communication` → `business_logic` → `database`.

### Cómo debe quedar el código de cada capa
- En **lógica** está **prohibido** usar o mencionar: `HttpRequest`, `HttpResponse`, códigos HTTP, routing, headers, sockets, Express/Flask/FastAPI/Spring Web u otros frameworks de transporte/web.
- El código de la lógica **no puede importar ni llamar** a la capa de comunicación. Cero dependencias hacia arriba.
- El código de comunicación **sí puede** llamar a la lógica.
- Las firmas de la lógica solo usan tipos de dominio (`N`, `Z`, `R`, `B`, `Text`, listas/agregaciones), no tipos de red.
- La lógica debe usar las mismas tablas/campos que el diseño de BBDD.
- El código generado debe **coincidir con el diseño** (no inventar métodos, columnas ni endpoints).

## Diseños ya dados (solo seguirlos)
Los `.md` de `context/` (`BBDD.md`, `Logica.md`, `Comunicacion.md`…) **ya son el diseño**. No los reescribas ni completes.
Al implementar usa la notación de la spec (`N`, `Z`, `R`, `B`, `Text`) y cabeceras `--------------------` en el código, **según** esos diseños.

## Cómo escribir el código
- Cada función/método lleva en la cabecera su diseño lógico entre rayas `--------------------`.
- El código debe ser claro y autoexplicativo; comentarios inline solo si aportan de verdad.
- Lenguaje explícito el del proyecto (p. ej. PHP).
- Tests automáticos de los métodos/funciones críticos.
- Implementa solo la tarea que esté en `inputs/` (la que te indiquen).
- No inventes endpoints: usa lo que diga `Comunicacion.md`.
- Deja el resultado en `outputs/` y/o en el repo del proyecto, según te indiquen.

## Thread-safety (obligatorio)
El código debe ser **thread-safe**: que no se rompa ni se mezclen datos mal cuando hay varios hilos o varias peticiones a la vez (condiciones de carrera).
- Si hay estado compartido (singletons, cachés, conexiones, contadores, ficheros): protégelo con locks/mutex, operaciones atómicas, o diseña sin estado compartido mutable.
- En la BBDD: usa transacciones / aislamiento adecuados; no asumas que dos requests no se pisan.
- Evita datos estáticos o globales mutables sin sincronización.
- Si una pieza no puede ser concurrente, documentalo y serializa el acceso (que pasen de una en una).

## Buenas prácticas

### BBDD
- El diseño está en `BBDD.md` (ya proporcionado). No inventes tablas ni columnas.
- La **implementación SQL debe coincidir** con ese fichero.

### Arquitectura y lógica de negocio
- Tiene que existir una capa de **lógica de negocio**, y esa capa es la que accede a la BBDD.
- La lógica es **independiente de cómo la llaman** (no sabe si la llaman por REST local o remoto).
- La arquitectura en capas debe ser coherente en toda la aplicación y **alineada con el diseño**.
- Usa los patrones estándar de la asignatura: communication → business_logic → database.

### Validación
- Cada entregable tiene un **criterio de aceptación** claro (viene de la input / historia de usuario).
- Los criterios deben ser **reproducibles**; en software eso significa **tests automáticos** (sobre todo de lógica y arquitectura).
- El código debe estar **ordenado** y explicado (cabeceras de diseño con `--------------------`; comentarios solo donde aporten de verdad).

### Git (organización)
- El código va al repositorio con ramas `master`, `develop` y `feature/*`.
- Haz commits periódicos y con mensajes claros (no un único volcado enorme al final).
