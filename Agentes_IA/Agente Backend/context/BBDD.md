# Diseño de BBDD

## ¿Qué es este fichero?
Aquí se describe el diseño de la base de datos del proyecto (tablas, campos y relaciones).  
Es el plano que luego hay que implementar en SQL y **la fuente única de verdad** del esquema:
toda implementación (DDL, lógica de negocio, DTOs y endpoints) debe ceñirse a lo escrito aquí.

## Rúbrica (Diseño de la BD — 20%)
- El diseño de BBDD debe ser correcto: tablas, campos, tipos, claves primarias (PK), claves foráneas (FK) y relaciones.
- La implementación SQL **debe coincidir** con este diseño (sin desviaciones: sin columnas inventadas ni relaciones omitidas).
- El formato del diseño debe seguir `Database_Design_Spec.md`.

## Qué debe incluir el diseño
Para cada tabla, según la spec:
- TABLE (nombre)
- DESCRIPTION (para qué sirve)
- COLUMNS (nombre, tipo, NULL/NOT NULL, valor por defecto si hay)
- PRIMARY KEY
- FOREIGN KEYS
- CONSTRAINTS

## Cómo debe quedar el código / SQL
- Debe seguir el formato de `Database_Design_Spec.md`.
- El SQL implementado debe coincidir con este diseño (mismas tablas, campos y relaciones).
- La lógica de negocio debe usar estas tablas/columnas (sin inventar otras).

---

## General Rules (reglas del esquema)

**Motor y codificación**
- **MariaDB 10.6+** (mínimo 10.2, necesario para que se apliquen los `CHECK`), motor **InnoDB**.
- `CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci` en toda la base de datos.
  La collation `_ci` hace que las columnas `UNIQUE` de texto (`email`, `nombre`) sean únicas
  sin distinguir mayúsculas/minúsculas.
- Fechas siempre en **UTC**, con tipo `DATETIME` (sin conversión implícita de zona horaria;
  no usar `TIMESTAMP`). La conversión a hora local es responsabilidad de la capa que presenta.

**Nomenclatura y tipos**
- Nombres en `snake_case` y en español; PK: `id_<entidad>`.
- Catálogos: PK `INT UNSIGNED AUTO_INCREMENT`.
- Tablas de alto volumen (`MEDICION`, `MEDICION_OFICIAL`): PK `BIGINT UNSIGNED AUTO_INCREMENT`.
- Tipos de FK: deben coincidir exactamente con la PK referenciada (`INT UNSIGNED`).
- Valores físicos y coordenadas: `DECIMAL` (nunca `FLOAT`) para evitar errores de redondeo.
- Booleanos: `BOOLEAN` (`TINYINT(1)`).

**Integridad**
- Todas las FK con `ON DELETE RESTRICT ON UPDATE RESTRICT`, salvo `UMBRAL.id_contaminante`,
  que usa `ON DELETE CASCADE` (los umbrales son datos derivados del catálogo).
- Borrado **lógico** de usuarios y nodos (`activo = FALSE`). Nunca se borra físicamente
  un usuario o un nodo que tenga mediciones: se conserva el histórico.
- Estados y categorías restringidos con `CHECK` (MariaDB los hace cumplir).
- La BD no guarda datos derivados innecesarios: `NODO_SENSOR.estado` **no** almacena
  `desconectado`, se calcula a partir de `ultima_comunicacion`.

**Seguridad**
- `USUARIO.password_hash` nunca se incluye en respuestas JSON ni se carga en listados.
- `USUARIO.foto_perfil` guarda una **ruta relativa** al fichero de imagen
  (p. ej. `perfiles/u_12.webp`). El binario vive en el sistema de ficheros, no en la BD.
  Se sirve mediante un endpoint dedicado.

**Datos numéricos del nodo**
- El `minor` del beacon iBeacon (uint16 de 0 a 65535) **transporta la medida**,
  con el encoding pactado `minor = round(valor * 10)` (resolución 0,1 en la unidad del
  contaminante; rango útil hasta 6553,5).
- `MEDICION.valor` guarda **ya el valor físico convertido** a la unidad del contaminante.
- `MEDICION.minor_bruto` conserva el `minor` original para trazabilidad y recalibraciones.

---

## Diseño

