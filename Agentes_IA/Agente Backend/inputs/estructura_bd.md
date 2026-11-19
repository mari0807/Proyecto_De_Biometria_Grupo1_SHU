# Especificación de Arquitectura de Base de Datos (SDD Context)

> **Propósito del documento:** Este archivo define la estructura, relaciones, reglas de negocio y restricciones de la base de datos del sistema de monitorización ambiental y biometría. Está diseñado como fuente única de verdad (*Single Source of Truth*) para el desarrollo guiado por especificaciones (**Spec-Driven Development - SDD**). Toda generación de modelos, migraciones, consultas, DTOs y endpoints debe adherirse estrictamente a este esquema.

---

## 1. Visión General del Dominio

El sistema gestiona dos flujos principales de datos medioambientales:
1. **Red de Sensores de Usuarios (IoT / Crowdsourcing):** Usuarios registrados que poseen nodos sensores móviles o fijos (`NODO_SENSOR`) que reportan mediciones geolocalizadas (`MEDICION`) de distintos contaminantes (`CONTAMINANTE`).
2. **Red de Estaciones Oficiales (Referencia):** Estaciones fijas institucionales (`ESTACION_OFICIAL`) que registran mediciones oficiales (`MEDICION_OFICIAL`) de los mismos contaminantes para contraste, calibración o visualización comparativa.

---

## 2. Esquema de Entidades y Diccionario de Datos

### 2.1. `ROLES`
Define los perfiles de permisos y acceso dentro de la plataforma.

| Columna | Tipo de Dato | Restricción | Descripción |
| :--- | :--- | :--- | :--- |
| `id_rol` | `INT` | **PK**, Auto-increment | Identificador único del rol. |
| `nombre` | `STRING` (VARCHAR) | NOT NULL, UNIQUE | Nombre técnico o visible del rol (ej. `admin`, `usuario`). |
| `descripcion` | `STRING` (TEXT/VARCHAR) | NULLABLE | Descripción detallada de los permisos asociados al rol. |

---

### 2.2. `USUARIO`
Almacena la información de las cuentas de usuario y sus credenciales de autenticación.

| Columna | Tipo de Dato | Restricción | Descripción |
| :--- | :--- | :--- | :--- |
| `id_usuario` | `INT` | **PK**, Auto-increment | Identificador único del usuario. |
| `nombre` | `STRING` (VARCHAR) | NOT NULL | Nombre(s) del usuario. |
| `apellidos` | `STRING` (VARCHAR) | NOT NULL | Apellidos del usuario. |
| `email` | `STRING` (VARCHAR) | NOT NULL, UNIQUE | Correo electrónico utilizado para login y notificaciones. |
| `password_hash` | `STRING` (VARCHAR) | NOT NULL | Contraseña cifrada (nunca almacenar en texto plano). |
| `id_rol` | `INT` *(Nota: figura como `string` en diagrama, corregir a `INT` en implementación SQL)* | **FK** -> `ROLES.id_rol`, NOT NULL | Rol asignado al usuario. |
| `foto_perfil` | `LONGBLOB` | NULLABLE | Imagen binaria del avatar del usuario. |

---

### 2.3. `NODO_SENSOR`
Representa los dispositivos físicos (hardware/IoT) vinculados a un usuario que capturan datos ambientales.

| Columna | Tipo de Dato | Restricción | Descripción |
| :--- | :--- | :--- | :--- |
| `id_nodo` | `INT` | **PK**, Auto-increment | Identificador interno del nodo sensor. |
| `identificador` | `STRING` (VARCHAR) | NOT NULL, UNIQUE | Código de serie, MAC o UUID físico del dispositivo. |
| `estado` | `STRING` (VARCHAR) | NOT NULL | Estado operativo del nodo (ej. `activo`, `inactivo`, `mantenimiento`, `desconectado`). |
| `ultima_comunicacion` | `DATETIME` | NULLABLE | Marca de tiempo de la última vez que el nodo envió datos o un *heartbeat*. |
| `id_usuario` | `INT` | **FK** -> `USUARIO.id_usuario`, NOT NULL | Usuario propietario o responsable del nodo sensor. |

---

### 2.4. `CONTAMINANTE`
Catálogo maestro de gases, partículas o parámetros ambientales medibles por tanto nodos como estaciones.

| Columna | Tipo de Dato | Restricción | Descripción |
| :--- | :--- | :--- | :--- |
| `id_contaminante` | `INT` | **PK**, Auto-increment | Identificador único del contaminante. |
| `nombre` | `STRING` (VARCHAR) | NOT NULL, UNIQUE | Nombre o fórmula del parámetro (ej. `CO2`, `PM2.5`, `NO2`, `O3`). |
| `unidad_medida` | `STRING` (VARCHAR) | NOT NULL | Unidad estándar de medición (ej. `ppm`, `µg/m³`, `ppb`). |

---

### 2.5. `MEDICION`
Registros de lecturas capturadas por los nodos sensores de los usuarios. Incluye geolocalización por lectura (soporta nodos móviles).

