# Diseño de BBDD

## ¿Qué es este fichero?
Aquí está el **diseño ya dado** de la base de datos (tablas, campos y relaciones).
El agente **no** diseña el esquema: implementa el SQL según ## Diseño.

## Buenas prácticas
- La implementación SQL **debe coincidir** con este fichero (sin inventar columnas ni omitir relaciones).
- Formato de tablas según `Database_Design_Spec.md` (no cajas OO de `Software_Engineering_Spec.md`).
- La lógica usa estas tablas/columnas (sin inventar otras).

## Cómo debe quedar el código / SQL
- Scripts: `bd/schema.sql` + `bd/seed.sql`.
- Mismas tablas, campos, PK, FK y constraints que abajo.

## Diseño

MariaDB 10.4+ (XAMPP), InnoDB, `utf8mb4_unicode_ci`. Fechas **UTC** con `DATETIME`. Valores/coords en `DECIMAL` (no `FLOAT`). Auth JWT sin tabla `SESION` (TTL 10 min en config).

En schema ahora: `ROLES`, `USUARIO`, `CONTAMINANTE`, `UMBRAL`, `NODO_SENSOR`, `MEDICION`.
Previsto (aún no en schema): `ESTACION_OFICIAL`, `MEDICION_OFICIAL`.

```text
ROLES (1) ──< USUARIO (1) ──< NODO_SENSOR (1) ──< MEDICION
CONTAMINANTE (1) ──< NODO_SENSOR | MEDICION | UMBRAL
ESTACION_OFICIAL (1) ──< MEDICION_OFICIAL   -- previsto
CONTAMINANTE (1) ──< MEDICION_OFICIAL      -- previsto
```

### ROLES

====================================================================================
TABLE: ROLES
DESCRIPTION: Roles (admin, usuario).
COLUMNS:
+ id_rol | INT UNSIGNED | NOT NULL | AUTO_INCREMENT
+ nombre | VARCHAR(30) | NOT NULL | -
+ descripcion | VARCHAR(255) | NULL | -
PRIMARY KEY: id_rol
FOREIGN KEYS: (ninguna)
CONSTRAINTS:
+ UNIQUE (nombre)
====================================================================================

### USUARIO

====================================================================================
TABLE: USUARIO
DESCRIPTION: Cuentas. `foto_perfil` = ruta relativa; `activo` = soft delete.
COLUMNS:
+ id_usuario | INT UNSIGNED | NOT NULL | AUTO_INCREMENT
+ nombre | VARCHAR(100) | NOT NULL | -
+ apellidos | VARCHAR(150) | NOT NULL | -
+ email | VARCHAR(254) | NOT NULL | -
+ password_hash | VARCHAR(255) | NOT NULL | -
+ id_rol | INT UNSIGNED | NOT NULL | -
+ foto_perfil | VARCHAR(255) | NULL | -
+ activo | BOOLEAN | NOT NULL | TRUE
+ fecha_registro | DATETIME | NOT NULL | CURRENT_TIMESTAMP
+ ultimo_acceso | DATETIME | NULL | -
PRIMARY KEY: id_usuario
FOREIGN KEYS:
+ id_rol -> ROLES(id_rol) | ON DELETE RESTRICT ON UPDATE RESTRICT
CONSTRAINTS:
+ UNIQUE (email)
+ CHECK (email LIKE '%_@_%._%')
====================================================================================

### CONTAMINANTE

====================================================================================
TABLE: CONTAMINANTE
DESCRIPTION: Catálogo de gases medibles.
COLUMNS:
+ id_contaminante | INT UNSIGNED | NOT NULL | AUTO_INCREMENT
+ nombre | VARCHAR(50) | NOT NULL | -
+ unidad_medida | VARCHAR(20) | NOT NULL | -
+ valor_min | DECIMAL(10,3) | NOT NULL | 0.000
+ valor_max | DECIMAL(10,3) | NOT NULL | -
PRIMARY KEY: id_contaminante
FOREIGN KEYS: (ninguna)
CONSTRAINTS:
+ UNIQUE (nombre)
+ CHECK (valor_max > valor_min)
====================================================================================

### NODO_SENSOR