> Origen: `inputs/estructura_bd.md` (que a su vez corrige el diagrama original).
> Si hay discrepancia entre ese documento y este fichero, **manda este fichero**.
> Las desviaciones están justificadas en *Design Clarifications*.

### Modelo entidad-relación

```text
ROLES (1) ───────────< (N) USUARIO
USUARIO (1) ─────────< (N) NODO_SENSOR
CONTAMINANTE (1) ────< (N) NODO_SENSOR          -- cada nodo mide un contaminante
NODO_SENSOR (1) ─────< (N) MEDICION
CONTAMINANTE (1) ────< (N) MEDICION
CONTAMINANTE (1) ────< (N) UMBRAL
CONTAMINANTE (1) ────< (N) MEDICION_OFICIAL
ESTACION_OFICIAL (1) ─< (N) MEDICION_OFICIAL
```

### Tablas

====================================================================================
TABLE: ROLES

DESCRIPTION: Catálogo de perfiles de permisos de la plataforma (admin, cliente).

COLUMNS:

+ id_rol | INT UNSIGNED | NOT NULL | AUTO_INCREMENT
+ nombre | VARCHAR(30) | NOT NULL | -
+ descripcion | VARCHAR(255) | NULL | -

PRIMARY KEY: id_rol

FOREIGN KEYS: (ninguna)

CONSTRAINTS:

+ UNIQUE (nombre)
====================================================================================

====================================================================================
TABLE: USUARIO

DESCRIPTION: Cuentas de usuario y credenciales de autenticación.

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

====================================================================================
TABLE: CONTAMINANTE

DESCRIPTION: Catálogo maestro de gases, partículas o parámetros ambientales medibles.

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

====================================================================================
TABLE: NODO_SENSOR

DESCRIPTION: Nodos físicos (Arduino + BLE) vinculados a un usuario. La identidad del
             dispositivo es el par (uuid, major); el minor transporta la medida.

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

====================================================================================
TABLE: UMBRAL

DESCRIPTION: Rangos de calidad del aire por contaminante, con categoría y color.
             Permite pintar el mapa y clasificar mediciones sin constantes en el código.

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

====================================================================================
TABLE: MEDICION

DESCRIPTION: Lecturas geolocalizadas enviadas por los nodos de los usuarios.

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

====================================================================================
TABLE: ESTACION_OFICIAL

DESCRIPTION: Estaciones institucionales de calidad del aire con ubicación fija.

COLUMNS:

+ id_estacion | INT UNSIGNED | NOT NULL | AUTO_INCREMENT
+ id_externo | VARCHAR(64) | NULL | -
+ nombre | VARCHAR(150) | NOT NULL | -
+ latitud | DECIMAL(9,6) | NOT NULL | -
+ longitud | DECIMAL(9,6) | NOT NULL | -
+ activa | BOOLEAN | NOT NULL | TRUE

PRIMARY KEY: id_estacion

FOREIGN KEYS: (ninguna)

CONSTRAINTS:

+ UNIQUE (id_externo)
+ CHECK (latitud BETWEEN -90 AND 90)
+ CHECK (longitud BETWEEN -180 AND 180)
====================================================================================

====================================================================================
TABLE: MEDICION_OFICIAL

DESCRIPTION: Mediciones de las estaciones oficiales. No guardan coordenadas por fila:
             heredan la ubicación fija de ESTACION_OFICIAL.

COLUMNS:

+ id_medicion_oficial | BIGINT UNSIGNED | NOT NULL | AUTO_INCREMENT
+ id_estacion | INT UNSIGNED | NOT NULL | -
+ id_contaminante | INT UNSIGNED | NOT NULL | -
+ valor | DECIMAL(10,3) | NOT NULL | -
+ fecha_hora | DATETIME | NOT NULL | -

PRIMARY KEY: id_medicion_oficial

FOREIGN KEYS:

+ id_estacion -> ESTACION_OFICIAL(id_estacion) | ON DELETE RESTRICT ON UPDATE RESTRICT
+ id_contaminante -> CONTAMINANTE(id_contaminante) | ON DELETE RESTRICT ON UPDATE RESTRICT

CONSTRAINTS:

+ UNIQUE (id_estacion, id_contaminante, fecha_hora)
+ CHECK (valor >= 0)
====================================================================================

### Índices

Las FK de InnoDB crean su índice automáticamente; los siguientes se añaden explícitamente.

