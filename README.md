# Proyecto de Biometría — Grupo 1 (QueAire)

Sistema de monitorización de la calidad del aire: nodos sensores BLE, aplicación
Android, backend con API y web con mapa.

## Cómo arrancar (XAMPP — sin terminales)

1. Instala [XAMPP](https://www.apachefriends.org/) en `C:\xampp` (Apache + MySQL).
2. **Una sola vez** por clon del repo: clic derecho en `activar-xampp.bat` →
   **Ejecutar como administrador** (enlaza el proyecto en `C:\xampp\htdocs`).
   Si solo haces doble clic, Windows pedirá el permiso UAC igual.
3. En el panel de XAMPP, **Start** en **Apache** y **MySQL**.
4. Base de datos (solo la primera vez): ver [`bd/README.md`](bd/README.md)  
   → phpMyAdmin e importar `schema.sql` + `seed.sql`.
5. Abre la web: [http://localhost/queaire/landing.html](http://localhost/queaire/landing.html)

| Qué | URL |
|---|---|
| Web | `http://localhost/queaire/landing.html` |
| API | `http://localhost/api/` |
| phpMyAdmin | `http://localhost/phpmyadmin` |

No hace falta abrir terminales ni usar `php -S`.

### Usuarios de prueba

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin@email.com` | `1234` | admin |
| `cliente@email.com` | `1234` | usuario |
| `tecnico@email.com` | `1234` | tecnico (demo) |

MySQL por defecto: usuario `root`, sin contraseña, BD `QueAire_Biometria` (ya configurado en `servidor/config.php`).

## Estructura del repositorio

| Carpeta | Contenido |
|---|---|
| `bd/` | Esquema SQL, datos de demo y tests de la base de datos |
| `servidor/` | API PHP (Apache sirve `servidor/public` como `/api`) |
| `Pagina web/` | Frontend (Apache la sirve como `/queaire`) |
| `android/` | Aplicación móvil: BLE, `ClienteREST` y pantallas |
| `arduino/` | Firmware del nodo sensor (emisión BLE) |
| `Agentes_IA/` | Contexto e instrucciones de los agentes de desarrollo |

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
