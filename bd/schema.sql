-- ============================================================================
-- QueAire_Biometria - Esquema completo de la base de datos
-- ----------------------------------------------------------------------------
-- Motor:   MariaDB 10.4+ (XAMPP) / InnoDB
-- Charset: utf8mb4 / utf8mb4_unicode_ci
-- Fechas:  UTC (tipo DATETIME)
-- Orden de ejecucion: 1) bd/schema.sql  2) bd/seed.sql
-- Idempotente: se puede ejecutar varias veces sin efectos secundarios.
-- Diseno de referencia: Agentes_IA/Agente Backend/context/BBDD.md
-- ============================================================================

CREATE DATABASE IF NOT EXISTS QueAire_Biometria
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE QueAire_Biometria;

-- ----------------------------------------------------------------------------
-- ROLES: catalogo de perfiles de permisos
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ROLES (
  id_rol      INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre      VARCHAR(30)  NOT NULL,
  descripcion VARCHAR(255) NULL,
  PRIMARY KEY (id_rol),
  UNIQUE KEY uq_roles_nombre (nombre)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- USUARIO: cuentas y credenciales
--   - password_hash: bcrypt generado con password_hash() de PHP
--   - foto_perfil: ruta relativa al fichero, nunca el binario
--   - activo: borrado logico (soft delete)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS USUARIO (
  id_usuario     INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre         VARCHAR(100) NOT NULL,
  apellidos      VARCHAR(150) NOT NULL,
  email          VARCHAR(254) NOT NULL,
  password_hash  VARCHAR(255) NOT NULL,
  id_rol         INT UNSIGNED NOT NULL,
  foto_perfil    VARCHAR(255) NULL,
  activo         BOOLEAN      NOT NULL DEFAULT TRUE,
  fecha_registro DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ultimo_acceso  DATETIME     NULL,
  PRIMARY KEY (id_usuario),
  UNIQUE KEY uq_usuario_email (email),
  KEY idx_usuario_rol (id_rol),
  CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol)
    REFERENCES ROLES (id_rol) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_usuario_email CHECK (email LIKE '%_@_%._%')
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- CONTAMINANTE: catalogo de gases / parametros ambientales
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS CONTAMINANTE (
  id_contaminante INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  nombre          VARCHAR(50)   NOT NULL,
  unidad_medida   VARCHAR(20)   NOT NULL,
  valor_min       DECIMAL(10,3) NOT NULL DEFAULT 0.000,
  valor_max       DECIMAL(10,3) NOT NULL,
  PRIMARY KEY (id_contaminante),
  UNIQUE KEY uq_contaminante_nombre (nombre),
  CONSTRAINT ck_contaminante_rango CHECK (valor_max > valor_min)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- UMBRAL: categorias de calidad por contaminante (colores del mapa)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS UMBRAL (
  id_umbral       INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  id_contaminante INT UNSIGNED  NOT NULL,
  categoria       VARCHAR(20)   NOT NULL,
  valor_desde     DECIMAL(10,3) NOT NULL,
  valor_hasta     DECIMAL(10,3) NULL,
  color           CHAR(7)       NOT NULL,
  PRIMARY KEY (id_umbral),
  UNIQUE KEY uq_umbral_categoria (id_contaminante, categoria),
  KEY idx_umbral_rango (id_contaminante, valor_desde),
  CONSTRAINT fk_umbral_contaminante FOREIGN KEY (id_contaminante)
    REFERENCES CONTAMINANTE (id_contaminante) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT ck_umbral_categoria
    CHECK (categoria IN ('buena', 'moderada', 'mala', 'muy_mala')),
  CONSTRAINT ck_umbral_rango CHECK (valor_hasta IS NULL OR valor_hasta > valor_desde),
  CONSTRAINT ck_umbral_color CHECK (color LIKE '#______')
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- NODO_SENSOR: nodos BLE; identidad = (uuid, major); el minor lleva la medida
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS NODO_SENSOR (
  id_nodo             INT UNSIGNED      NOT NULL AUTO_INCREMENT,
  uuid                CHAR(36)          NOT NULL,
  major               SMALLINT UNSIGNED NOT NULL,
  nombre              VARCHAR(100)      NULL,
  id_contaminante     INT UNSIGNED      NOT NULL,
  id_usuario          INT UNSIGNED      NOT NULL,
  estado              VARCHAR(20)       NOT NULL DEFAULT 'activo',
  ultima_comunicacion DATETIME          NULL,
  activo              BOOLEAN           NOT NULL DEFAULT TRUE,
  fecha_alta          DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_nodo),
  UNIQUE KEY uq_nodo_identidad (uuid, major),
  UNIQUE KEY uq_nodo_nombre_usuario (nombre, id_usuario),
  KEY idx_nodo_contaminante (id_contaminante),
  KEY idx_nodo_usuario (id_usuario),
  CONSTRAINT fk_nodo_contaminante FOREIGN KEY (id_contaminante)
    REFERENCES CONTAMINANTE (id_contaminante) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_nodo_usuario FOREIGN KEY (id_usuario)
    REFERENCES USUARIO (id_usuario) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_nodo_estado
    CHECK (estado IN ('activo', 'inactivo', 'mantenimiento'))
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- MEDICION: lecturas geolocalizadas de los nodos de usuario
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS MEDICION (
  id_medicion     BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
  id_nodo         INT UNSIGNED      NOT NULL,
  id_contaminante INT UNSIGNED      NOT NULL,
  valor           DECIMAL(10,3)     NOT NULL,
  minor_bruto     SMALLINT UNSIGNED NULL,
  fecha_hora      DATETIME          NOT NULL,
  latitud         DECIMAL(9,6)      NOT NULL,
  longitud        DECIMAL(9,6)      NOT NULL,
  PRIMARY KEY (id_medicion),
  UNIQUE KEY uq_medicion_nodo_fecha (id_nodo, id_contaminante, fecha_hora),
  KEY idx_medicion_contaminante_fecha (id_contaminante, fecha_hora),
  KEY idx_medicion_nodo_fecha (id_nodo, fecha_hora),
  KEY idx_medicion_ubicacion (latitud, longitud),
  CONSTRAINT fk_medicion_nodo FOREIGN KEY (id_nodo)
    REFERENCES NODO_SENSOR (id_nodo) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_medicion_contaminante FOREIGN KEY (id_contaminante)
    REFERENCES CONTAMINANTE (id_contaminante) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_medicion_valor CHECK (valor >= 0),
  CONSTRAINT ck_medicion_latitud CHECK (latitud BETWEEN -90 AND 90),
  CONSTRAINT ck_medicion_longitud CHECK (longitud BETWEEN -180 AND 180)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- ESTACION_OFICIAL y MEDICION_OFICIAL
-- Se anadiran mas adelante (06_estaciones_oficiales.sql). No se crean aqui.
-- ----------------------------------------------------------------------------