| Columna | Tipo de Dato | Restricción | Descripción |
| :--- | :--- | :--- | :--- |
| `id_medicion` | `INT` | **PK**, Auto-increment | Identificador único de la lectura. |
| `id_contaminante` | `INT` | **FK** -> `CONTAMINANTE.id_contaminante`, NOT NULL | Contaminante medido. |
| `id_nodo` | `INT` | **FK** -> `NODO_SENSOR.id_nodo`, NOT NULL | Nodo sensor que originó la lectura. |
| `valor` | `FLOAT` | NOT NULL | Valor numérico de la concentración o parámetro medido. |
| `fecha_hora` | `DATETIME` | NOT NULL | Fecha y hora exacta (preferiblemente UTC) en que se tomó la muestra. |
| `latitud` | `FLOAT` | NOT NULL | Coordenada de latitud GPS en el momento de la medición. |
| `longitud` | `FLOAT` | NOT NULL | Coordenada de longitud GPS en el momento de la medición. |

---

### 2.6. `ESTACION_OFICIAL`
Catálogo de estaciones meteorológicas o de calidad del aire gubernamentales/oficiales con ubicación fija.

| Columna | Tipo de Dato | Restricción | Descripción |
| :--- | :--- | :--- | :--- |
| `id_estacion` | `INT` | **PK**, Auto-increment | Identificador único de la estación oficial. |
| `nombre` | `STRING` (VARCHAR) | NOT NULL | Nombre oficial o ubicación descriptiva de la estación. |
| `latitud` | `FLOAT` | NOT NULL | Coordenada fija de latitud de la estación. |
| `longitud` | `FLOAT` | NOT NULL | Coordenada fija de longitud de la estación. |

---

### 2.7. `MEDICION_OFICIAL`
Registros históricos o en tiempo real obtenidos de las estaciones oficiales. No requiere coordenadas por fila ya que hereda la ubicación fija de `ESTACION_OFICIAL`.

| Columna | Tipo de Dato | Restricción | Descripción |
| :--- | :--- | :--- | :--- |
| `id_medicion_oficial` | `INT` | **PK**, Auto-increment | Identificador único de la medición oficial. |
| `id_estacion` | `INT` | **FK** -> `ESTACION_OFICIAL.id_estacion`, NOT NULL | Estación oficial que reporta el dato. |
| `id_contaminante` | `INT` | **FK** -> `CONTAMINANTE.id_contaminante`, NOT NULL | Contaminante medido. |
| `valor` | `FLOAT` | NOT NULL | Valor numérico reportado por la estación. |
| `fecha_hora` | `DATETIME` | NOT NULL | Fecha y hora de la medición oficial *(Nota: aparece tipado como `datetim` en el diagrama original)*. |

---

## 3. Diagrama de Relaciones y Cardinalidad (ER)

```text
ROLES (1) ────────< (N) USUARIO
USUARIO (1) ──────< (N) NODO_SENSOR
NODO_SENSOR (1) ──< (N) MEDICION
CONTAMINANTE (1) ─< (N) MEDICION

ESTACION_OFICIAL (1) ──< (N) MEDICION_OFICIAL
CONTAMINANTE (1) ──────< (N) MEDICION_OFICIAL
```

### Resumen de Cardinalidades:
* **Un Rol** puede estar asignado a **muchos Usuarios** (`1:N`).
* **Un Usuario** puede tener vinculados **muchos Nodos Sensores** (`1:N`).
* **Un Nodo Sensor** genera **muchas Mediciones** (`1:N`).
* **Una Estación Oficial** genera **muchas Mediciones Oficiales** (`1:N`).
* **Un Contaminante** está presente tanto en **muchas Mediciones** (de nodos) como en **muchas Mediciones Oficiales** (`1:N`).

---

## 4. Notas de Implementación y Reglas para la IA (SDD Guidelines)

Al generar código basado en esta especificación, la IA debe aplicar obligatoriamente las siguientes correcciones y buenas prácticas:

1. **Consistencia de Tipos en Claves Foráneas (`USUARIO.id_rol`):**
   * En el diagrama visual original, `USUARIO.id_rol` figura como `string`, mientras que `ROLES.id_rol` es `int`. Al generar el DDL SQL o modelos ORM (Prisma, TypeORM, SQLAlchemy, Sequelize, etc.), **debe implementarse como `INT`** para mantener la integridad referencial.
2. **Corrección de Typo en `MEDICION_OFICIAL.fecha_hora`:**
   * En el diagrama aparece como `datetim`. Debe implementarse como tipo estándar `DATETIME` o `TIMESTAMP` (con soporte UTC).
3. **Seguridad y Serialización de `USUARIO`:**
   * `password_hash` **nunca** debe incluirse en respuestas JSON de API (usar exclusión en DTOs/Serializers).
   * `foto_perfil` (`LONGBLOB`) no debe cargarse por defecto en listados de usuarios para evitar cuellos de botella de memoria/red; debe servirse mediante un endpoint dedicado o carga diferida (*lazy loading*).
4. **Actualización de Estado del Nodo:**
   * Cada vez que se inserte un registro válido en `MEDICION` asociado a un `id_nodo`, la lógica de negocio debe actualizar automáticamente `NODO_SENSOR.ultima_comunicacion` con el timestamp actual.
5. **Indexación Recomendada:**
   * Crear índices compuestos en `(id_contaminante, fecha_hora)` tanto en `MEDICION` como en `MEDICION_OFICIAL` para optimizar consultas de series temporales y gráficas.
   * Crear índices espaciales o compuestos en `(latitud, longitud)` en `MEDICION` y `ESTACION_OFICIAL` para búsquedas por proximidad geográfica.