+ MEDICION: INDEX (id_contaminante, fecha_hora)          -- series temporales por gas
+ MEDICION: INDEX (id_nodo, fecha_hora)                  -- histórico de un nodo
+ MEDICION: INDEX (latitud, longitud)                    -- ventana geográfica del mapa (bounding box)
+ MEDICION_OFICIAL: INDEX (id_contaminante, fecha_hora)  -- comparativas y gráficas
+ ESTACION_OFICIAL: INDEX (latitud, longitud)            -- búsqueda por zona

> En MariaDB no se usa `POINT SRID 4326` + índice espacial (soporte distinto al de MySQL 8):
> el mapa filtra con `WHERE latitud BETWEEN ? AND ? AND longitud BETWEEN ? AND ?`,
> que aprovecha el índice compuesto.

### Reglas de negocio que la lógica debe garantizar

1. **Registro**: `email` único (sin distinguir mayúsculas), `password_hash` con `password_hash()`
   de PHP (bcrypt) y tamaño ≤ 255. Rol por defecto: `cliente`.
2. **Login**: solo usuarios con `activo = TRUE`. Al entrar se actualiza `ultimo_acceso`.
   `password_hash` y `foto_perfil` nunca viajan en las respuestas.
3. **Alta de nodo (auto-registro)**: el usuario autenticado registra `uuid` + `major` y el nombre
   del contaminante que mide. El nodo se crea con `id_usuario` del token.
   - Si `(uuid, major)` ya existe y es de otro usuario y está activo → conflicto (`409`).
   - Si existe, es de otro usuario y está inactivo → se reactiva y se reasigna al usuario actual.
   - Si ya es del mismo usuario → operación idempotente.
4. **Insertar medición** (una sola transacción):
   - el nodo existe, está `activo` y pertenece al usuario autenticado;
   - `MEDICION.id_contaminante` coincide con `NODO_SENSOR.id_contaminante`;
   - `valor` dentro de `[CONTAMINANTE.valor_min, CONTAMINANTE.valor_max]`;
   - `latitud`/`longitud` válidas y presentes (si el móvil no tiene GPS, no se envía la lectura);
   - se inserta la fila y se actualiza `NODO_SENSOR.ultima_comunicacion = NOW()` (UTC).
5. **Idempotencia**: si el `UNIQUE (id_nodo, id_contaminante, fecha_hora)` rechaza la fila,
   se trata como éxito (el reintento del móvil no debe generar error ni duplicado).
6. **Estado derivado**: `desconectado` = `ultima_comunicacion IS NULL` o anterior a 24 h.
   Nunca se escribe en `estado`; `estado` solo refleja la decisión del administrador.
7. **Bajas**: `USUARIO.activo` y `NODO_SENSOR.activo` a `FALSE`. No se borran filas con histórico.
   Un usuario inactivo no puede hacer login ni enviar mediciones.
8. **Categoría de calidad**: se busca en `UMBRAL` el rango del contaminante que contiene `valor`
   (`valor_desde <= valor < valor_hasta`; `valor_hasta NULL` = sin techo).
   Si no encaja en ninguno, la categoría es `sin_datos` (gris `#95A5A6`, la resuelve la capa web).
9. **Consultas**: toda lectura de mediciones filtra por rango de `fecha_hora`, por nodo o por
   ventana geográfica, y excluye nodos con `activo = FALSE`.
10. **Comparativa oficial**: por `id_estacion` + `id_contaminante` + rango de fechas.
11. **Concurrencia**: inserción y actualización de `ultima_comunicacion` van en la misma
    transacción; no se leen-modifican contadores en memoria. Los `INSERT` deben ser atómicos
    y tolerar el `UNIQUE` (punto 5).

### Datos iniciales (seed)

Script idempotente (`INSERT ... ON DUPLICATE KEY UPDATE` o `WHERE NOT EXISTS`), con IDs fijos
para los catálogos:

+ **ROLES**: `1 admin`, `3 cliente`. No hay rol `tecnico`.
+ **CONTAMINANTE** (IDs fijos):

| id | nombre | unidad_medida | valor_min | valor_max |
| :--- | :--- | :--- | :--- | :--- |
| 1 | CO2 | ppm | 400 | 5000 |
| 2 | PM2.5 | µg/m³ | 0 | 500 |
| 3 | NO2 | ppb | 0 | 500 |
| 4 | O3 | ppb | 0 | 300 |
+ **UMBRAL** (4 categorías por contaminante, valores orientativos de demo):