====================================================================================
TABLE: NODO_SENSOR
DESCRIPTION: Nodo BLE. Identidad = (uuid, major); el minor lleva la medida.
COLUMNS:
+ id_nodo | INT UNSIGNED | NOT NULL | AUTO_INCREMENT
+ uuid | CHAR(36) | NOT NULL | -
+ major | SMALLINT UNSIGNED | NOT NULL | -
+ nombre | VARCHAR(100) | NULL | -
+ id_contaminante | INT UNSIGNED | NOT NULL | -
+ id_usuario | INT UNSIGNED | NOT NULL | -
+ estado | VARCHAR(20) | NOT NULL | 'activo'
+ ultima_comunicacion | DATETIME | NULL | -
+ activo | BOOLEAN | NOT NULL | TRUE
+ fecha_alta | DATETIME | NOT NULL | CURRENT_TIMESTAMP
PRIMARY KEY: id_nodo
FOREIGN KEYS:
+ id_contaminante -> CONTAMINANTE(id_contaminante) | ON DELETE RESTRICT ON UPDATE RESTRICT
+ id_usuario -> USUARIO(id_usuario) | ON DELETE RESTRICT ON UPDATE RESTRICT
CONSTRAINTS:
+ UNIQUE (uuid, major)
+ UNIQUE (nombre, id_usuario)
+ CHECK (estado IN ('activo', 'inactivo', 'mantenimiento'))
====================================================================================

### UMBRAL

====================================================================================
TABLE: UMBRAL
DESCRIPTION: Rangos/colores del mapa por contaminante.
COLUMNS:
+ id_umbral | INT UNSIGNED | NOT NULL | AUTO_INCREMENT
+ id_contaminante | INT UNSIGNED | NOT NULL | -
+ categoria | VARCHAR(20) | NOT NULL | -
+ valor_desde | DECIMAL(10,3) | NOT NULL | -
+ valor_hasta | DECIMAL(10,3) | NULL | -
+ color | CHAR(7) | NOT NULL | -
PRIMARY KEY: id_umbral
FOREIGN KEYS:
+ id_contaminante -> CONTAMINANTE(id_contaminante) | ON DELETE CASCADE ON UPDATE RESTRICT
CONSTRAINTS:
+ UNIQUE (id_contaminante, categoria)
+ CHECK (categoria IN ('buena', 'moderada', 'mala', 'muy_mala'))
+ CHECK (valor_hasta IS NULL OR valor_hasta > valor_desde)
+ CHECK (color LIKE '#______')
====================================================================================

### MEDICION

====================================================================================
TABLE: MEDICION
DESCRIPTION: Lecturas geolocalizadas. `valor` ya convertido; `minor_bruto` opcional.
COLUMNS:
+ id_medicion | BIGINT UNSIGNED | NOT NULL | AUTO_INCREMENT
+ id_nodo | INT UNSIGNED | NOT NULL | -
+ id_contaminante | INT UNSIGNED | NOT NULL | -
+ valor | DECIMAL(10,3) | NOT NULL | -
+ minor_bruto | SMALLINT UNSIGNED | NULL | -
+ fecha_hora | DATETIME | NOT NULL | -
+ latitud | DECIMAL(9,6) | NOT NULL | -
+ longitud | DECIMAL(9,6) | NOT NULL | -
PRIMARY KEY: id_medicion
FOREIGN KEYS:
+ id_nodo -> NODO_SENSOR(id_nodo) | ON DELETE RESTRICT ON UPDATE RESTRICT
+ id_contaminante -> CONTAMINANTE(id_contaminante) | ON DELETE RESTRICT ON UPDATE RESTRICT
CONSTRAINTS:
+ UNIQUE (id_nodo, id_contaminante, fecha_hora)
+ CHECK (valor >= 0)
+ CHECK (latitud BETWEEN -90 AND 90)
+ CHECK (longitud BETWEEN -180 AND 180)
====================================================================================

### Índices (además de los de las FK)

+ MEDICION: (id_contaminante, fecha_hora), (id_nodo, fecha_hora), (latitud, longitud)

### Seed (mínimo)

- ROLES: `1 admin`, `3 usuario`
- CONTAMINANTE: CO2, PM2.5, NO2, O3
- UMBRAL: 4 categorías × contaminante (colores `#2ECC71` / `#F1C40F` / `#E67E22` / `#E74C3C`)
- Usuario demo `cliente@email.com` (rol `usuario`), nodos y mediciones de demo para el mapa

### Tests

`tests/check_schema.sql` y `tests/check_seed.sql`.
