-- SCRIPT COMPLETO: 01 creación + 02 programación (triggers y procedimientos) + 03 datos de prueba
-- Ejecutar como root:  mysql -u root -p < 00_script_completo.sql

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
-- =====================================================================
-- Proyecto  : Generación de proformas - Importaciones Chafloque E.I.R.L.
-- Script 02 : Programación de la base de datos
--             1) Procedimientos auxiliares
--             2) Triggers de validación (BEFORE)
--             3) Triggers de auditoría y de cálculo del total (AFTER)
--             4) Procedimientos CRUD (insertar, listar, modificar, eliminar)
--             5) Procedimientos con transacción para la proforma
--             6) Procedimientos de reportes (12 consultas)
-- Gestor    : MySQL 8.0
-- =====================================================================
USE chafloque_proformas;

-- =====================================================================
-- 1) PROCEDIMIENTOS AUXILIARES
-- =====================================================================

DROP PROCEDURE IF EXISTS sp_registrar_auditoria;
DELIMITER //
-- Inserta una fila en la tabla auditoria.
-- Durante la carga inicial de datos (@carga_inicial = 1) no registra nada.
CREATE PROCEDURE sp_registrar_auditoria(
  IN p_tabla       VARCHAR(30),
  IN p_accion      VARCHAR(10),
  IN p_id_registro VARCHAR(30),
  IN p_detalle     VARCHAR(500))
BEGIN
  IF COALESCE(@carga_inicial, 0) = 0 THEN
    INSERT INTO auditoria (tabla, accion, id_registro, detalle, usuario)
    VALUES (p_tabla, p_accion, p_id_registro, LEFT(p_detalle, 500), USER());
  END IF;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS sp_recalcular_total;
DELIMITER //
-- Recalcula el total de una proforma a partir de sus servicios y repuestos.
-- Lo invocan los triggers de las tablas de detalle.
CREATE PROCEDURE sp_recalcular_total(IN p_id_proforma INT UNSIGNED)
BEGIN
  UPDATE proforma p
     SET p.total =
         COALESCE((SELECT SUM(sp.cantidad_servicio * s.precio_unitario)
                     FROM servicio_proforma sp
                     JOIN servicio s ON s.id_servicio = sp.id_servicio
                    WHERE sp.id_proforma = p_id_proforma), 0)
       + COALESCE((SELECT SUM(rp.cantidad_repuesto * r.precio_unitario)
                     FROM repuesto_proforma rp
                     JOIN repuesto r ON r.id_repuesto = rp.id_repuesto
                    WHERE rp.id_proforma = p_id_proforma), 0)
   WHERE p.id_proforma = p_id_proforma;
END //
DELIMITER ;

-- =====================================================================
-- 2) TRIGGERS DE VALIDACIÓN (BEFORE INSERT / BEFORE UPDATE)
--    Limpian espacios y rechazan datos inválidos con mensajes claros.
-- =====================================================================

-- ---------- cliente ----------
DROP TRIGGER IF EXISTS trg_cliente_bi;
DROP TRIGGER IF EXISTS trg_cliente_bu;
DELIMITER //
CREATE TRIGGER trg_cliente_bi BEFORE INSERT ON cliente
FOR EACH ROW
BEGIN
  SET NEW.id_cliente     = TRIM(NEW.id_cliente);
  SET NEW.nombre_cliente = TRIM(NEW.nombre_cliente);
  SET NEW.telefono       = TRIM(NEW.telefono);
  IF NEW.id_cliente NOT REGEXP '^([0-9]{8}|[0-9]{11})$' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El DNI debe tener 8 dígitos o el RUC 11 dígitos.';
  END IF;
  IF NEW.nombre_cliente = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El nombre del cliente es obligatorio.';
  END IF;
  IF NEW.telefono NOT REGEXP '^[0-9]{9}$' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El teléfono debe tener 9 dígitos.';
  END IF;
END //
CREATE TRIGGER trg_cliente_bu BEFORE UPDATE ON cliente
FOR EACH ROW
BEGIN
  SET NEW.nombre_cliente = TRIM(NEW.nombre_cliente);
  SET NEW.telefono       = TRIM(NEW.telefono);
  IF NEW.nombre_cliente = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El nombre del cliente es obligatorio.';
  END IF;
  IF NEW.telefono NOT REGEXP '^[0-9]{9}$' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El teléfono debe tener 9 dígitos.';
  END IF;
END //
DELIMITER ;

-- ---------- mecanico ----------
DROP TRIGGER IF EXISTS trg_mecanico_bi;
DROP TRIGGER IF EXISTS trg_mecanico_bu;
DELIMITER //
CREATE TRIGGER trg_mecanico_bi BEFORE INSERT ON mecanico
FOR EACH ROW
BEGIN
  SET NEW.nombre_mecanico = TRIM(NEW.nombre_mecanico);
  IF NEW.nombre_mecanico = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El nombre del mecánico es obligatorio.';
  END IF;
END //
CREATE TRIGGER trg_mecanico_bu BEFORE UPDATE ON mecanico
FOR EACH ROW
BEGIN
  SET NEW.nombre_mecanico = TRIM(NEW.nombre_mecanico);
  IF NEW.nombre_mecanico = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El nombre del mecánico es obligatorio.';
  END IF;
END //
DELIMITER ;

-- ---------- servicio ----------
DROP TRIGGER IF EXISTS trg_servicio_bi;
DROP TRIGGER IF EXISTS trg_servicio_bu;
DELIMITER //
CREATE TRIGGER trg_servicio_bi BEFORE INSERT ON servicio
FOR EACH ROW
BEGIN
  SET NEW.nombre_servicio = TRIM(NEW.nombre_servicio);
  IF NEW.nombre_servicio = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El nombre del servicio es obligatorio.';
  END IF;
  IF NEW.precio_unitario < 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El precio del servicio no puede ser negativo.';
  END IF;
END //
CREATE TRIGGER trg_servicio_bu BEFORE UPDATE ON servicio
FOR EACH ROW
BEGIN
  SET NEW.nombre_servicio = TRIM(NEW.nombre_servicio);
  IF NEW.nombre_servicio = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El nombre del servicio es obligatorio.';
  END IF;
  IF NEW.precio_unitario < 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El precio del servicio no puede ser negativo.';
  END IF;
END //
DELIMITER ;

-- ---------- repuesto ----------
DROP TRIGGER IF EXISTS trg_repuesto_bi;
DROP TRIGGER IF EXISTS trg_repuesto_bu;
DELIMITER //
CREATE TRIGGER trg_repuesto_bi BEFORE INSERT ON repuesto
FOR EACH ROW
BEGIN
  SET NEW.nombre_repuesto = TRIM(NEW.nombre_repuesto);
  IF NEW.nombre_repuesto = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El nombre del repuesto es obligatorio.';
  END IF;
  IF NEW.precio_unitario < 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El precio del repuesto no puede ser negativo.';
  END IF;
END //
CREATE TRIGGER trg_repuesto_bu BEFORE UPDATE ON repuesto
FOR EACH ROW
BEGIN
  SET NEW.nombre_repuesto = TRIM(NEW.nombre_repuesto);
  IF NEW.nombre_repuesto = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El nombre del repuesto es obligatorio.';
  END IF;
  IF NEW.precio_unitario < 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El precio del repuesto no puede ser negativo.';
  END IF;
END //
DELIMITER ;

