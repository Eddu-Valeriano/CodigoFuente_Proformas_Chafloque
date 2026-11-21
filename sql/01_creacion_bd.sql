-- =====================================================================
-- Proyecto  : Generación de proformas - Importaciones Chafloque E.I.R.L.
-- Script 01 : Creación de la base de datos, tablas, índices,
--             tabla de auditoría y usuario del aplicativo
-- Gestor    : MySQL 8.0  |  Motor: InnoDB
-- Ejecutar  : como root (o un usuario administrador), en este orden:
--             01_creacion_bd.sql -> 02_programacion_bd.sql -> 03_datos_prueba.sql
-- =====================================================================

DROP DATABASE IF EXISTS chafloque_proformas;
CREATE DATABASE chafloque_proformas
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE chafloque_proformas;

-- ---------------------------------------------------------------------
-- Tablas padre (catálogos y actores)
-- ---------------------------------------------------------------------
CREATE TABLE cliente (
  id_cliente      VARCHAR(11)  NOT NULL,          -- DNI (8) o RUC (11)
  nombre_cliente  VARCHAR(100) NOT NULL,
  telefono        CHAR(9)      NOT NULL,
  CONSTRAINT pk_cliente PRIMARY KEY (id_cliente),
  CONSTRAINT ck_cliente_id  CHECK (id_cliente REGEXP '^([0-9]{8}|[0-9]{11})$'),
  CONSTRAINT ck_cliente_tel CHECK (telefono REGEXP '^[0-9]{9}$'),
  KEY idx_cliente_nombre (nombre_cliente)
) ENGINE = InnoDB;

CREATE TABLE mecanico (
  id_mecanico      INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre_mecanico  VARCHAR(100) NOT NULL,
  CONSTRAINT pk_mecanico PRIMARY KEY (id_mecanico),
  KEY idx_mecanico_nombre (nombre_mecanico)
) ENGINE = InnoDB;

CREATE TABLE servicio (
  id_servicio      INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  nombre_servicio  VARCHAR(100)  NOT NULL,
  precio_unitario  DECIMAL(10,2) NOT NULL,
  CONSTRAINT pk_servicio PRIMARY KEY (id_servicio),
  CONSTRAINT uq_servicio_nombre UNIQUE (nombre_servicio),
  CONSTRAINT ck_servicio_precio CHECK (precio_unitario >= 0)
) ENGINE = InnoDB;

CREATE TABLE repuesto (
  id_repuesto      INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  nombre_repuesto  VARCHAR(100)  NOT NULL,
  precio_unitario  DECIMAL(10,2) NOT NULL,
  CONSTRAINT pk_repuesto PRIMARY KEY (id_repuesto),
  CONSTRAINT uq_repuesto_nombre UNIQUE (nombre_repuesto),
  CONSTRAINT ck_repuesto_precio CHECK (precio_unitario >= 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Proforma (cabecera)
-- ---------------------------------------------------------------------
CREATE TABLE proforma (
  id_proforma  INT UNSIGNED  NOT NULL AUTO_INCREMENT,
  id_mecanico  INT UNSIGNED  NOT NULL,
  id_cliente   VARCHAR(11)   NOT NULL,
  marca_motor  VARCHAR(30)   NOT NULL,
  fecha        DATE          NOT NULL,
  total        DECIMAL(10,2) NOT NULL DEFAULT 0.00,  -- lo mantienen los triggers
  ruc          CHAR(11)      NULL,
  CONSTRAINT pk_proforma PRIMARY KEY (id_proforma),
  CONSTRAINT ck_proforma_total CHECK (total >= 0),
  CONSTRAINT ck_proforma_ruc   CHECK (ruc IS NULL OR ruc REGEXP '^[0-9]{11}$'),
  KEY idx_proforma_cliente  (id_cliente),
  KEY idx_proforma_mecanico (id_mecanico),
  KEY idx_proforma_fecha    (fecha),
  CONSTRAINT fk_proforma_cliente FOREIGN KEY (id_cliente)
    REFERENCES cliente (id_cliente) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_proforma_mecanico FOREIGN KEY (id_mecanico)
    REFERENCES mecanico (id_mecanico) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Tablas de detalle
-- ---------------------------------------------------------------------
CREATE TABLE servicio_proforma (
  id_proforma        INT UNSIGNED NOT NULL,
  id_servicio        INT UNSIGNED NOT NULL,
  cantidad_servicio  INT UNSIGNED NOT NULL,
  CONSTRAINT pk_servicio_proforma PRIMARY KEY (id_proforma, id_servicio),
  CONSTRAINT ck_sp_cantidad CHECK (cantidad_servicio > 0),
  KEY idx_sp_servicio (id_servicio),
  CONSTRAINT fk_sp_proforma FOREIGN KEY (id_proforma)
    REFERENCES proforma (id_proforma) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_sp_servicio FOREIGN KEY (id_servicio)
    REFERENCES servicio (id_servicio) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE repuesto_proforma (
  id_proforma        INT UNSIGNED NOT NULL,
  id_repuesto        INT UNSIGNED NOT NULL,
  cantidad_repuesto  INT UNSIGNED NOT NULL,
  CONSTRAINT pk_repuesto_proforma PRIMARY KEY (id_proforma, id_repuesto),
  CONSTRAINT ck_rp_cantidad CHECK (cantidad_repuesto > 0),
  KEY idx_rp_repuesto (id_repuesto),
  CONSTRAINT fk_rp_proforma FOREIGN KEY (id_proforma)
    REFERENCES proforma (id_proforma) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_rp_repuesto FOREIGN KEY (id_repuesto)
    REFERENCES repuesto (id_repuesto) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Tabla de auditoría (la llenan los triggers AFTER de cada tabla)
-- ---------------------------------------------------------------------
CREATE TABLE auditoria (
  id_auditoria  INT UNSIGNED NOT NULL AUTO_INCREMENT,
  tabla         VARCHAR(30)  NOT NULL,
  accion        ENUM('INSERT','UPDATE','DELETE') NOT NULL,
  id_registro   VARCHAR(30)  NOT NULL,
  detalle       VARCHAR(500) NULL,
  usuario       VARCHAR(100) NOT NULL,
  fecha_hora    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_auditoria PRIMARY KEY (id_auditoria),
  KEY idx_auditoria_tabla (tabla, fecha_hora)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Usuario del aplicativo: solo puede EJECUTAR procedimientos.
-- No tiene permisos directos de SELECT, INSERT, UPDATE ni DELETE.
-- ---------------------------------------------------------------------
CREATE USER IF NOT EXISTS 'chafloque_app'@'localhost' IDENTIFIED BY 'Chafloque.2026';
GRANT EXECUTE ON chafloque_proformas.* TO 'chafloque_app'@'localhost';
FLUSH PRIVILEGES;
