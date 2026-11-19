-- ============================================================================
-- QueAire_Biometria - Test de esquema
-- ----------------------------------------------------------------------------
-- Comprueba que la base de datos real coincide con el diseno de BBDD.md.
-- Solo lectura: no modifica datos (usa una tabla temporal para acumular fallos).
-- Uso:  mysql -u root < database/tests/check_schema.sql
-- Interpretacion: total_problemas = 0 significa esquema correcto.
-- ============================================================================

USE QueAire_Biometria;

DROP TEMPORARY TABLE IF EXISTS _problemas;
CREATE TEMPORARY TABLE _problemas (
  tipo    VARCHAR(40)  NOT NULL,
  detalle VARCHAR(200) NOT NULL
);

-- ----------------------------------------------------------------------------
-- 1) Tablas
-- ----------------------------------------------------------------------------
INSERT INTO _problemas (tipo, detalle)
SELECT 'TABLA_FALTANTE', t.nombre
  FROM (SELECT 'ROLES' AS nombre
        UNION ALL SELECT 'USUARIO'
        UNION ALL SELECT 'CONTAMINANTE'
        UNION ALL SELECT 'UMBRAL'
        UNION ALL SELECT 'NODO_SENSOR'
        UNION ALL SELECT 'MEDICION') t
 WHERE NOT EXISTS (
   SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND LOWER(TABLE_NAME) = LOWER(t.nombre));

-- ----------------------------------------------------------------------------
-- 2) Columnas por tabla
-- ----------------------------------------------------------------------------
INSERT INTO _problemas (tipo, detalle)
SELECT 'COLUMNA_FALTANTE', CONCAT('ROLES.', c.col)
  FROM (SELECT 'id_rol' AS col UNION ALL SELECT 'nombre' UNION ALL SELECT 'descripcion') c
 WHERE NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'roles'
                      AND COLUMN_NAME = c.col);

INSERT INTO _problemas (tipo, detalle)
SELECT 'COLUMNA_FALTANTE', CONCAT('USUARIO.', c.col)
  FROM (SELECT 'id_usuario' AS col UNION ALL SELECT 'nombre' UNION ALL SELECT 'apellidos'
        UNION ALL SELECT 'email' UNION ALL SELECT 'password_hash' UNION ALL SELECT 'id_rol'
        UNION ALL SELECT 'foto_perfil' UNION ALL SELECT 'activo'
        UNION ALL SELECT 'fecha_registro' UNION ALL SELECT 'ultimo_acceso') c
 WHERE NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'usuario'
                      AND COLUMN_NAME = c.col);

INSERT INTO _problemas (tipo, detalle)
SELECT 'COLUMNA_FALTANTE', CONCAT('CONTAMINANTE.', c.col)
  FROM (SELECT 'id_contaminante' AS col UNION ALL SELECT 'nombre' UNION ALL SELECT 'unidad_medida'
        UNION ALL SELECT 'valor_min' UNION ALL SELECT 'valor_max') c
 WHERE NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'contaminante'
                      AND COLUMN_NAME = c.col);

INSERT INTO _problemas (tipo, detalle)
SELECT 'COLUMNA_FALTANTE', CONCAT('UMBRAL.', c.col)
  FROM (SELECT 'id_umbral' AS col UNION ALL SELECT 'id_contaminante' UNION ALL SELECT 'categoria'
        UNION ALL SELECT 'valor_desde' UNION ALL SELECT 'valor_hasta' UNION ALL SELECT 'color') c
 WHERE NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'umbral'
                      AND COLUMN_NAME = c.col);

INSERT INTO _problemas (tipo, detalle)
SELECT 'COLUMNA_FALTANTE', CONCAT('NODO_SENSOR.', c.col)
  FROM (SELECT 'id_nodo' AS col UNION ALL SELECT 'uuid' UNION ALL SELECT 'major'
        UNION ALL SELECT 'nombre' UNION ALL SELECT 'id_contaminante' UNION ALL SELECT 'id_usuario'
        UNION ALL SELECT 'estado' UNION ALL SELECT 'ultima_comunicacion'
        UNION ALL SELECT 'activo' UNION ALL SELECT 'fecha_alta') c
 WHERE NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'nodo_sensor'
                      AND COLUMN_NAME = c.col);

