# Proyecto de Biometría — Grupo 1 (QueAire)

Sistema de monitorización de la calidad del aire: nodos sensores BLE, aplicación
Android, backend con API y web con mapa.

## Estructura del repositorio

| Carpeta | Contenido |
|---|---|
| `database/` | Esquema SQL, datos de demo y tests de la base de datos |
| `backend/` | API, lógica de negocio y acceso a datos |
| `frontend/` | Web (interfaz y `LogicaProxy`) |
| `android/` | Aplicación móvil: BLE, `ClienteREST` y pantallas |
| `arduino/` | Firmware del nodo sensor (emisión BLE) |
| `Agentes_IA/` | Contexto e instrucciones de los agentes de desarrollo |

Las carpetas de `backend/`, `frontend/`, `android/` y `arduino/` se irán añadiendo
con el código de cada parte.

## Base de datos

MariaDB 10.4+ (XAMPP). Instrucciones de instalación, credenciales de prueba y datos
de demo en [`database/README.md`](database/README.md).

Resumen: importar `database/schema.sql` y después `database/seed.sql`.

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin@email.com` | `1234` | admin |
| `tecnico@email.com` | `1234` | tecnico |
| `cliente@email.com` | `1234` | usuario |

## Flujo del sistema

```
Nodo Arduino --BLE/iBeacon--> App Android --POST JSON--> Backend --> BBDD
                                                            ^
                                              Web (GUI -> LogicaProxy)
```

- El nodo emite la medida en el `minor` del beacon; la identidad del nodo es
  `(uuid, major)`.
- El backend aplica la arquitectura en capas `communication -> business_logic -> database`.
- La web nunca llama a la API directamente: pasa por `LogicaProxy`.
