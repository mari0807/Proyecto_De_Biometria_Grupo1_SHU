-- ============================================================================
-- QueAire_Biometria - Esquema: parte de usuarios
-- Diseno de referencia: Agentes_IA/Agente Backend/context/BBDD.md
-- Motor: MariaDB 10.4+ / InnoDB / utf8mb4_unicode_ci / fechas en UTC
-- Idempotente: se puede ejecutar varias veces sin efectos secundarios.
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
-- USUARIO: cuentas y credenciales de autenticacion
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
