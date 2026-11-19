-- ============================================================================
-- QueAire_Biometria - Datos iniciales y datos de demo
-- ----------------------------------------------------------------------------
-- 1) Roles y usuarios de prueba
-- 2) Catalogo de contaminantes y umbrales de calidad
-- 3) Nodos y 48 mediciones de demo en el Grao de Gandia (mapa)
-- ----------------------------------------------------------------------------
-- Orden de ejecucion: 1) schema.sql  2) seed.sql
-- Idempotente: reejecutar no duplica. El bloque de demo borra solo los nodos
-- con uuid 00000000-0000-0000-0000-00000000000X y sus mediciones.
-- ============================================================================

USE QueAire_Biometria;
SET time_zone = '+00:00';

-- ----------------------------------------------------------------------------
-- 1) ROLES
-- ----------------------------------------------------------------------------
INSERT INTO ROLES (id_rol, nombre, descripcion) VALUES
  (1, 'admin',   'Acceso total: gestion de usuarios, nodos y catalogo'),
  (2, 'tecnico', 'Mantenimiento de nodos y estaciones oficiales'),
  (3, 'usuario', 'Usuario estandar: registra sus nodos y consulta mediciones')
ON DUPLICATE KEY UPDATE
  nombre      = VALUES(nombre),
  descripcion = VALUES(descripcion);

-- ----------------------------------------------------------------------------
-- 2) USUARIOS DE PRUEBA  (contrasena: 1234 para los tres)
-- ----------------------------------------------------------------------------
INSERT INTO USUARIO (nombre, apellidos, email, password_hash, id_rol) VALUES
  ('Admin',   'QueAire', 'admin@email.com',
   '$2y$10$ZMY/mBqCKxeF4hbnnKxCkebVXdIp/xd7.FnoRE8GVj0TsQrcmh6eO', 1),
  ('Tecnico', 'QueAire', 'tecnico@email.com',
   '$2y$10$q4q57LI1w1ZPd25y091kSuPtFDli7JjzYUlYEWdD0mYh4Rdm7Ivey', 2),
  ('Cliente', 'QueAire', 'cliente@email.com',
   '$2y$10$KstHsuP1QFlSL8.bDWugwORqQvNgnQrv4ZPI9hsQG5BOWdVpuOlXO', 3)
ON DUPLICATE KEY UPDATE
  nombre    = VALUES(nombre),
  apellidos = VALUES(apellidos),
  id_rol    = VALUES(id_rol),
  activo    = TRUE;

-- ----------------------------------------------------------------------------
-- 3) CONTAMINANTES (IDs fijos)
-- ----------------------------------------------------------------------------
INSERT INTO CONTAMINANTE (id_contaminante, nombre, unidad_medida, valor_min, valor_max) VALUES
  (1, 'CO2',   'ppm',   400.000, 5000.000),
  (2, 'PM2.5', 'µg/m³',   0.000,  500.000),
  (3, 'NO2',   'ppb',     0.000,  500.000),
  (4, 'O3',    'ppb',     0.000,  300.000)
ON DUPLICATE KEY UPDATE
  unidad_medida = VALUES(unidad_medida),
  valor_min     = VALUES(valor_min),
  valor_max     = VALUES(valor_max);

-- ----------------------------------------------------------------------------
-- 4) UMBRALES DE CALIDAD (4 categorias por contaminante)
-- ----------------------------------------------------------------------------
INSERT INTO UMBRAL (id_contaminante, categoria, valor_desde, valor_hasta, color) VALUES
  (1, 'buena',       0.000, 1000.000, '#2ECC71'),
  (1, 'moderada', 1000.000, 1500.000, '#F1C40F'),
  (1, 'mala',     1500.000, 2000.000, '#E67E22'),
  (1, 'muy_mala', 2000.000,     NULL, '#E74C3C'),
  (2, 'buena',       0.000,   15.000, '#2ECC71'),
  (2, 'moderada',   15.000,   35.000, '#F1C40F'),
  (2, 'mala',       35.000,   75.000, '#E67E22'),
  (2, 'muy_mala',   75.000,     NULL, '#E74C3C'),
  (3, 'buena',       0.000,   40.000, '#2ECC71'),
  (3, 'moderada',   40.000,  100.000, '#F1C40F'),
  (3, 'mala',      100.000,  200.000, '#E67E22'),
  (3, 'muy_mala',  200.000,     NULL, '#E74C3C'),
  (4, 'buena',       0.000,   50.000, '#2ECC71'),
  (4, 'moderada',   50.000,  100.000, '#F1C40F'),
  (4, 'mala',      100.000,  150.000, '#E67E22'),
  (4, 'muy_mala',  150.000,     NULL, '#E74C3C')
ON DUPLICATE KEY UPDATE
  valor_desde = VALUES(valor_desde),
  valor_hasta = VALUES(valor_hasta),
  color       = VALUES(color);