-- ---------- proforma ----------
-- Regla de negocio: si el cliente tiene RUC y no se indicó otro,
-- se consigna automáticamente el RUC del cliente en la proforma.
DROP TRIGGER IF EXISTS trg_proforma_bi;
DROP TRIGGER IF EXISTS trg_proforma_bu;
DELIMITER //
CREATE TRIGGER trg_proforma_bi BEFORE INSERT ON proforma
FOR EACH ROW
BEGIN
  SET NEW.marca_motor = TRIM(NEW.marca_motor);
  IF NEW.marca_motor = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La marca y modelo del motor es obligatoria.';
  END IF;
  IF NEW.fecha > CURDATE() THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La fecha de la proforma no puede ser futura.';
  END IF;
  IF NEW.ruc = '' THEN
    SET NEW.ruc = NULL;
  END IF;
  IF NEW.ruc IS NULL AND CHAR_LENGTH(NEW.id_cliente) = 11 THEN
    SET NEW.ruc = NEW.id_cliente;
  END IF;
  IF NEW.ruc IS NOT NULL AND NEW.ruc NOT REGEXP '^[0-9]{11}$' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El RUC de la proforma debe tener 11 dígitos.';
  END IF;
END //
CREATE TRIGGER trg_proforma_bu BEFORE UPDATE ON proforma
FOR EACH ROW
BEGIN
  SET NEW.marca_motor = TRIM(NEW.marca_motor);
  IF NEW.marca_motor = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La marca y modelo del motor es obligatoria.';
  END IF;
  IF NEW.fecha <> OLD.fecha AND NEW.fecha > CURDATE() THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La fecha de la proforma no puede ser futura.';
  END IF;
  IF NEW.ruc = '' THEN
    SET NEW.ruc = NULL;
  END IF;
  IF NEW.ruc IS NULL AND CHAR_LENGTH(NEW.id_cliente) = 11 THEN
    SET NEW.ruc = NEW.id_cliente;
  END IF;
  IF NEW.ruc IS NOT NULL AND NEW.ruc NOT REGEXP '^[0-9]{11}$' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El RUC de la proforma debe tener 11 dígitos.';
  END IF;
END //
DELIMITER ;

-- =====================================================================
-- 3) TRIGGERS AFTER: AUDITORÍA Y CÁLCULO AUTOMÁTICO DEL TOTAL
-- =====================================================================

-- ---------- cliente ----------
DROP TRIGGER IF EXISTS trg_cliente_ai;
DROP TRIGGER IF EXISTS trg_cliente_au;
DROP TRIGGER IF EXISTS trg_cliente_ad;
DELIMITER //
CREATE TRIGGER trg_cliente_ai AFTER INSERT ON cliente
FOR EACH ROW
  CALL sp_registrar_auditoria('cliente', 'INSERT', NEW.id_cliente,
       CONCAT('Nombre: ', NEW.nombre_cliente, ' | Teléfono: ', NEW.telefono)) //
CREATE TRIGGER trg_cliente_au AFTER UPDATE ON cliente
FOR EACH ROW
  CALL sp_registrar_auditoria('cliente', 'UPDATE', NEW.id_cliente,
       CONCAT('Antes: ', OLD.nombre_cliente, ' / ', OLD.telefono,
              ' | Después: ', NEW.nombre_cliente, ' / ', NEW.telefono)) //
CREATE TRIGGER trg_cliente_ad AFTER DELETE ON cliente
FOR EACH ROW
  CALL sp_registrar_auditoria('cliente', 'DELETE', OLD.id_cliente,
       CONCAT('Nombre: ', OLD.nombre_cliente, ' | Teléfono: ', OLD.telefono)) //
DELIMITER ;

-- ---------- mecanico ----------
DROP TRIGGER IF EXISTS trg_mecanico_ai;
DROP TRIGGER IF EXISTS trg_mecanico_au;
DROP TRIGGER IF EXISTS trg_mecanico_ad;
DELIMITER //
CREATE TRIGGER trg_mecanico_ai AFTER INSERT ON mecanico
FOR EACH ROW
  CALL sp_registrar_auditoria('mecanico', 'INSERT', NEW.id_mecanico,
       CONCAT('Nombre: ', NEW.nombre_mecanico)) //
CREATE TRIGGER trg_mecanico_au AFTER UPDATE ON mecanico
FOR EACH ROW
  CALL sp_registrar_auditoria('mecanico', 'UPDATE', NEW.id_mecanico,
       CONCAT('Antes: ', OLD.nombre_mecanico, ' | Después: ', NEW.nombre_mecanico)) //
CREATE TRIGGER trg_mecanico_ad AFTER DELETE ON mecanico
FOR EACH ROW
  CALL sp_registrar_auditoria('mecanico', 'DELETE', OLD.id_mecanico,
       CONCAT('Nombre: ', OLD.nombre_mecanico)) //
DELIMITER ;

-- ---------- servicio ----------
DROP TRIGGER IF EXISTS trg_servicio_ai;
DROP TRIGGER IF EXISTS trg_servicio_au;
DROP TRIGGER IF EXISTS trg_servicio_ad;
DELIMITER //
CREATE TRIGGER trg_servicio_ai AFTER INSERT ON servicio
FOR EACH ROW
  CALL sp_registrar_auditoria('servicio', 'INSERT', NEW.id_servicio,
       CONCAT(NEW.nombre_servicio, ' | S/ ', NEW.precio_unitario)) //
CREATE TRIGGER trg_servicio_au AFTER UPDATE ON servicio
FOR EACH ROW
  CALL sp_registrar_auditoria('servicio', 'UPDATE', NEW.id_servicio,
       CONCAT('Antes: ', OLD.nombre_servicio, ' / S/ ', OLD.precio_unitario,
              ' | Después: ', NEW.nombre_servicio, ' / S/ ', NEW.precio_unitario)) //
CREATE TRIGGER trg_servicio_ad AFTER DELETE ON servicio
FOR EACH ROW
  CALL sp_registrar_auditoria('servicio', 'DELETE', OLD.id_servicio,
       CONCAT(OLD.nombre_servicio, ' | S/ ', OLD.precio_unitario)) //
DELIMITER ;

-- ---------- repuesto ----------
DROP TRIGGER IF EXISTS trg_repuesto_ai;
DROP TRIGGER IF EXISTS trg_repuesto_au;
DROP TRIGGER IF EXISTS trg_repuesto_ad;
DELIMITER //
CREATE TRIGGER trg_repuesto_ai AFTER INSERT ON repuesto
FOR EACH ROW
  CALL sp_registrar_auditoria('repuesto', 'INSERT', NEW.id_repuesto,
       CONCAT(NEW.nombre_repuesto, ' | S/ ', NEW.precio_unitario)) //
CREATE TRIGGER trg_repuesto_au AFTER UPDATE ON repuesto
FOR EACH ROW
  CALL sp_registrar_auditoria('repuesto', 'UPDATE', NEW.id_repuesto,
       CONCAT('Antes: ', OLD.nombre_repuesto, ' / S/ ', OLD.precio_unitario,
              ' | Después: ', NEW.nombre_repuesto, ' / S/ ', NEW.precio_unitario)) //
CREATE TRIGGER trg_repuesto_ad AFTER DELETE ON repuesto
FOR EACH ROW
  CALL sp_registrar_auditoria('repuesto', 'DELETE', OLD.id_repuesto,
       CONCAT(OLD.nombre_repuesto, ' | S/ ', OLD.precio_unitario)) //
