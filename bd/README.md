# Base de datos — QueAire_Biometria

Carpeta única de scripts SQL del proyecto. Motor **MariaDB 10.4+** (XAMPP),
**InnoDB**, `utf8mb4_unicode_ci` y fechas en **UTC**.

## Contenido

| Fichero | Qué hace |
|---|---|
| `schema.sql` | Esquema completo: crea `QueAire_Biometria` y las tablas del dominio |
| `seed.sql` | Roles, usuarios de prueba, contaminantes, umbrales y demo del mapa |
| `01_schema_usuarios.sql` | Solo `ROLES` + `USUARIO` (útil si solo montas auth) |
| `02_seed_usuarios.sql` | Solo roles/usuarios de auth (`admin` + `usuario`) |
| `QueAire_Biometria.sql` | Volcado completo (estructura + datos) de una instalación |
| `tests/check_schema.sql` | Comprueba tablas, columnas, claves e índices |
| `tests/check_seed.sql` | Comprueba usuarios de prueba y datos de demo |

## Instalación recomendada (esquema completo + demo)

**Opción A — phpMyAdmin**

1. `http://localhost/phpmyadmin`
2. Importar `schema.sql` → Continuar
3. Importar `seed.sql` → Continuar

**Opción B — línea de comandos** (desde la raíz del repo)

```bat
C:\xampp\mysql\bin\mysql.exe -u root --default-character-set=utf8mb4 < bd\schema.sql
C:\xampp\mysql\bin\mysql.exe -u root --default-character-set=utf8mb4 < bd\seed.sql
```

PowerShell:

```powershell
Get-Content -Raw "bd\schema.sql" | C:\xampp\mysql\bin\mysql.exe -u root --default-character-set=utf8mb4
Get-Content -Raw "bd\seed.sql"   | C:\xampp\mysql\bin\mysql.exe -u root --default-character-set=utf8mb4
```

Los scripts son **idempotentes**: se pueden reejecutar sin duplicar datos.

### Solo autenticación (opcional)

```powershell
Get-Content -Raw "bd\01_schema_usuarios.sql" | C:\xampp\mysql\bin\mysql.exe -u root
Get-Content -Raw "bd\02_seed_usuarios.sql"   | C:\xampp\mysql\bin\mysql.exe -u root
```

## Usuarios de prueba

| Email | Contraseña | Rol |
|---|---|---|
| `admin@email.com` | `1234` | admin |
| `cliente@email.com` | `1234` | usuario |
| `tecnico@email.com` | `1234` | tecnico (demo / seed completo) |

Las contraseñas van con bcrypt (`password_hash()` de PHP).
El backend de auth usa el rol **`usuario`** para cuentas normales.

## Datos de demo del mapa (`seed.sql`)

4 nodos en el Grao de Gandia, uno por contaminante, con 12 lecturas cada 2 h
(48 mediciones):

| Nodo | Gas | Zona aproximada |
|---|---|---|
| Campus UPV | CO2 | 38.9965, -0.1660 |
| Port de Gandia | NO2 | 38.9748, -0.1655 |
| Av. del Port | PM2.5 | 38.9908, -0.1580 |
| Centre del Grau | O3 | 38.9825, -0.1605 |

Centro sugerido del mapa: **38.9857, -0.1625** (zoom 14–15).

## Comprobar el estado

```bat
cmd /c "C:\xampp\mysql\bin\mysql.exe -u root --default-character-set=utf8mb4 < bd\tests\check_schema.sql"
cmd /c "C:\xampp\mysql\bin\mysql.exe -u root --default-character-set=utf8mb4 < bd\tests\check_seed.sql"
```

## Esquema

```
ROLES (1) ───────< USUARIO (N)
USUARIO (1) ─────< NODO_SENSOR (N)
CONTAMINANTE (1) ─< NODO_SENSOR (N)
NODO_SENSOR (1) ──< MEDICION (N)
CONTAMINANTE (1) ──< MEDICION (N)
CONTAMINANTE (1) ──< UMBRAL (N)
```

Diseño de referencia: `Agentes_IA/Agente Backend/context/BBDD.md`.

## Convenciones

- Solo se versionan **scripts**, nunca `C:\xampp\mysql\data`.
- Cambios de esquema: primero en `BBDD.md`, luego en `schema.sql`.
- Nombres en `snake_case` y español; fechas en UTC.