-- ----------------------------------------------------------------------------
-- 5) DATOS DE DEMO DEL MAPA
-- ----------------------------------------------------------------------------
-- Propietario de los nodos de demo (se busca por email, no por id fijo).
SET @id_cliente = (SELECT id_usuario FROM USUARIO WHERE email = 'cliente@email.com');

-- Limpieza de la demo anterior (solo nodos de demo y sus mediciones).
DELETE m FROM MEDICION m
  INNER JOIN NODO_SENSOR n ON n.id_nodo = m.id_nodo
 WHERE n.uuid IN ('00000000-0000-0000-0000-000000000001',
                  '00000000-0000-0000-0000-000000000002',
                  '00000000-0000-0000-0000-000000000003',
                  '00000000-0000-0000-0000-000000000004');

DELETE FROM NODO_SENSOR
 WHERE uuid IN ('00000000-0000-0000-0000-000000000001',
                '00000000-0000-0000-0000-000000000002',
                '00000000-0000-0000-0000-000000000003',
                '00000000-0000-0000-0000-000000000004');

-- Nodos de demo:
--   1 Campus UPV      -> CO2   (limpio)
--   2 Port de Gandia  -> NO2   (trafico maritimo y pesado)
--   3 Av. del Port    -> PM2.5 (medio)
--   4 Centre del Grau -> O3    (mixto)
INSERT INTO NODO_SENSOR
  (id_nodo, uuid, major, nombre, id_contaminante, id_usuario, estado, ultima_comunicacion, activo) VALUES
  (1, '00000000-0000-0000-0000-000000000001', 1, 'Campus UPV',      1, @id_cliente, 'activo', DATE_SUB(UTC_TIMESTAMP(), INTERVAL 2 HOUR), TRUE),
  (2, '00000000-0000-0000-0000-000000000002', 2, 'Port de Gandia',  3, @id_cliente, 'activo', DATE_SUB(UTC_TIMESTAMP(), INTERVAL 2 HOUR), TRUE),
  (3, '00000000-0000-0000-0000-000000000003', 3, 'Av. del Port',    2, @id_cliente, 'activo', DATE_SUB(UTC_TIMESTAMP(), INTERVAL 2 HOUR), TRUE),
  (4, '00000000-0000-0000-0000-000000000004', 4, 'Centre del Grau', 4, @id_cliente, 'activo', DATE_SUB(UTC_TIMESTAMP(), INTERVAL 2 HOUR), TRUE)
ON DUPLICATE KEY UPDATE
  nombre              = VALUES(nombre),
  id_contaminante     = VALUES(id_contaminante),
  id_usuario          = VALUES(id_usuario),
  estado              = VALUES(estado),
  ultima_comunicacion = VALUES(ultima_comunicacion),
  activo              = VALUES(activo);

-- 48 mediciones: 12 por nodo, una cada 2 h en las ultimas 24 h.
-- minor_bruto = ROUND(valor * 10), segun el contrato del beacon.

-- Nodo 1 - Campus UPV - CO2 (ppm): 8 buena, 3 moderada, 1 mala
INSERT INTO MEDICION (id_nodo, id_contaminante, valor, minor_bruto, fecha_hora, latitud, longitud) VALUES
  (1, 1,  610.000,  6100, DATE_SUB(UTC_TIMESTAMP(), INTERVAL  2 HOUR), 38.996460, -0.166030),
  (1, 1,  520.000,  5200, DATE_SUB(UTC_TIMESTAMP(), INTERVAL  4 HOUR), 38.997100, -0.165600),
  (1, 1,  480.000,  4800, DATE_SUB(UTC_TIMESTAMP(), INTERVAL  6 HOUR), 38.997800, -0.166400),
  (1, 1,  590.000,  5900, DATE_SUB(UTC_TIMESTAMP(), INTERVAL  8 HOUR), 38.996000, -0.164800),
  (1, 1,  700.000,  7000, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 10 HOUR), 38.995500, -0.166900),
  (1, 1, 1050.000, 10500, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 12 HOUR), 38.998300, -0.165200),
  (1, 1,  660.000,  6600, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 14 HOUR), 38.994900, -0.165600),
  (1, 1,  630.000,  6300, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 16 HOUR), 38.996800, -0.167500),
  (1, 1, 1180.000, 11800, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 18 HOUR), 38.997400, -0.167100),
  (1, 1,  550.000,  5500, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 20 HOUR), 38.995200, -0.164500),
  (1, 1, 1320.000, 13200, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 22 HOUR), 38.998600, -0.166800),
  (1, 1, 1650.000, 16500, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 24 HOUR), 38.994600, -0.166300);