DELIMITER ;

-- ---------- proforma ----------
-- En la modificación solo se audita si cambia la cabecera; los cambios
-- del total que hacen los triggers de detalle no generan registro.
DROP TRIGGER IF EXISTS trg_proforma_ai;
DROP TRIGGER IF EXISTS trg_proforma_au;
DROP TRIGGER IF EXISTS trg_proforma_ad;
DELIMITER //
CREATE TRIGGER trg_proforma_ai AFTER INSERT ON proforma
FOR EACH ROW
  CALL sp_registrar_auditoria('proforma', 'INSERT', NEW.id_proforma,
       CONCAT('Cliente: ', NEW.id_cliente, ' | Mecánico: ', NEW.id_mecanico,
              ' | Motor: ', NEW.marca_motor, ' | Fecha: ', NEW.fecha)) //
CREATE TRIGGER trg_proforma_au AFTER UPDATE ON proforma
FOR EACH ROW
BEGIN
  IF NOT (OLD.id_cliente <=> NEW.id_cliente AND OLD.id_mecanico <=> NEW.id_mecanico
          AND OLD.marca_motor <=> NEW.marca_motor AND OLD.fecha <=> NEW.fecha
          AND OLD.ruc <=> NEW.ruc) THEN
    CALL sp_registrar_auditoria('proforma', 'UPDATE', NEW.id_proforma,
         CONCAT('Antes: ', OLD.id_cliente, ' / ', OLD.id_mecanico, ' / ', OLD.marca_motor,
                ' / ', OLD.fecha, ' | Después: ', NEW.id_cliente, ' / ', NEW.id_mecanico,
                ' / ', NEW.marca_motor, ' / ', NEW.fecha));
  END IF;
END //
CREATE TRIGGER trg_proforma_ad AFTER DELETE ON proforma
FOR EACH ROW
  CALL sp_registrar_auditoria('proforma', 'DELETE', OLD.id_proforma,
       CONCAT('Cliente: ', OLD.id_cliente, ' | Motor: ', OLD.marca_motor,
              ' | Fecha: ', OLD.fecha, ' | Total: S/ ', OLD.total)) //
DELIMITER ;

-- ---------- servicio_proforma: total + auditoría ----------
DROP TRIGGER IF EXISTS trg_sp_ai;
DROP TRIGGER IF EXISTS trg_sp_au;
DROP TRIGGER IF EXISTS trg_sp_ad;
DELIMITER //
CREATE TRIGGER trg_sp_ai AFTER INSERT ON servicio_proforma
FOR EACH ROW
BEGIN
  CALL sp_recalcular_total(NEW.id_proforma);
  CALL sp_registrar_auditoria('servicio_proforma', 'INSERT',
       CONCAT(NEW.id_proforma, '-', NEW.id_servicio),
       CONCAT('Proforma ', NEW.id_proforma, ' | Servicio ', NEW.id_servicio,
              ' | Cantidad: ', NEW.cantidad_servicio));
END //
CREATE TRIGGER trg_sp_au AFTER UPDATE ON servicio_proforma
FOR EACH ROW
BEGIN
  CALL sp_recalcular_total(NEW.id_proforma);
  IF OLD.id_proforma <> NEW.id_proforma THEN
    CALL sp_recalcular_total(OLD.id_proforma);
  END IF;
  CALL sp_registrar_auditoria('servicio_proforma', 'UPDATE',
       CONCAT(NEW.id_proforma, '-', NEW.id_servicio),
       CONCAT('Cantidad: ', OLD.cantidad_servicio, ' -> ', NEW.cantidad_servicio));
END //
CREATE TRIGGER trg_sp_ad AFTER DELETE ON servicio_proforma
FOR EACH ROW
BEGIN
  CALL sp_recalcular_total(OLD.id_proforma);
  CALL sp_registrar_auditoria('servicio_proforma', 'DELETE',
       CONCAT(OLD.id_proforma, '-', OLD.id_servicio),
       CONCAT('Proforma ', OLD.id_proforma, ' | Servicio ', OLD.id_servicio,
              ' | Cantidad: ', OLD.cantidad_servicio));
END //
DELIMITER ;

-- ---------- repuesto_proforma: total + auditoría ----------
DROP TRIGGER IF EXISTS trg_rp_ai;
DROP TRIGGER IF EXISTS trg_rp_au;
DROP TRIGGER IF EXISTS trg_rp_ad;
DELIMITER //
CREATE TRIGGER trg_rp_ai AFTER INSERT ON repuesto_proforma
FOR EACH ROW
BEGIN
  CALL sp_recalcular_total(NEW.id_proforma);
  CALL sp_registrar_auditoria('repuesto_proforma', 'INSERT',
       CONCAT(NEW.id_proforma, '-', NEW.id_repuesto),
       CONCAT('Proforma ', NEW.id_proforma, ' | Repuesto ', NEW.id_repuesto,
              ' | Cantidad: ', NEW.cantidad_repuesto));
END //
CREATE TRIGGER trg_rp_au AFTER UPDATE ON repuesto_proforma
FOR EACH ROW
BEGIN
  CALL sp_recalcular_total(NEW.id_proforma);
  IF OLD.id_proforma <> NEW.id_proforma THEN
    CALL sp_recalcular_total(OLD.id_proforma);
  END IF;
  CALL sp_registrar_auditoria('repuesto_proforma', 'UPDATE',
       CONCAT(NEW.id_proforma, '-', NEW.id_repuesto),
       CONCAT('Cantidad: ', OLD.cantidad_repuesto, ' -> ', NEW.cantidad_repuesto));
END //
CREATE TRIGGER trg_rp_ad AFTER DELETE ON repuesto_proforma
FOR EACH ROW
BEGIN
  CALL sp_recalcular_total(OLD.id_proforma);
  CALL sp_registrar_auditoria('repuesto_proforma', 'DELETE',
       CONCAT(OLD.id_proforma, '-', OLD.id_repuesto),
       CONCAT('Proforma ', OLD.id_proforma, ' | Repuesto ', OLD.id_repuesto,
              ' | Cantidad: ', OLD.cantidad_repuesto));
END //
DELIMITER ;

-- =====================================================================
-- 4) PROCEDIMIENTOS CRUD
--    Todos los cambios se hacen dentro de una transacción: si ocurre un
--    error se ejecuta ROLLBACK y el error se devuelve al aplicativo (RESIGNAL).
-- =====================================================================

-- ------------------------------ CLIENTE ------------------------------
DROP PROCEDURE IF EXISTS sp_cliente_listar;
DROP PROCEDURE IF EXISTS sp_cliente_insertar;
DROP PROCEDURE IF EXISTS sp_cliente_actualizar;
DROP PROCEDURE IF EXISTS sp_cliente_eliminar;
DELIMITER //
-- Lista los clientes; filtra por DNI/RUC o por nombre si se envía texto.
CREATE PROCEDURE sp_cliente_listar(IN p_texto VARCHAR(100))
BEGIN
  SELECT id_cliente, nombre_cliente, telefono
    FROM cliente
   WHERE p_texto IS NULL OR p_texto = ''
      OR id_cliente LIKE CONCAT(p_texto, '%')
      OR nombre_cliente LIKE CONCAT('%', p_texto, '%')
   ORDER BY nombre_cliente;
END //

