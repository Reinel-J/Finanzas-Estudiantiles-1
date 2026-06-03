-- ============================================================
--  Finanzas Estudiantiles - Script de Base de Datos
--  Ejecutar en MySQL antes de iniciar la aplicación
-- ============================================================

CREATE DATABASE IF NOT EXISTS finanzas_estudiantiles
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE finanzas_estudiantiles;

-- Tabla usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre                VARCHAR(100) NOT NULL UNIQUE,
    apellido              VARCHAR(100) NOT NULL,
    contrasena            VARCHAR(255) NOT NULL,
    tipo_doc              VARCHAR(50)  NOT NULL,
    n_doc                 VARCHAR(50)  NOT NULL UNIQUE,
    universidad           VARCHAR(150) NOT NULL,
    semestre_actual       INT          DEFAULT 1,
    fecha_inicio_semestre DATE,
    fecha_fin_semestre    DATE,
    porcentaje_ahorro     DOUBLE       DEFAULT 10.0,
    monto_ahorrado        DOUBLE       DEFAULT 0.0
);

-- Tabla ingresos
CREATE TABLE IF NOT EXISTS ingresos (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL,
    monto       DOUBLE       NOT NULL,
    descripcion VARCHAR(255),
    fecha       DATE         NOT NULL,
    categoria   VARCHAR(100),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- Tabla gastos
CREATE TABLE IF NOT EXISTS gastos (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL,
    ingreso_id  BIGINT,
    monto       DOUBLE       NOT NULL,
    descripcion VARCHAR(255),
    fecha       DATE         NOT NULL,
    categoria   VARCHAR(100),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (ingreso_id) REFERENCES ingresos(id) ON DELETE SET NULL
);

-- Tabla metas_ahorro
CREATE TABLE IF NOT EXISTS metas_ahorro (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT       NOT NULL,
    nombre         VARCHAR(150) NOT NULL,
    monto_objetivo DOUBLE       NOT NULL,
    monto_actual   DOUBLE       DEFAULT 0.0,
    fecha_limite   DATE,
    estado         VARCHAR(50)  DEFAULT 'EN_PROGRESO',
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- Tabla reportes
CREATE TABLE IF NOT EXISTS reportes (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id       BIGINT       NOT NULL,
    tipo             VARCHAR(50)  NOT NULL,
    fecha_inicio     DATE         NOT NULL,
    fecha_fin        DATE         NOT NULL,
    total_gastos     DOUBLE       NOT NULL,
    total_ingresos   DOUBLE,
    detalle_diario   TEXT,
    fecha_generacion DATETIME     NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- Tabla historial_semestres
CREATE TABLE IF NOT EXISTS historial_semestres (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id              BIGINT       NOT NULL,
    semestre_numero         INT          NOT NULL,
    fecha_inicio            DATE         NOT NULL,
    fecha_fin               DATE         NOT NULL,
    fecha_finalizacion      DATETIME,
    total_ingresos          DOUBLE       DEFAULT 0.0,
    total_gastos            DOUBLE       DEFAULT 0.0,
    saldo_final             DOUBLE       DEFAULT 0.0,
    monto_ahorrado_final    DOUBLE       DEFAULT 0.0,
    cantidad_ingresos       INT          DEFAULT 0,
    cantidad_gastos         INT          DEFAULT 0,
    gastos_por_categoria    TEXT,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);