-- Nodo 2 - Port de Gandia - NO2 (ppb): 3 moderada, 5 mala, 4 muy mala
INSERT INTO MEDICION (id_nodo, id_contaminante, valor, minor_bruto, fecha_hora, latitud, longitud) VALUES
  (2, 3, 180.000, 1800, DATE_SUB(UTC_TIMESTAMP(), INTERVAL  2 HOUR), 38.974800, -0.165500),
  (2, 3, 215.000, 2150, DATE_SUB(UTC_TIMESTAMP(), INTERVAL  4 HOUR), 38.975600, -0.165000),
  (2, 3, 160.000, 1600, DATE_SUB(UTC_TIMESTAMP(), INTERVAL  6 HOUR), 38.973900, -0.166200),
  (2, 3, 110.000, 1100, DATE_SUB(UTC_TIMESTAMP(), INTERVAL  8 HOUR), 38.976300, -0.166000),
  (2, 3, 240.000, 2400, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 10 HOUR), 38.973200, -0.165400),
  (2, 3,  95.000,  950, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 12 HOUR), 38.975000, -0.167100),
  (2, 3, 195.000, 1950, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 14 HOUR), 38.974200, -0.164800),
  (2, 3, 275.000, 2750, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 16 HOUR), 38.976800, -0.165400),
  (2, 3, 135.000, 1350, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 18 HOUR), 38.972800, -0.166000),
  (2, 3,  72.000,  720, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 20 HOUR), 38.975700, -0.164600),
  (2, 3, 310.000, 3100, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 22 HOUR), 38.973600, -0.167400),
  (2, 3,  55.000,  550, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 24 HOUR), 38.976200, -0.166900);

-- Nodo 3 - Av. del Port - PM2.5 (µg/m³): 2 buena, 5 moderada, 4 mala, 1 muy mala
INSERT INTO MEDICION (id_nodo, id_contaminante, valor, minor_bruto, fecha_hora, latitud, longitud) VALUES
  (3, 2, 31.000,  310, DATE_SUB(UTC_TIMESTAMP(), INTERVAL  2 HOUR), 38.990800, -0.158000),
  (3, 2, 40.000,  400, DATE_SUB(UTC_TIMESTAMP(), INTERVAL  4 HOUR), 38.991500, -0.158500),
  (3, 2, 22.000,  220, DATE_SUB(UTC_TIMESTAMP(), INTERVAL  6 HOUR), 38.990100, -0.157600),
  (3, 2, 12.000,  120, DATE_SUB(UTC_TIMESTAMP(), INTERVAL  8 HOUR), 38.992200, -0.158200),
  (3, 2, 58.000,  580, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 10 HOUR), 38.989400, -0.158600),
  (3, 2, 34.000,  340, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 12 HOUR), 38.991000, -0.159000),
  (3, 2, 48.000,  480, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 14 HOUR), 38.989800, -0.157800),
  (3, 2, 18.000,  180, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 16 HOUR), 38.992600, -0.159000),
  (3, 2, 70.000,  700, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 18 HOUR), 38.988700, -0.158200),
  (3, 2, 27.000,  270, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 20 HOUR), 38.990300, -0.159400),
  (3, 2, 88.000,  880, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 22 HOUR), 38.992900, -0.157900),
  (3, 2,  8.000,   80, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 24 HOUR), 38.989100, -0.159200);

-- Nodo 4 - Centre del Grau - O3 (ppb): 3 buena, 3 moderada, 3 mala, 3 muy mala
INSERT INTO MEDICION (id_nodo, id_contaminante, valor, minor_bruto, fecha_hora, latitud, longitud) VALUES
  (4, 4,  46.000,  460, DATE_SUB(UTC_TIMESTAMP(), INTERVAL  2 HOUR), 38.982500, -0.160500),
  (4, 4,  90.000,  900, DATE_SUB(UTC_TIMESTAMP(), INTERVAL  4 HOUR), 38.983200, -0.161200),
  (4, 4, 110.000, 1100, DATE_SUB(UTC_TIMESTAMP(), INTERVAL  6 HOUR), 38.981800, -0.159900),
  (4, 4, 160.000, 1600, DATE_SUB(UTC_TIMESTAMP(), INTERVAL  8 HOUR), 38.984000, -0.160800),
  (4, 4,  72.000,  720, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 10 HOUR), 38.981200, -0.161000),
  (4, 4, 130.000, 1300, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 12 HOUR), 38.983500, -0.159700),
  (4, 4,  25.000,  250, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 14 HOUR), 38.982000, -0.162000),
  (4, 4, 175.000, 1750, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 16 HOUR), 38.984600, -0.161600),
  (4, 4,  55.000,  550, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 18 HOUR), 38.980800, -0.159800),
  (4, 4, 148.000, 1480, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 20 HOUR), 38.983000, -0.162300),
  (4, 4, 210.000, 2100, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 22 HOUR), 38.985000, -0.160200),
  (4, 4,  38.000,  380, DATE_SUB(UTC_TIMESTAMP(), INTERVAL 24 HOUR), 38.981500, -0.160500);