CREATE PROCEDURE sp_cliente_insertar(
  IN p_id_cliente VARCHAR(11),
  IN p_nombre     VARCHAR(100),
  IN p_telefono   CHAR(9))
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
    IF EXISTS (SELECT 1 FROM cliente WHERE id_cliente = TRIM(p_id_cliente)) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ya existe un cliente con ese DNI/RUC.';
    END IF;
    INSERT INTO cliente (id_cliente, nombre_cliente, telefono)
    VALUES (p_id_cliente, p_nombre, p_telefono);
  COMMIT;
END //

CREATE PROCEDURE sp_cliente_actualizar(
  IN p_id_cliente VARCHAR(11),
  IN p_nombre     VARCHAR(100),
  IN p_telefono   CHAR(9))
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM cliente WHERE id_cliente = p_id_cliente) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El cliente no existe.';
    END IF;
    UPDATE cliente
       SET nombre_cliente = p_nombre,
           telefono       = p_telefono
     WHERE id_cliente = p_id_cliente;
  COMMIT;
END //

CREATE PROCEDURE sp_cliente_eliminar(IN p_id_cliente VARCHAR(11))
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
    IF EXISTS (SELECT 1 FROM proforma WHERE id_cliente = p_id_cliente) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No se puede eliminar: el cliente tiene proformas registradas.';
    END IF;
    DELETE FROM cliente WHERE id_cliente = p_id_cliente;
  COMMIT;
END //
DELIMITER ;

-- ------------------------------ MECÁNICO -----------------------------
DROP PROCEDURE IF EXISTS sp_mecanico_listar;
DROP PROCEDURE IF EXISTS sp_mecanico_insertar;
DROP PROCEDURE IF EXISTS sp_mecanico_actualizar;
DROP PROCEDURE IF EXISTS sp_mecanico_eliminar;
DELIMITER //
CREATE PROCEDURE sp_mecanico_listar(IN p_texto VARCHAR(100))
BEGIN
  SELECT id_mecanico, nombre_mecanico
    FROM mecanico
   WHERE p_texto IS NULL OR p_texto = ''
      OR nombre_mecanico LIKE CONCAT('%', p_texto, '%')
   ORDER BY nombre_mecanico;
END //

CREATE PROCEDURE sp_mecanico_insertar(
  IN  p_nombre      VARCHAR(100),
  OUT p_id_mecanico INT UNSIGNED)
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
    INSERT INTO mecanico (nombre_mecanico) VALUES (p_nombre);
    SET p_id_mecanico = LAST_INSERT_ID();
  COMMIT;
END //

CREATE PROCEDURE sp_mecanico_actualizar(
  IN p_id_mecanico INT UNSIGNED,
  IN p_nombre      VARCHAR(100))
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM mecanico WHERE id_mecanico = p_id_mecanico) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El mecánico no existe.';
    END IF;
    UPDATE mecanico SET nombre_mecanico = p_nombre WHERE id_mecanico = p_id_mecanico;
  COMMIT;
END //

CREATE PROCEDURE sp_mecanico_eliminar(IN p_id_mecanico INT UNSIGNED)
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
    IF EXISTS (SELECT 1 FROM proforma WHERE id_mecanico = p_id_mecanico) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No se puede eliminar: el mecánico tiene proformas registradas.';
    END IF;
    DELETE FROM mecanico WHERE id_mecanico = p_id_mecanico;
  COMMIT;
END //
DELIMITER ;

-- ------------------------------ SERVICIO -----------------------------
DROP PROCEDURE IF EXISTS sp_servicio_listar;
DROP PROCEDURE IF EXISTS sp_servicio_insertar;
DROP PROCEDURE IF EXISTS sp_servicio_actualizar;
DROP PROCEDURE IF EXISTS sp_servicio_eliminar;
DELIMITER //
CREATE PROCEDURE sp_servicio_listar(IN p_texto VARCHAR(100))
BEGIN
  SELECT id_servicio, nombre_servicio, precio_unitario
    FROM servicio
   WHERE p_texto IS NULL OR p_texto = ''
      OR nombre_servicio LIKE CONCAT('%', p_texto, '%')
   ORDER BY nombre_servicio;
END //

CREATE PROCEDURE sp_servicio_insertar(
  IN  p_nombre      VARCHAR(100),
  IN  p_precio      DECIMAL(10,2),
  OUT p_id_servicio INT UNSIGNED)
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
    IF EXISTS (SELECT 1 FROM servicio WHERE nombre_servicio = TRIM(p_nombre)) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ya existe un servicio con ese nombre.';
    END IF;
    INSERT INTO servicio (nombre_servicio, precio_unitario) VALUES (p_nombre, p_precio);
    SET p_id_servicio = LAST_INSERT_ID();
  COMMIT;
END //

CREATE PROCEDURE sp_servicio_actualizar(
  IN p_id_servicio INT UNSIGNED,
  IN p_nombre      VARCHAR(100),
  IN p_precio      DECIMAL(10,2))
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
    IF EXISTS (SELECT 1 FROM servicio
                WHERE nombre_servicio = TRIM(p_nombre) AND id_servicio <> p_id_servicio) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ya existe otro servicio con ese nombre.';
    END IF;
    UPDATE servicio
       SET nombre_servicio = p_nombre,
           precio_unitario = p_precio
     WHERE id_servicio = p_id_servicio;
  COMMIT;
END //

CREATE PROCEDURE sp_servicio_eliminar(IN p_id_servicio INT UNSIGNED)
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
    IF EXISTS (SELECT 1 FROM servicio_proforma WHERE id_servicio = p_id_servicio) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No se puede eliminar: el servicio figura en proformas registradas.';
    END IF;
    DELETE FROM servicio WHERE id_servicio = p_id_servicio;
  COMMIT;
END //
DELIMITER ;

-- ------------------------------ REPUESTO -----------------------------
DROP PROCEDURE IF EXISTS sp_repuesto_listar;
DROP PROCEDURE IF EXISTS sp_repuesto_insertar;
DROP PROCEDURE IF EXISTS sp_repuesto_actualizar;
DROP PROCEDURE IF EXISTS sp_repuesto_eliminar;
DELIMITER //
CREATE PROCEDURE sp_repuesto_listar(IN p_texto VARCHAR(100))
BEGIN
  SELECT id_repuesto, nombre_repuesto, precio_unitario
    FROM repuesto
   WHERE p_texto IS NULL OR p_texto = ''
      OR nombre_repuesto LIKE CONCAT('%', p_texto, '%')
   ORDER BY nombre_repuesto;
END //

CREATE PROCEDURE sp_repuesto_insertar(
  IN  p_nombre      VARCHAR(100),
  IN  p_precio      DECIMAL(10,2),
  OUT p_id_repuesto INT UNSIGNED)
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
    IF EXISTS (SELECT 1 FROM repuesto WHERE nombre_repuesto = TRIM(p_nombre)) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ya existe un repuesto con ese nombre.';
    END IF;
    INSERT INTO repuesto (nombre_repuesto, precio_unitario) VALUES (p_nombre, p_precio);
    SET p_id_repuesto = LAST_INSERT_ID();
  COMMIT;
END //

CREATE PROCEDURE sp_repuesto_actualizar(
  IN p_id_repuesto INT UNSIGNED,
  IN p_nombre      VARCHAR(100),
  IN p_precio      DECIMAL(10,2))
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
    IF EXISTS (SELECT 1 FROM repuesto
                WHERE nombre_repuesto = TRIM(p_nombre) AND id_repuesto <> p_id_repuesto) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ya existe otro repuesto con ese nombre.';
    END IF;
    UPDATE repuesto
       SET nombre_repuesto = p_nombre,
           precio_unitario = p_precio
     WHERE id_repuesto = p_id_repuesto;
  COMMIT;