INSERT INTO _problemas (tipo, detalle)
SELECT 'COLUMNA_FALTANTE', CONCAT('MEDICION.', c.col)
  FROM (SELECT 'id_medicion' AS col UNION ALL SELECT 'id_nodo' UNION ALL SELECT 'id_contaminante'
        UNION ALL SELECT 'valor' UNION ALL SELECT 'minor_bruto' UNION ALL SELECT 'fecha_hora'
        UNION ALL SELECT 'latitud' UNION ALL SELECT 'longitud') c
 WHERE NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'medicion'
                      AND COLUMN_NAME = c.col);

-- ----------------------------------------------------------------------------
-- 3) Tipos de datos criticos
-- ----------------------------------------------------------------------------
INSERT INTO _problemas (tipo, detalle)
SELECT 'TIPO_INCORRECTO', 'MEDICION.id_medicion (BIGINT UNSIGNED)'
 WHERE NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'medicion'
                      AND COLUMN_NAME = 'id_medicion' AND DATA_TYPE = 'bigint'
                      AND COLUMN_TYPE LIKE '%unsigned%');

INSERT INTO _problemas (tipo, detalle)
SELECT 'TIPO_INCORRECTO', 'MEDICION.valor (DECIMAL(10,3))'
 WHERE NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'medicion'
                      AND COLUMN_NAME = 'valor' AND DATA_TYPE = 'decimal'
                      AND NUMERIC_PRECISION = 10 AND NUMERIC_SCALE = 3);

INSERT INTO _problemas (tipo, detalle)
SELECT 'TIPO_INCORRECTO', 'MEDICION.latitud/longitud (DECIMAL(9,6))'
 WHERE NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'medicion'
                      AND COLUMN_NAME = 'latitud' AND DATA_TYPE = 'decimal'
                      AND NUMERIC_PRECISION = 9 AND NUMERIC_SCALE = 6)
    OR NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'medicion'
                      AND COLUMN_NAME = 'longitud' AND DATA_TYPE = 'decimal'
                      AND NUMERIC_PRECISION = 9 AND NUMERIC_SCALE = 6);

INSERT INTO _problemas (tipo, detalle)
SELECT 'TIPO_INCORRECTO', 'NODO_SENSOR.uuid (CHAR(36))'
 WHERE NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'nodo_sensor'
                      AND COLUMN_NAME = 'uuid' AND DATA_TYPE = 'char'
                      AND CHARACTER_MAXIMUM_LENGTH = 36);

INSERT INTO _problemas (tipo, detalle)
SELECT 'TIPO_INCORRECTO', 'NODO_SENSOR.major (SMALLINT UNSIGNED)'
 WHERE NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'nodo_sensor'
                      AND COLUMN_NAME = 'major' AND DATA_TYPE = 'smallint'
                      AND COLUMN_TYPE LIKE '%unsigned%');

INSERT INTO _problemas (tipo, detalle)
SELECT 'TIPO_INCORRECTO', 'USUARIO.password_hash (VARCHAR(255))'
 WHERE NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'usuario'
                      AND COLUMN_NAME = 'password_hash' AND DATA_TYPE = 'varchar'
                      AND CHARACTER_MAXIMUM_LENGTH = 255);

