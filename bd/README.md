# Base de datos QuéAire

Motor: **MariaDB / MySQL** (XAMPP). Nombre: `QueAire_Biometria`.

## Archivos

| Archivo | Qué es |
|---------|--------|
| `01_schema_usuarios.sql` | Crea la BD y las tablas `ROLES` y `USUARIO` |
| `02_seed_usuarios.sql` | Roles y usuarios de prueba (`admin@email.com` / `cliente@email.com`, contraseña `1234`) |
| `QueAire_Biometria.sql` | Volcado completo actual (estructura + datos) |

## Cargar en XAMPP

Con MySQL en marcha:

```powershell
Get-Content -Raw "bd\01_schema_usuarios.sql" | C:\xampp\mysql\bin\mysql.exe -u root
Get-Content -Raw "bd\02_seed_usuarios.sql" | C:\xampp\mysql\bin\mysql.exe -u root
```

O importar el volcado completo:

```powershell
Get-Content -Raw "bd\QueAire_Biometria.sql" | C:\xampp\mysql\bin\mysql.exe -u root
```

También puedes importar desde phpMyAdmin → Importar.