END //

CREATE PROCEDURE sp_repuesto_eliminar(IN p_id_repuesto INT UNSIGNED)
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
    IF EXISTS (SELECT 1 FROM repuesto_proforma WHERE id_repuesto = p_id_repuesto) THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No se puede eliminar: el repuesto figura en proformas registradas.';
    END IF;
    DELETE FROM repuesto WHERE id_repuesto = p_id_repuesto;
  COMMIT;
END //
DELIMITER ;

-- =====================================================================
-- 5) PROFORMA: PROCEDIMIENTOS CON TRANSACCIÓN
--    El detalle llega como JSON desde el aplicativo, por ejemplo:
--    servicios = '[{"id":2,"cantidad":1},{"id":3,"cantidad":8}]'
--    La cabecera y todo el detalle se guardan juntos o no se guarda nada.
-- =====================================================================
DROP PROCEDURE IF EXISTS sp_proforma_listar;
DROP PROCEDURE IF EXISTS sp_proforma_obtener;
DROP PROCEDURE IF EXISTS sp_proforma_detalle;
DROP PROCEDURE IF EXISTS sp_proforma_registrar;
DROP PROCEDURE IF EXISTS sp_proforma_actualizar;
DROP PROCEDURE IF EXISTS sp_proforma_eliminar;
DELIMITER //
-- Lista las proformas; filtra por cliente, motor o número y por rango de fechas.
CREATE PROCEDURE sp_proforma_listar(
  IN p_texto VARCHAR(100),
  IN p_desde DATE,
  IN p_hasta DATE)
BEGIN
  SELECT p.id_proforma,
         LPAD(p.id_proforma, 6, '0') AS nro_proforma,
         p.fecha,
         c.nombre_cliente,
         m.nombre_mecanico,
         p.marca_motor,
         p.total
    FROM proforma p
    JOIN cliente  c ON c.id_cliente  = p.id_cliente
    JOIN mecanico m ON m.id_mecanico = p.id_mecanico
   WHERE (p_texto IS NULL OR p_texto = ''
          OR c.nombre_cliente LIKE CONCAT('%', p_texto, '%')
          OR c.id_cliente     LIKE CONCAT(p_texto, '%')
          OR p.marca_motor    LIKE CONCAT('%', p_texto, '%')
          OR CAST(p.id_proforma AS CHAR) = p_texto)
     AND (p_desde IS NULL OR p.fecha >= p_desde)
     AND (p_hasta IS NULL OR p.fecha <= p_hasta)
   ORDER BY p.fecha DESC, p.id_proforma DESC;
END //

-- Cabecera completa de una proforma (consulta 2 del informe).
CREATE PROCEDURE sp_proforma_obtener(IN p_id_proforma INT UNSIGNED)
BEGIN
  SELECT p.id_proforma,
         LPAD(p.id_proforma, 6, '0') AS nro_proforma,
         p.fecha, p.id_cliente, c.nombre_cliente, c.telefono, p.ruc,
         p.id_mecanico, m.nombre_mecanico, p.marca_motor, p.total
    FROM proforma p
    JOIN cliente  c ON c.id_cliente  = p.id_cliente
    JOIN mecanico m ON m.id_mecanico = p.id_mecanico
   WHERE p.id_proforma = p_id_proforma;
END //

-- Detalle de servicios y repuestos (consulta 3 del informe).
CREATE PROCEDURE sp_proforma_detalle(IN p_id_proforma INT UNSIGNED)
BEGIN
  SELECT 'Servicio' AS tipo, s.id_servicio AS id_item, s.nombre_servicio AS descripcion,
         sp.cantidad_servicio AS cantidad, s.precio_unitario,
         sp.cantidad_servicio * s.precio_unitario AS subtotal
    FROM servicio_proforma sp
    JOIN servicio s ON s.id_servicio = sp.id_servicio
   WHERE sp.id_proforma = p_id_proforma
  UNION ALL
  SELECT 'Repuesto', r.id_repuesto, r.nombre_repuesto,
         rp.cantidad_repuesto, r.precio_unitario,
         rp.cantidad_repuesto * r.precio_unitario
    FROM repuesto_proforma rp
    JOIN repuesto r ON r.id_repuesto = rp.id_repuesto
   WHERE rp.id_proforma = p_id_proforma
   ORDER BY tipo DESC, descripcion;
END //

-- Registra la cabecera y el detalle en una sola transacción.
CREATE PROCEDURE sp_proforma_registrar(
  IN  p_id_cliente  VARCHAR(11),
  IN  p_id_mecanico INT UNSIGNED,
  IN  p_marca_motor VARCHAR(30),
  IN  p_fecha       DATE,
  IN  p_ruc         CHAR(11),
  IN  p_servicios   JSON,
  IN  p_repuestos   JSON,
  OUT p_id_proforma INT UNSIGNED)
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  IF COALESCE(JSON_LENGTH(p_servicios), 0) + COALESCE(JSON_LENGTH(p_repuestos), 0) = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'La proforma debe tener al menos un servicio o un repuesto.';
  END IF;

  START TRANSACTION;
    INSERT INTO proforma (id_mecanico, id_cliente, marca_motor, fecha, ruc)
    VALUES (p_id_mecanico, p_id_cliente, p_marca_motor, COALESCE(p_fecha, CURDATE()), p_ruc);
    SET p_id_proforma = LAST_INSERT_ID();

    INSERT INTO servicio_proforma (id_proforma, id_servicio, cantidad_servicio)
    SELECT p_id_proforma, j.id, j.cantidad
      FROM JSON_TABLE(COALESCE(p_servicios, JSON_ARRAY()), '$[*]'
           COLUMNS (id INT UNSIGNED PATH '$.id', cantidad INT UNSIGNED PATH '$.cantidad')) j;

    INSERT INTO repuesto_proforma (id_proforma, id_repuesto, cantidad_repuesto)
    SELECT p_id_proforma, j.id, j.cantidad
      FROM JSON_TABLE(COALESCE(p_repuestos, JSON_ARRAY()), '$[*]'
           COLUMNS (id INT UNSIGNED PATH '$.id', cantidad INT UNSIGNED PATH '$.cantidad')) j;
  COMMIT;   -- el total ya fue calculado por los triggers del detalle
END //