| Contaminante | buena | moderada | mala | muy_mala |
| :--- | :--- | :--- | :--- | :--- |
| CO2 (ppm) | 0 – 1000 | 1000 – 1500 | 1500 – 2000 | > 2000 |
| PM2.5 (µg/m³) | 0 – 15 | 15 – 35 | 35 – 75 | > 75 |
| NO2 (ppb) | 0 – 40 | 40 – 100 | 100 – 200 | > 200 |
| O3 (ppb) | 0 – 50 | 50 – 100 | 100 – 150 | > 150 |

  Colores: `buena #2ECC71`, `moderada #F1C40F`, `mala #E67E22`, `muy_mala #E74C3C`.
+ **USUARIO demo**: `cliente@email.com`, rol `cliente`, `activo = TRUE`.
  El hash se genera con `php -r "echo password_hash('Demo1234!', PASSWORD_BCRYPT);"`
  y se pega en el script (nunca se guarda la contraseña en claro).
+ **ESTACION_OFICIAL**: 2 estaciones de Barcelona (IDs fijos, `id_externo` relleno).
+ **NODO_SENSOR**: 2 nodos de demo asignados al usuario demo, con `uuid` fijos.
+ **MEDICION**: ~20 filas repartidas alrededor de las estaciones para que el mapa y las
  gráficas tengan datos desde el primer arranque.

### Validación (tests automáticos)

`tests/check_schema.sql` consulta `information_schema` y falla (devuelve filas) si falta:
- alguna de las 8 tablas,
- alguna columna con su tipo y nulabilidad,
- alguna PK, UNIQUE o FK con su política `ON DELETE`,
- alguno de los índices de la sección *Índices*.

`tests/check_seed.sql` comprueba que los catálogos tienen las filas mínimas
(3 roles, 4 contaminantes, 16 umbrales) y que el usuario demo existe y está activo.

## Design Clarifications

| Decisión | Motivo |
| :--- | :--- |
| `NODO_SENSOR.identificador` → `uuid` + `major` + `UNIQUE(uuid, major)` | El `minor` del iBeacon lleva la medida, así que no puede formar parte de la identidad. |
| `minor_bruto` en `MEDICION` | Trazabilidad: permite recalibrar o auditar el dato original sin volver a muestrear. |
| `NODO_SENSOR.id_contaminante` | Cada nodo mide un gas fijo; la app necesita saber cuál y el backend puede validar coherencia. |
| `UNIQUE (nombre, id_usuario)` en `NODO_SENSOR` | Evita dos nodos con el mismo alias para un mismo usuario. Los nodos sin alias (`NULL`) no colisionan. |
| `foto_perfil` como ruta y no `LONGBLOB` | Evita inflar backups, memoria y respuestas; el binario va al sistema de ficheros. |
| `FLOAT` → `DECIMAL(9,6)` / `DECIMAL(10,3)` | Evita errores de redondeo en comparaciones y rangos. |
| `INT` → `BIGINT` en las tablas de mediciones | Crecimiento continuo por lectura de sensores. |
| `'desconectado'` fuera de `estado` | Se deriva de `ultima_comunicacion`; así no puede quedar desincronizado. |
| `activo` (soft delete) + `ON DELETE RESTRICT` | Conserva el histórico de mediciones y permite desactivar cuentas y nodos. |
| Tabla `UMBRAL` (nueva) | Clasificación y colores del mapa como datos, no como constantes en el código. |
| `valor_min` / `valor_max` en `CONTAMINANTE` | Permite validar lecturas en la lógica sin valores mágicos. |
| `UNIQUE` de idempotencia en `MEDICION` y `MEDICION_OFICIAL` | Los reintentos del móvil y las reimportaciones oficiales no duplican datos. |
| `id_externo` en `ESTACION_OFICIAL` | Permite importar de fuentes oficiales de forma repetible. |
| Índices compuestos y no espaciales | MariaDB no ofrece el `SRID` de MySQL 8; el *bounding box* cubre el mapa. |
| `DATETIME` en UTC | Evita conversiones implícitas de `TIMESTAMP` y unifica a web/Android/Arduino. |
| Sin tabla de sesiones | Autenticación JWT *stateless*; si hiciera falta revocación, se añadiría `SESION`. |