-- ----------------------------------------------------------------------------
-- 4) Claves primarias
-- ----------------------------------------------------------------------------
INSERT INTO _problemas (tipo, detalle)
SELECT 'PK_INCORRECTA', t.tabla
  FROM (SELECT 'ROLES' AS tabla, 'id_rol' AS col
        UNION ALL SELECT 'USUARIO', 'id_usuario'
        UNION ALL SELECT 'CONTAMINANTE', 'id_contaminante'
        UNION ALL SELECT 'UMBRAL', 'id_umbral'
        UNION ALL SELECT 'NODO_SENSOR', 'id_nodo'
        UNION ALL SELECT 'MEDICION', 'id_medicion') t
 WHERE NOT EXISTS (
   SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = LOWER(t.tabla)
      AND INDEX_NAME = 'PRIMARY' AND COLUMN_NAME = t.col);

-- ----------------------------------------------------------------------------
-- 5) Claves foraneas (columna, destino y ON DELETE)
-- ----------------------------------------------------------------------------
INSERT INTO _problemas (tipo, detalle)
SELECT 'FK_FALTANTE', f.tabla
  FROM (SELECT 'USUARIO.id_rol -> ROLES' AS tabla, 'USUARIO' AS t, 'id_rol' AS col,
               'ROLES' AS rt, 'id_rol' AS rcol, 'RESTRICT' AS del
        UNION ALL SELECT 'UMBRAL.id_contaminante -> CONTAMINANTE', 'UMBRAL', 'id_contaminante', 'CONTAMINANTE', 'id_contaminante', 'CASCADE'
        UNION ALL SELECT 'NODO_SENSOR.id_contaminante -> CONTAMINANTE', 'NODO_SENSOR', 'id_contaminante', 'CONTAMINANTE', 'id_contaminante', 'RESTRICT'
        UNION ALL SELECT 'NODO_SENSOR.id_usuario -> USUARIO', 'NODO_SENSOR', 'id_usuario', 'USUARIO', 'id_usuario', 'RESTRICT'
        UNION ALL SELECT 'MEDICION.id_nodo -> NODO_SENSOR', 'MEDICION', 'id_nodo', 'NODO_SENSOR', 'id_nodo', 'RESTRICT'
        UNION ALL SELECT 'MEDICION.id_contaminante -> CONTAMINANTE', 'MEDICION', 'id_contaminante', 'CONTAMINANTE', 'id_contaminante', 'RESTRICT') f
 WHERE NOT EXISTS (
   SELECT 1
     FROM information_schema.KEY_COLUMN_USAGE k
     JOIN information_schema.REFERENTIAL_CONSTRAINTS r
       ON r.CONSTRAINT_SCHEMA = k.CONSTRAINT_SCHEMA
      AND r.CONSTRAINT_NAME   = k.CONSTRAINT_NAME
    WHERE k.TABLE_SCHEMA = DATABASE()
      AND LOWER(k.TABLE_NAME) = LOWER(f.t)
      AND k.COLUMN_NAME = f.col
      AND LOWER(k.REFERENCED_TABLE_NAME) = LOWER(f.rt)
      AND k.REFERENCED_COLUMN_NAME = f.rcol
      AND r.DELETE_RULE = f.del);

-- ----------------------------------------------------------------------------
-- 6) Indices UNIQUE y de rendimiento
-- ----------------------------------------------------------------------------
INSERT INTO _problemas (tipo, detalle)
SELECT 'UNIQUE_FALTANTE', u.detalle
  FROM (SELECT 'USUARIO.uq_usuario_email' AS detalle, 'USUARIO' AS t, 'uq_usuario_email' AS idx
        UNION ALL SELECT 'ROLES.uq_roles_nombre', 'ROLES', 'uq_roles_nombre'
        UNION ALL SELECT 'CONTAMINANTE.uq_contaminante_nombre', 'CONTAMINANTE', 'uq_contaminante_nombre'
        UNION ALL SELECT 'UMBRAL.uq_umbral_categoria', 'UMBRAL', 'uq_umbral_categoria'
        UNION ALL SELECT 'NODO_SENSOR.uq_nodo_identidad', 'NODO_SENSOR', 'uq_nodo_identidad'
        UNION ALL SELECT 'NODO_SENSOR.uq_nodo_nombre_usuario', 'NODO_SENSOR', 'uq_nodo_nombre_usuario'
        UNION ALL SELECT 'MEDICION.uq_medicion_nodo_fecha', 'MEDICION', 'uq_medicion_nodo_fecha') u
 WHERE NOT EXISTS (
   SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = LOWER(u.t)
      AND INDEX_NAME = u.idx AND NON_UNIQUE = 0);