-- Modifica la cabecera y sincroniza el detalle en una sola transacción.
CREATE PROCEDURE sp_proforma_actualizar(
  IN p_id_proforma INT UNSIGNED,
  IN p_id_cliente  VARCHAR(11),
  IN p_id_mecanico INT UNSIGNED,
  IN p_marca_motor VARCHAR(30),
  IN p_fecha       DATE,
  IN p_ruc         CHAR(11),
  IN p_servicios   JSON,
  IN p_repuestos   JSON)
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  IF COALESCE(JSON_LENGTH(p_servicios), 0) + COALESCE(JSON_LENGTH(p_repuestos), 0) = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'La proforma debe tener al menos un servicio o un repuesto.';
  END IF;

  START TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM proforma WHERE id_proforma = p_id_proforma) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La proforma no existe.';
    END IF;

    UPDATE proforma
       SET id_cliente  = p_id_cliente,
           id_mecanico = p_id_mecanico,
           marca_motor = p_marca_motor,
           fecha       = p_fecha,
           ruc         = p_ruc
     WHERE id_proforma = p_id_proforma;

    -- Servicios: se quitan los que ya no están, se actualizan las cantidades
    -- que cambiaron y se agregan los nuevos (solo cambia lo necesario).
    DELETE FROM servicio_proforma
     WHERE id_proforma = p_id_proforma
       AND id_servicio NOT IN (
           SELECT j.id FROM JSON_TABLE(COALESCE(p_servicios, JSON_ARRAY()), '$[*]'
                  COLUMNS (id INT UNSIGNED PATH '$.id')) j);

    UPDATE servicio_proforma sp
      JOIN JSON_TABLE(COALESCE(p_servicios, JSON_ARRAY()), '$[*]'
           COLUMNS (id INT UNSIGNED PATH '$.id', cantidad INT UNSIGNED PATH '$.cantidad')) j
        ON j.id = sp.id_servicio
       SET sp.cantidad_servicio = j.cantidad
     WHERE sp.id_proforma = p_id_proforma
       AND sp.cantidad_servicio <> j.cantidad;

    INSERT INTO servicio_proforma (id_proforma, id_servicio, cantidad_servicio)
    SELECT p_id_proforma, j.id, j.cantidad
      FROM JSON_TABLE(COALESCE(p_servicios, JSON_ARRAY()), '$[*]'
           COLUMNS (id INT UNSIGNED PATH '$.id', cantidad INT UNSIGNED PATH '$.cantidad')) j
     WHERE NOT EXISTS (SELECT 1 FROM servicio_proforma x
                        WHERE x.id_proforma = p_id_proforma AND x.id_servicio = j.id);

    -- Repuestos: mismo tratamiento.
    DELETE FROM repuesto_proforma
     WHERE id_proforma = p_id_proforma
       AND id_repuesto NOT IN (
           SELECT j.id FROM JSON_TABLE(COALESCE(p_repuestos, JSON_ARRAY()), '$[*]'
                  COLUMNS (id INT UNSIGNED PATH '$.id')) j);

    UPDATE repuesto_proforma rp
      JOIN JSON_TABLE(COALESCE(p_repuestos, JSON_ARRAY()), '$[*]'
           COLUMNS (id INT UNSIGNED PATH '$.id', cantidad INT UNSIGNED PATH '$.cantidad')) j
        ON j.id = rp.id_repuesto
       SET rp.cantidad_repuesto = j.cantidad
     WHERE rp.id_proforma = p_id_proforma
       AND rp.cantidad_repuesto <> j.cantidad;

    INSERT INTO repuesto_proforma (id_proforma, id_repuesto, cantidad_repuesto)
    SELECT p_id_proforma, j.id, j.cantidad
      FROM JSON_TABLE(COALESCE(p_repuestos, JSON_ARRAY()), '$[*]'
           COLUMNS (id INT UNSIGNED PATH '$.id', cantidad INT UNSIGNED PATH '$.cantidad')) j
     WHERE NOT EXISTS (SELECT 1 FROM repuesto_proforma x
                        WHERE x.id_proforma = p_id_proforma AND x.id_repuesto = j.id);
  COMMIT;
END //

-- Elimina la proforma; su detalle se borra por ON DELETE CASCADE.
CREATE PROCEDURE sp_proforma_eliminar(IN p_id_proforma INT UNSIGNED)
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;
  START TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM proforma WHERE id_proforma = p_id_proforma) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La proforma no existe.';
    END IF;
    DELETE FROM proforma WHERE id_proforma = p_id_proforma;
  COMMIT;
END //
DELIMITER ;

-- ------------------------------ AUDITORÍA ----------------------------
DROP PROCEDURE IF EXISTS sp_auditoria_listar;
DELIMITER //
-- Últimos 500 movimientos registrados por los triggers (todas las tablas o una).
CREATE PROCEDURE sp_auditoria_listar(IN p_tabla VARCHAR(30))
BEGIN
  SELECT id_auditoria, fecha_hora, tabla, accion, id_registro, detalle, usuario
    FROM auditoria
   WHERE p_tabla IS NULL OR p_tabla = '' OR tabla = p_tabla
   ORDER BY id_auditoria DESC
   LIMIT 500;
END //
DELIMITER ;

-- =====================================================================
-- 6) PROCEDIMIENTOS DE REPORTES (las 12 consultas del informe, con parámetros)
--    Usan JOIN los reportes 2, 3, 4, 5, 9, 10, 11 y 12.
-- =====================================================================
DROP PROCEDURE IF EXISTS sp_rep01_buscar_cliente;
DROP PROCEDURE IF EXISTS sp_rep02_encabezado_proforma;
DROP PROCEDURE IF EXISTS sp_rep03_detalle_proforma;
DROP PROCEDURE IF EXISTS sp_rep04_historial_cliente;
DROP PROCEDURE IF EXISTS sp_rep05_proformas_periodo;
DROP PROCEDURE IF EXISTS sp_rep06_catalogo_servicios;
DROP PROCEDURE IF EXISTS sp_rep07_buscar_repuestos;
DROP PROCEDURE IF EXISTS sp_rep08_monto_por_mes;
DROP PROCEDURE IF EXISTS sp_rep09_carga_mecanico;
DROP PROCEDURE IF EXISTS sp_rep10_servicios_top;
DROP PROCEDURE IF EXISTS sp_rep11_repuestos_top;
DROP PROCEDURE IF EXISTS sp_rep12_clientes_frecuentes;
DELIMITER //
-- R01. Búsqueda de un cliente por el inicio de su nombre.
CREATE PROCEDURE sp_rep01_buscar_cliente(IN p_nombre VARCHAR(100))
BEGIN
  SELECT id_cliente, nombre_cliente, telefono
    FROM cliente
   WHERE nombre_cliente LIKE CONCAT(p_nombre, '%')
   ORDER BY nombre_cliente;
END //

-- R02. Encabezado de una proforma (JOIN cliente y mecánico).
CREATE PROCEDURE sp_rep02_encabezado_proforma(IN p_id_proforma INT UNSIGNED)
BEGIN
  SELECT LPAD(p.id_proforma, 6, '0') AS nro_proforma, p.fecha,
         c.id_cliente, c.nombre_cliente, c.telefono, p.ruc,
         m.nombre_mecanico, p.marca_motor, p.total
    FROM proforma p
    JOIN cliente  c ON c.id_cliente  = p.id_cliente
    JOIN mecanico m ON m.id_mecanico = p.id_mecanico
   WHERE p.id_proforma = p_id_proforma;
END //

-- R03. Detalle de servicios y repuestos de una proforma (JOIN + UNION ALL).
CREATE PROCEDURE sp_rep03_detalle_proforma(IN p_id_proforma INT UNSIGNED)
BEGIN
  SELECT 'Servicio' AS tipo, s.nombre_servicio AS descripcion,
         sp.cantidad_servicio AS cantidad, s.precio_unitario,
         sp.cantidad_servicio * s.precio_unitario AS subtotal
    FROM servicio_proforma sp
    JOIN servicio s ON s.id_servicio = sp.id_servicio
   WHERE sp.id_proforma = p_id_proforma
  UNION ALL
  SELECT 'Repuesto', r.nombre_repuesto, rp.cantidad_repuesto, r.precio_unitario,
         rp.cantidad_repuesto * r.precio_unitario
    FROM repuesto_proforma rp
    JOIN repuesto r ON r.id_repuesto = rp.id_repuesto
   WHERE rp.id_proforma = p_id_proforma
   ORDER BY tipo DESC, descripcion;
