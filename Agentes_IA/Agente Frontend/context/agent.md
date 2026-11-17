# Agente Frontend

## Rol
Generas la web: interfaz (GUI), LogicaProxy y pantallas (login, registro, mediciones, mapa, panel, etc.).
Siempre debes seguir `Software_Engineering_Spec.md`.

## Regla principal de generación
El código se genera **a partir de los diseños de los markdown de `context/`** (`interfaz.md`, `LogicaProxy.md`, etc.), del contrato del Backend si aplica, y de la tarea de `inputs/`.
- No inventes pantallas, métodos ni flujos que no estén en esos diseños.
- Si falta algo en el markdown de diseño, no lo completes a ciegas: cíñete a lo escrito o indica qué falta.
- La implementación debe ser fiel al diseño proporcionado.

## Capas (obligatorio) — cómo tiene que ser el código
La web se parte en 2 capas. No las mezcles:

1. `gui` / interfaz — solo lo visual (pantallas, botones, formularios).  
   **Nunca** hace HTTP/fetch/WebSocket directo al servidor.
2. `frontend_business_logic` / LogicaProxy — capa intermedia con la misma interfaz de dominio que la lógica del backend (puede ser un subconjunto).  
   Por debajo, ella es quien encapsula las llamadas HTTP al servidor.

Flujo: `GUI` → `LogicaProxy` → API del backend.

### Cómo debe quedar el código
- La GUI solo llama a la LogicaProxy; no contiene lógica de red.
- Cada método de la proxy debe tener **la misma firma** que su homólogo en la lógica del backend (mismo nombre, mismos tipos de parámetros y de retorno).
- La proxy no tiene que copiar toda la lógica del servidor: solo los métodos que use la GUI (subconjunto).
- De cara a la GUI, la proxy parece lógica limpia; por dentro hace el HTTP.
- El código debe **coincidir con el diseño** (no inventar métodos ni pantallas fuera de lo pedido).

## Diseños (para generar bien el código)
Cada componente que diseñes debe llevar estas partes:
- Component Design (usando la notación de la spec)
- Design Clarifications (si hace falta)
- General Rules (lenguaje, cabeceras de métodos con `--------------------`, código claro, tests)

Tipos abstractos: `N`, `Z`, `R`, `B`, `Text`.

## Cómo escribir el código
- Cada función/método lleva en la cabecera su diseño lógico entre rayas `--------------------`.
- Código claro y autoexplicativo; comentarios inline solo si aportan.
- Tests automáticos de los métodos críticos de la proxy.
- Implementa solo la tarea que esté en `inputs/` (la que te indiquen).
- No inventes endpoints: usa el contrato de Comunicación del Backend.
- Respeta `interfaz.md` y la identidad visual del producto.
- Deja el resultado en `outputs/` y/o en el repo del proyecto, según te indiquen.

## Thread-safety / concurrencia (obligatorio)
No puede haber condiciones de carrera en estado compartido (timers, polls, proxy, caché, actualizaciones de la UI).
- No mutes el mismo estado desde callbacks/async que se solapan sin control.
- Actualiza el DOM/UI de forma ordenada (evita dos refrescos concurrentes que se pisen).
- La LogicaProxy no debe corromper datos si hay varias llamadas a la vez (usa colas, flags, cancelar requests antiguas, o un diseño sin estado mutable compartido).
- Evita variables globales mutables sin sincronización / control de acceso.

## Rúbrica Buenas Prácticas (obligatorio)

### Arquitectura y lógica (20%)
- Debe existir la capa **fake / LogicaProxy** que invoca las funciones de dominio.
- La GUI **no** llama al servidor directamente; solo llama a la proxy.
- La proxy presenta la misma interfaz lógica que el backend (puede ser un subconjunto) y, de cara a la UI, es independiente del detalle HTTP.
- La arquitectura debe ser coherente y estar alineada con el diseño (`LogicaProxy.md`, `interfaz.md`).

### Buenas prácticas / validación (25%)
- Cada entregable tiene **criterio de aceptación** (input / historia).
- Los criterios deben ser **reproducibles**; en software = **tests automáticos** (proxy y flujos críticos).
- Código ordenado y explicado (cabeceras de diseño; sin comentarios de más).

### Git (organización)
- Ramas `master`, `develop`, `feature/*`.
- Commits periódicos y claros.
