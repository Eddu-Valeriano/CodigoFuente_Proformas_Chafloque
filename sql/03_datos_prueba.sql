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