END //

-- R04. Historial de proformas de un cliente (JOIN mecánico).
CREATE PROCEDURE sp_rep04_historial_cliente(IN p_id_cliente VARCHAR(11))
BEGIN
  SELECT LPAD(p.id_proforma, 6, '0') AS nro_proforma, p.fecha, p.marca_motor,
         m.nombre_mecanico, p.total
    FROM proforma p
    JOIN mecanico m ON m.id_mecanico = p.id_mecanico
   WHERE p.id_cliente = p_id_cliente
   ORDER BY p.fecha DESC;
END //

-- R05. Proformas emitidas en un periodo (JOIN cliente).
CREATE PROCEDURE sp_rep05_proformas_periodo(IN p_desde DATE, IN p_hasta DATE)
BEGIN
  SELECT LPAD(p.id_proforma, 6, '0') AS nro_proforma, p.fecha, c.nombre_cliente,
         p.marca_motor, p.total
    FROM proforma p
    JOIN cliente c ON c.id_cliente = p.id_cliente
   WHERE p.fecha BETWEEN p_desde AND p_hasta
   ORDER BY p.fecha;
END //

-- R06. Catálogo de servicios con precios.
CREATE PROCEDURE sp_rep06_catalogo_servicios()
BEGIN
  SELECT id_servicio, nombre_servicio, precio_unitario
    FROM servicio
   ORDER BY nombre_servicio;
END //

-- R07. Búsqueda de repuestos por una palabra del nombre.
CREATE PROCEDURE sp_rep07_buscar_repuestos(IN p_texto VARCHAR(100))
BEGIN
  SELECT id_repuesto, nombre_repuesto, precio_unitario
    FROM repuesto
   WHERE nombre_repuesto LIKE CONCAT('%', p_texto, '%')
   ORDER BY nombre_repuesto;
END //

-- R08. Monto total proformado por mes en un año.
CREATE PROCEDURE sp_rep08_monto_por_mes(IN p_anio INT)
BEGIN
  SELECT YEAR(fecha) AS anio, MONTH(fecha) AS mes,
         COUNT(*) AS cantidad_proformas,
         SUM(total) AS monto_total,
         ROUND(AVG(total), 2) AS monto_promedio
    FROM proforma
   WHERE fecha BETWEEN MAKEDATE(p_anio, 1) AND MAKEDATE(p_anio + 1, 1) - INTERVAL 1 DAY
   GROUP BY YEAR(fecha), MONTH(fecha)
   ORDER BY anio, mes;
END //

-- R09. Carga de trabajo por mecánico (LEFT JOIN proforma).
CREATE PROCEDURE sp_rep09_carga_mecanico()
BEGIN
  SELECT m.id_mecanico, m.nombre_mecanico,
         COUNT(p.id_proforma) AS cantidad_proformas,
         COALESCE(SUM(p.total), 0) AS monto_total
    FROM mecanico m
    LEFT JOIN proforma p ON p.id_mecanico = m.id_mecanico
   GROUP BY m.id_mecanico, m.nombre_mecanico
   ORDER BY cantidad_proformas DESC, monto_total DESC;
END //

-- R10. Servicios más solicitados (JOIN servicio).
CREATE PROCEDURE sp_rep10_servicios_top(IN p_limite INT)
BEGIN
  SELECT s.nombre_servicio,
         COUNT(*) AS veces_en_proformas,
         SUM(sp.cantidad_servicio) AS cantidad_total,
         SUM(sp.cantidad_servicio * s.precio_unitario) AS monto_estimado
    FROM servicio_proforma sp
    JOIN servicio s ON s.id_servicio = sp.id_servicio
   GROUP BY s.id_servicio, s.nombre_servicio
   ORDER BY veces_en_proformas DESC, monto_estimado DESC
   LIMIT p_limite;
END //

-- R11. Repuestos más solicitados (JOIN repuesto).
CREATE PROCEDURE sp_rep11_repuestos_top(IN p_limite INT)
BEGIN
  SELECT r.nombre_repuesto,
         COUNT(*) AS veces_en_proformas,
         SUM(rp.cantidad_repuesto) AS unidades_totales,
         SUM(rp.cantidad_repuesto * r.precio_unitario) AS monto_estimado
    FROM repuesto_proforma rp
    JOIN repuesto r ON r.id_repuesto = rp.id_repuesto
   GROUP BY r.id_repuesto, r.nombre_repuesto
   ORDER BY unidades_totales DESC
   LIMIT p_limite;
END //

-- R12. Clientes frecuentes (JOIN proforma).
CREATE PROCEDURE sp_rep12_clientes_frecuentes(IN p_minimo INT)
BEGIN
  SELECT c.id_cliente, c.nombre_cliente,
         COUNT(p.id_proforma) AS cantidad_proformas,
         SUM(p.total) AS monto_acumulado,
         MAX(p.fecha) AS ultima_proforma
    FROM cliente c
    JOIN proforma p ON p.id_cliente = c.id_cliente
   GROUP BY c.id_cliente, c.nombre_cliente
  HAVING COUNT(p.id_proforma) >= p_minimo
   ORDER BY monto_acumulado DESC;
END //
DELIMITER ;

-- =====================================================================
-- 7) RESUMEN PARA LA PANTALLA DE INICIO DEL APLICATIVO
-- =====================================================================
DROP PROCEDURE IF EXISTS sp_resumen_general;
DELIMITER //
CREATE PROCEDURE sp_resumen_general()
BEGIN
  SELECT (SELECT COUNT(*) FROM cliente)  AS clientes,
         (SELECT COUNT(*) FROM mecanico) AS mecanicos,
         (SELECT COUNT(*) FROM servicio) AS servicios,
         (SELECT COUNT(*) FROM repuesto) AS repuestos,
         (SELECT COUNT(*) FROM proforma) AS proformas,
         (SELECT COUNT(*) FROM proforma
           WHERE YEAR(fecha) = YEAR(CURDATE()) AND MONTH(fecha) = MONTH(CURDATE())) AS proformas_mes,
         (SELECT COALESCE(SUM(total), 0) FROM proforma
           WHERE YEAR(fecha) = YEAR(CURDATE()) AND MONTH(fecha) = MONTH(CURDATE())) AS monto_mes,
         (SELECT COALESCE(SUM(total), 0) FROM proforma) AS monto_total;
END //
DELIMITER ;
-- =====================================================================
-- Proyecto  : Generación de proformas - Importaciones Chafloque E.I.R.L.
-- Script 03 : Datos de prueba (12 clientes, 5 mecánicos, 12 servicios,
--             12 repuestos y 20 proformas con su detalle)
-- Nota      : el total de cada proforma lo calculan los triggers del
--             detalle; durante esta carga no se registra auditoría.
-- =====================================================================
USE chafloque_proformas;

SET @carga_inicial = 1;     -- desactiva la auditoría solo para esta carga

