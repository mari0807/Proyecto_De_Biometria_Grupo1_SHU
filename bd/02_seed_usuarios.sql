-- ============================================================================
-- QueAire_Biometria - Datos iniciales: roles y usuarios de prueba
-- Solo hay dos roles: admin y cliente.
-- Contrasena de los dos usuarios: 1234  (solo para pruebas)
-- Idempotente: reejecutar no duplica ni cambia el hash de la contrasena.
-- ============================================================================

USE QueAire_Biometria;

-- Catalogo de roles con IDs fijos para que nunca cambien.
INSERT INTO ROLES (id_rol, nombre, descripcion) VALUES
  (1, 'admin',   'Acceso total: gestion de usuarios, nodos y catalogo'),
  (3, 'cliente', 'Cliente: registra sus nodos y consulta mediciones')
ON DUPLICATE KEY UPDATE
  nombre      = VALUES(nombre),
  descripcion = VALUES(descripcion);

-- El rol tecnico no forma parte del producto. Primero la cuenta, luego el rol.
DELETE FROM USUARIO WHERE email = 'tecnico@email.com';
DELETE FROM ROLES WHERE nombre = 'tecnico';

-- Usuarios de prueba (password en claro: 1234).
INSERT INTO USUARIO (nombre, apellidos, email, password_hash, id_rol) VALUES
  ('Admin',   'QueAire', 'admin@email.com',
   '$2y$10$ZMY/mBqCKxeF4hbnnKxCkebVXdIp/xd7.FnoRE8GVj0TsQrcmh6eO', 1),
  ('Cliente', 'QueAire', 'cliente@email.com',
   '$2y$10$KstHsuP1QFlSL8.bDWugwORqQvNgnQrv4ZPI9hsQG5BOWdVpuOlXO', 3)
ON DUPLICATE KEY UPDATE
  nombre     = VALUES(nombre),
  apellidos  = VALUES(apellidos),
  id_rol     = VALUES(id_rol),
  activo     = TRUE;
