# Base de datos — QueAire_Biometria

Esquema y datos de la base de datos del proyecto. Motor **MariaDB 10.4+** (XAMPP),
**InnoDB**, `utf8mb4_unicode_ci` y fechas en **UTC**.

## Contenido

| Fichero | Qué hace |
|---|---|
| `schema.sql` | Crea la base `QueAire_Biometria` y las 6 tablas |
| `seed.sql` | Roles, usuarios de prueba, catálogo de contaminantes, umbrales y datos de demo del mapa |
| `tests/check_schema.sql` | Verifica que las tablas, columnas, claves e índices existen |
| `tests/check_seed.sql` | Verifica los usuarios de prueba y los datos de demo |

## Instalación

**Opción A — phpMyAdmin**

1. `http://localhost/phpmyadmin`
2. Pestaña **Importar** → `schema.sql` → Continuar
3. Pestaña **Importar** → `seed.sql` → Continuar

**Opción B — línea de comandos** (desde la raíz del repo, en `cmd`)

```bat
C:\xampp\mysql\bin\mysql.exe -u root --default-character-set=utf8mb4 < database\schema.sql
C:\xampp\mysql\bin\mysql.exe -u root --default-character-set=utf8mb4 < database\seed.sql
```

Ambos scripts son **idempotentes**: se pueden ejecutar varias veces sin duplicar datos.

## Usuarios de prueba

| Email | Contraseña | Rol |
|---|---|---|
| `admin@email.com` | `1234` | admin |
| `tecnico@email.com` | `1234` | tecnico |
| `cliente@email.com` | `1234` | usuario |

Las contraseñas están almacenadas con bcrypt (`password_hash()` de PHP). Los usuarios
de demo son propietarios de los 4 nodos del mapa.

## Datos de demo del mapa

4 nodos en el Grao de Gandia, uno por contaminante, con 12 lecturas cada 2 horas
durante las últimas 24 h (48 mediciones en total):

| Nodo | Gas | Zona aproximada |
|---|---|---|
| Campus UPV | CO2 | 38.9965, -0.1660 |
| Port de Gandia | NO2 | 38.9748, -0.1655 |
| Av. del Port | PM2.5 | 38.9908, -0.1580 |
| Centre del Grau | O3 | 38.9825, -0.1605 |

Centro sugerido para el mapa: **38.9857, -0.1625** (zoom 14–15).
Los colores salen de la tabla `UMBRAL` (verde, amarillo, naranja, rojo).

**Regenerar la demo** (refresca las fechas a las últimas 24 h):

```bat
C:\xampp\mysql\bin\mysql.exe -u root --default-character-set=utf8mb4 < database\seed.sql
```

El seed solo borra los nodos con uuid `00000000-0000-0000-0000-00000000000X` y sus
mediciones; los datos reales no se tocan.

## Comprobar el estado

```bat
cmd /c "C:\xampp\mysql\bin\mysql.exe -u root --default-character-set=utf8mb4 < database\tests\check_schema.sql"
cmd /c "C:\xampp\mysql\bin\mysql.exe -u root --default-character-set=utf8mb4 < database\tests\check_seed.sql"
```

`check_schema.sql` termina con `total_problemas = 0` si todo está correcto.

## Esquema

```
ROLES (1) ───────< USUARIO (N)
USUARIO (1) ─────< NODO_SENSOR (N)
CONTAMINANTE (1) ─< NODO_SENSOR (N)      -- cada nodo mide un gas fijo
NODO_SENSOR (1) ──< MEDICION (N)
CONTAMINANTE (1) ──< MEDICION (N)
CONTAMINANTE (1) ──< UMBRAL (N)
```

Reglas principales:

- Identidad del nodo: `(uuid, major)`. El `minor` del beacon lleva la medida
  (`minor = round(valor × 10)`), no la identidad.
- `MEDICION.valor` se guarda ya convertido a la unidad del contaminante.
- Borrado lógico con `activo`; las claves foráneas son `RESTRICT` (salvo `UMBRAL`).
- `NODO_SENSOR.estado` no guarda `desconectado`: se deduce de `ultima_comunicacion`.
- Las tablas `ESTACION_OFICIAL` y `MEDICION_OFICIAL` se añadirán más adelante.

## Convenciones

- Solo se versionan **scripts**, nunca el directorio de datos de MySQL
  (`C:\xampp\mysql\data`).
- Todo cambio de esquema se refleja primero en
  `Agentes_IA/Agente Backend/context/BBDD.md` y luego en `schema.sql`.
- Nombres en `snake_case` y español; fechas en UTC.

## Nota sobre Windows

Con `lower_case_table_names = 1` (por defecto en Windows), MariaDB guarda las tablas en
minúsculas aunque el script las cree como `USUARIO` o `NODO_SENSOR`. Las consultas en
mayúsculas funcionan igual. En Linux sí distingue mayúsculas: si se despliega allí, usar
`lower_case_table_names = 1` o crear las tablas en minúsculas.