INSERT INTO cliente (id_cliente, nombre_cliente, telefono) VALUES
  ('20456789123', 'Transportes Misti S.A.C.', '054256789'),
  ('20601234567', 'Agroindustrias Majes E.I.R.L.', '959312480'),
  ('20539871246', 'Constructora Chachani S.R.L.', '974551236'),
  ('10432187654', 'Rosa Elena Quispe Mamani', '958741236'),
  ('43218765', 'Sergio Torres Valdivia', '983296095'),
  ('45671234', 'Kevin Rojas Huamaní', '972217597'),
  ('40125896', 'Luis Alberto Cáceres Zúñiga', '951478236'),
  ('47896321', 'Helen Paredes Flores', '987654321'),
  ('20612398745', 'Gloria Minera del Sur S.A.C.', '054431122'),
  ('42587413', 'Sebastián Ccama Apaza', '962145874'),
  ('20498765431', 'Inversiones Yura S.A.', '054278899'),
  ('46325874', 'Marco Antonio Chávez Ríos', '945632178');

INSERT INTO mecanico (id_mecanico, nombre_mecanico) VALUES
  (1, 'Vicente Palacios Medina'),
  (2, 'Mauricio Lazo Herrera'),
  (3, 'Juan Pérez Condori'),
  (4, 'Orlando Mechan Ruiz'),
  (5, 'Mauricio Medina Salas');

INSERT INTO servicio (id_servicio, nombre_servicio, precio_unitario) VALUES
  (1, 'Rectificar camisetas', 60.00),
  (2, 'Cepillar culata', 120.00),
  (3, 'Rectificar válvulas', 15.00),
  (4, 'Cambiar válvulas', 12.00),
  (5, 'Lavar motor', 80.00),
  (6, 'Rectificar bielas', 45.00),
  (7, 'Cambiar asientos de válvula con material', 35.00),
  (8, 'Pulir cigüeñal', 150.00),
  (9, 'Rectificar cilindros', 70.00),
  (10, 'Cepillar monoblock', 180.00),
  (11, 'Soldadura de culata', 250.00),
  (12, 'Tornería general', 90.00);

INSERT INTO repuesto (id_repuesto, nombre_repuesto, precio_unitario) VALUES
  (1, 'Juego (kit) de pistones', 480.00),
  (2, 'Metal de eje (juego)', 210.00),
  (3, 'Válvula de admisión', 35.00),
  (4, 'Válvula de escape', 40.00),
  (5, 'Juego de anillos', 180.00),
  (6, 'Camiseta de cilindro', 95.00),
  (7, 'Axiales (juego)', 45.00),
  (8, 'Metal de biela (juego)', 160.00),
  (9, 'Empaquetadura de culata', 120.00),
  (10, 'Retén de válvula', 6.50),
  (11, 'Guía de válvula', 15.00),
  (12, 'Bomba de aceite', 340.00);

INSERT INTO proforma (id_proforma, id_mecanico, id_cliente, marca_motor, fecha, ruc) VALUES
  (1, 1, '20456789123', 'Mercedes-Benz OM457', '2026-01-08', '20456789123'),
  (2, 2, '43218765', 'Toyota 3E', '2026-01-15', NULL),
  (3, 1, '20601234567', 'Mitsubishi 4D56', '2026-02-03', '20601234567'),
  (4, 3, '45671234', 'Toyota 2JZ-GTE', '2026-02-20', NULL),
  (5, 4, '10432187654', 'Nissan QD32', '2026-03-05', '10432187654'),
  (6, 2, '20539871246', 'Isuzu 4JB1', '2026-03-18', '20539871246'),
  (7, 1, '40125896', 'Toyota 5L', '2026-04-02', NULL),
  (8, 3, '20456789123', 'Mercedes-Benz OM457', '2026-04-22', '20456789123'),
  (9, 4, '47896321', 'Toyota 3ZZ-GCE', '2026-05-07', NULL),
  (10, 2, '20612398745', 'Hyundai D4BH', '2026-05-26', '20612398745'),
  (11, 1, '42587413', 'Toyota 1KZ-TE', '2026-06-10', NULL),
  (12, 3, '20601234567', 'Mitsubishi 4D56', '2026-06-29', '20601234567'),
  (13, 4, '43218765', 'Toyota 3E', '2026-07-14', NULL),
  (14, 1, '20498765431', 'Isuzu 4HF1', '2026-08-04', '20498765431'),
  (15, 2, '20456789123', 'Volvo D13', '2026-08-25', '20456789123'),
  (16, 3, '45671234', 'Toyota 2JZ-GTE', '2026-09-09', NULL),
  (17, 4, '20539871246', 'Isuzu 4JB1', '2026-09-23', '20539871246'),
  (18, 1, '10432187654', 'Nissan QD32', '2026-10-06', '10432187654'),
  (19, 2, '20612398745', 'Hyundai D4BH', '2026-10-15', '20612398745'),
  (20, 3, '40125896', 'Toyota 5L', '2026-10-21', NULL);

INSERT INTO servicio_proforma (id_proforma, id_servicio, cantidad_servicio) VALUES
  (1, 1, 6), (1, 2, 1), (1, 3, 12), (1, 5, 1),
  (2, 2, 1), (2, 3, 8), (2, 4, 8),
  (3, 1, 4), (3, 6, 4), (3, 8, 1),
  (4, 2, 1), (4, 3, 24), (4, 7, 6),
  (5, 9, 4), (5, 6, 4), (5, 5, 1),
  (6, 1, 4), (6, 2, 1), (6, 8, 1),
  (7, 2, 1), (7, 3, 8),
  (8, 10, 1), (8, 11, 1), (8, 5, 1),
  (9, 3, 16), (9, 4, 4), (9, 2, 1),
  (10, 1, 4), (10, 8, 1), (10, 12, 2),
  (11, 2, 1), (11, 9, 4), (11, 6, 4),
  (12, 3, 8), (12, 4, 8),
  (13, 2, 1), (13, 5, 1),
  (14, 1, 4), (14, 2, 1), (14, 3, 8), (14, 5, 1),
  (15, 10, 1), (15, 8, 1), (15, 6, 6),
  (16, 2, 1), (16, 7, 12),
  (17, 9, 4), (17, 6, 4), (17, 5, 1),
  (18, 2, 1), (18, 3, 8),
  (19, 1, 4), (19, 11, 1),
  (20, 2, 1), (20, 3, 8), (20, 4, 8);

INSERT INTO repuesto_proforma (id_proforma, id_repuesto, cantidad_repuesto) VALUES
  (1, 6, 6), (1, 5, 1), (1, 9, 1),
  (2, 3, 4), (2, 4, 4), (2, 10, 8),
  (3, 1, 1), (3, 2, 1), (3, 8, 1),
  (4, 3, 12), (4, 4, 12), (4, 10, 24),
  (5, 1, 1), (5, 5, 1), (5, 7, 1),
  (6, 6, 4), (6, 2, 1),
  (7, 9, 1), (7, 10, 8),
  (8, 9, 1), (8, 12, 1),
  (9, 3, 8), (9, 4, 8), (9, 11, 4),
  (10, 6, 4), (10, 5, 1), (10, 8, 1),
  (11, 1, 1), (11, 5, 1),
  (12, 3, 4), (12, 4, 4), (12, 10, 8),
  (14, 6, 4), (14, 9, 1),
  (15, 2, 1), (15, 8, 1), (15, 7, 1),
  (16, 3, 12), (16, 4, 12),
  (17, 1, 1), (17, 5, 1),
  (18, 9, 1), (18, 10, 8),
  (19, 6, 4), (19, 9, 1),
  (20, 3, 4), (20, 4, 4), (20, 9, 1);

SET @carga_inicial = NULL;  -- desde aquí todo cambio queda auditado

-- Verificación: 20 proformas con un total general de S/ 20 430.00
SELECT COUNT(*) AS proformas, SUM(total) AS total_general FROM proforma;