INSERT INTO _problemas (tipo, detalle)
SELECT 'INDICE_FALTANTE', i.detalle
  FROM (SELECT 'USUARIO.idx_usuario_rol' AS detalle, 'USUARIO' AS t, 'idx_usuario_rol' AS idx
        UNION ALL SELECT 'UMBRAL.idx_umbral_rango', 'UMBRAL', 'idx_umbral_rango'
        UNION ALL SELECT 'NODO_SENSOR.idx_nodo_contaminante', 'NODO_SENSOR', 'idx_nodo_contaminante'
        UNION ALL SELECT 'NODO_SENSOR.idx_nodo_usuario', 'NODO_SENSOR', 'idx_nodo_usuario'
        UNION ALL SELECT 'MEDICION.idx_medicion_contaminante_fecha', 'MEDICION', 'idx_medicion_contaminante_fecha'
        UNION ALL SELECT 'MEDICION.idx_medicion_nodo_fecha', 'MEDICION', 'idx_medicion_nodo_fecha'
        UNION ALL SELECT 'MEDICION.idx_medicion_ubicacion', 'MEDICION', 'idx_medicion_ubicacion') i
 WHERE NOT EXISTS (
   SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = LOWER(i.t)
      AND INDEX_NAME = i.idx);

-- ----------------------------------------------------------------------------
-- 7) Restricciones CHECK
-- ----------------------------------------------------------------------------
INSERT INTO _problemas (tipo, detalle)
SELECT 'CHECK_FALTANTE', c.detalle
  FROM (SELECT 'USUARIO.ck_usuario_email' AS detalle, 'ck_usuario_email' AS nombre
        UNION ALL SELECT 'CONTAMINANTE.ck_contaminante_rango', 'ck_contaminante_rango'
        UNION ALL SELECT 'UMBRAL.ck_umbral_categoria', 'ck_umbral_categoria'
        UNION ALL SELECT 'UMBRAL.ck_umbral_rango', 'ck_umbral_rango'
        UNION ALL SELECT 'UMBRAL.ck_umbral_color', 'ck_umbral_color'
        UNION ALL SELECT 'NODO_SENSOR.ck_nodo_estado', 'ck_nodo_estado'
        UNION ALL SELECT 'MEDICION.ck_medicion_valor', 'ck_medicion_valor'
        UNION ALL SELECT 'MEDICION.ck_medicion_latitud', 'ck_medicion_latitud'
        UNION ALL SELECT 'MEDICION.ck_medicion_longitud', 'ck_medicion_longitud') c
 WHERE NOT EXISTS (
   SELECT 1 FROM information_schema.CHECK_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = c.nombre);

-- ----------------------------------------------------------------------------
-- 8) Charset de las tablas
-- ----------------------------------------------------------------------------
INSERT INTO _problemas (tipo, detalle)
SELECT 'CHARSET_INCORRECTO', t.TABLE_NAME
  FROM information_schema.TABLES t
 WHERE t.TABLE_SCHEMA = DATABASE()
   AND LOWER(t.TABLE_NAME) IN ('roles', 'usuario', 'contaminante', 'umbral', 'nodo_sensor', 'medicion')
   AND t.TABLE_COLLATION <> 'utf8mb4_unicode_ci';

-- ----------------------------------------------------------------------------
-- Resultado
-- ----------------------------------------------------------------------------
SELECT tipo, detalle FROM _problemas ORDER BY tipo, detalle;
SELECT COUNT(*) AS total_problemas FROM _problemas;

DROP TEMPORARY TABLE IF EXISTS _problemas;
