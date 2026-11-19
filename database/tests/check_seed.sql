-- ============================================================================
-- QueAire_Biometria - Test de datos iniciales y demo
-- ----------------------------------------------------------------------------
-- Solo lectura. Comprueba usuarios de prueba, catalogos y datos del mapa.
-- Uso:  mysql -u root < database/tests/check_seed.sql
-- ============================================================================

USE QueAire_Biometria;
SET time_zone = '+00:00';

-- ----------------------------------------------------------------------------
-- 1) Recuento de filas
-- ----------------------------------------------------------------------------
SELECT 'CONTAMINANTE' AS tabla, COUNT(*) AS filas FROM CONTAMINANTE
UNION ALL SELECT 'UMBRAL',      COUNT(*) FROM UMBRAL
UNION ALL SELECT 'NODO_SENSOR', COUNT(*) FROM NODO_SENSOR
UNION ALL SELECT 'MEDICION',    COUNT(*) FROM MEDICION;

-- ----------------------------------------------------------------------------
-- 2) Usuarios de prueba (3, activos y con rol asignado)
-- ----------------------------------------------------------------------------
SELECT u.id_usuario, u.email, r.nombre AS rol, u.activo,
       LENGTH(u.password_hash) AS longitud_hash
  FROM USUARIO u
  JOIN ROLES r ON r.id_rol = u.id_rol
 WHERE u.email IN ('admin@email.com', 'tecnico@email.com', 'cliente@email.com')
 ORDER BY u.id_usuario;

-- ----------------------------------------------------------------------------
-- 3) Lecturas por nodo de demo (deben ser 12 cada uno)
-- ----------------------------------------------------------------------------
SELECT n.id_nodo, n.nombre, c.nombre AS gas, COUNT(*) AS lecturas,
       MIN(m.fecha_hora) AS primera, MAX(m.fecha_hora) AS ultima
  FROM MEDICION m
  JOIN NODO_SENSOR n ON n.id_nodo = m.id_nodo
  JOIN CONTAMINANTE c ON c.id_contaminante = n.id_contaminante
 WHERE n.uuid LIKE '00000000-0000-0000-0000-00000000000%'
 GROUP BY n.id_nodo, n.nombre, c.nombre
 ORDER BY n.id_nodo;

-- ----------------------------------------------------------------------------
-- 4) Distribucion por categoria (deben aparecer las 4)
-- ----------------------------------------------------------------------------
SELECT u.categoria, u.color, COUNT(*) AS mediciones
  FROM MEDICION m
  JOIN UMBRAL u ON u.id_contaminante = m.id_contaminante
   AND m.valor >= u.valor_desde
   AND (u.valor_hasta IS NULL OR m.valor < u.valor_hasta)
 GROUP BY u.categoria, u.color
 ORDER BY FIELD(u.categoria, 'buena', 'moderada', 'mala', 'muy_mala');

-- ----------------------------------------------------------------------------
-- 5) Caja geografica (Grao de Gandia: lat 38.97..39.00, lon -0.17..-0.15)
-- ----------------------------------------------------------------------------
SELECT MIN(latitud) AS lat_min, MAX(latitud) AS lat_max,
       MIN(longitud) AS lon_min, MAX(longitud) AS lon_max
  FROM MEDICION;

-- ----------------------------------------------------------------------------
-- 6) Integridad de los datos
-- ----------------------------------------------------------------------------
SELECT
  (SELECT COUNT(*) FROM MEDICION m
     JOIN NODO_SENSOR n ON n.id_nodo = m.id_nodo
    WHERE m.id_contaminante <> n.id_contaminante) AS contaminante_incoherente,
  (SELECT COUNT(*) FROM MEDICION
    WHERE minor_bruto IS NULL OR minor_bruto <> ROUND(valor * 10)) AS minor_incoherente,
  (SELECT COUNT(*) FROM MEDICION m
     JOIN CONTAMINANTE c ON c.id_contaminante = m.id_contaminante
    WHERE m.valor < c.valor_min OR m.valor > c.valor_max) AS valor_fuera_rango,
  (SELECT COUNT(*) FROM MEDICION m
    WHERE NOT EXISTS (
      SELECT 1 FROM UMBRAL u
       WHERE u.id_contaminante = m.id_contaminante
         AND m.valor >= u.valor_desde
         AND (u.valor_hasta IS NULL OR m.valor < u.valor_hasta))) AS sin_categoria;

-- ----------------------------------------------------------------------------
-- 7) Muestra de los 5 puntos mas recientes
-- ----------------------------------------------------------------------------
SELECT m.id_medicion, n.nombre AS nodo, c.nombre AS gas,
       CONCAT(m.valor, ' ', c.unidad_medida) AS valor,
       u.categoria, u.color, m.fecha_hora, m.latitud, m.longitud
  FROM MEDICION m
  JOIN NODO_SENSOR n ON n.id_nodo = m.id_nodo
  JOIN CONTAMINANTE c ON c.id_contaminante = m.id_contaminante
  JOIN UMBRAL u ON u.id_contaminante = m.id_contaminante
   AND m.valor >= u.valor_desde
   AND (u.valor_hasta IS NULL OR m.valor < u.valor_hasta)
 ORDER BY m.fecha_hora DESC
 LIMIT 5;
