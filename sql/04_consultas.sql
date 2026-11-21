-- =====================================================================
-- Proyecto  : Generación de proformas - Importaciones Chafloque E.I.R.L.
-- Script 04 : Las 12 consultas frecuentes del informe (versión SQL directa).
--             En el aplicativo se ejecutan como procedimientos sp_rep01 ... sp_rep12.
-- =====================================================================
USE chafloque_proformas;

-- Consulta 1: Búsqueda de un cliente por nombre
SELECT id_cliente, nombre_cliente, telefono
FROM cliente
WHERE nombre_cliente LIKE 'Transportes%'
ORDER BY nombre_cliente;

-- Consulta 2: Encabezado de una proforma
SELECT p.id_proforma,
LPAD(p.id_proforma, 6, '0') AS nro_proforma,
p.fecha,
c.id_cliente,
c.nombre_cliente,
c.telefono,
p.ruc,
m.nombre_mecanico,
p.marca_motor,
p.total
FROM proforma p
JOIN cliente c ON c.id_cliente = p.id_cliente
JOIN mecanico m ON m.id_mecanico = p.id_mecanico
WHERE p.id_proforma = 1;

-- Consulta 3: Detalle de servicios y repuestos de una proforma
SELECT 'Servicio' AS tipo,
s.nombre_servicio AS descripcion,
sp.cantidad_servicio AS cantidad,
s.precio_unitario,
sp.cantidad_servicio * s.precio_unitario AS subtotal
FROM servicio_proforma sp
JOIN servicio s ON s.id_servicio = sp.id_servicio
WHERE sp.id_proforma = 1
UNION ALL
SELECT 'Repuesto',
r.nombre_repuesto,
rp.cantidad_repuesto,
r.precio_unitario,
rp.cantidad_repuesto * r.precio_unitario
FROM repuesto_proforma rp
JOIN repuesto r ON r.id_repuesto = rp.id_repuesto
WHERE rp.id_proforma = 1
ORDER BY tipo DESC, descripcion;

-- Consulta 4: Historial de proformas de un cliente
SELECT p.id_proforma, p.fecha, p.marca_motor,
m.nombre_mecanico, p.total
FROM proforma p
JOIN mecanico m ON m.id_mecanico = p.id_mecanico
WHERE p.id_cliente = '20456789123'
ORDER BY p.fecha DESC;

-- Consulta 5: Proformas emitidas en un periodo
SELECT p.id_proforma, p.fecha, c.nombre_cliente,
p.marca_motor, p.total
FROM proforma p
JOIN cliente c ON c.id_cliente = p.id_cliente
WHERE p.fecha BETWEEN '2026-10-01' AND '2026-10-31'
ORDER BY p.fecha;

-- Consulta 6: Catálogo de servicios con precios
SELECT id_servicio, nombre_servicio, precio_unitario
FROM servicio
ORDER BY nombre_servicio;

-- Consulta 7: Búsqueda de repuestos por nombre
SELECT id_repuesto, nombre_repuesto, precio_unitario
FROM repuesto
WHERE nombre_repuesto LIKE '%válvula%'
ORDER BY nombre_repuesto;

-- Consulta 8: Monto total proformado por mes
SELECT YEAR(fecha) AS anio,
MONTH(fecha) AS mes,
COUNT(*) AS cantidad_proformas,
SUM(total) AS monto_total,
ROUND(AVG(total), 2) AS monto_promedio
FROM proforma
WHERE fecha BETWEEN '2026-01-01' AND '2026-12-31'
GROUP BY YEAR(fecha), MONTH(fecha)
ORDER BY anio, mes;

-- Consulta 9: Carga de trabajo por mecánico
SELECT m.id_mecanico, m.nombre_mecanico,
COUNT(p.id_proforma) AS cantidad_proformas,
COALESCE(SUM(p.total), 0) AS monto_total
FROM mecanico m
LEFT JOIN proforma p ON p.id_mecanico = m.id_mecanico
GROUP BY m.id_mecanico, m.nombre_mecanico
ORDER BY cantidad_proformas DESC, monto_total DESC;

-- Consulta 10: Servicios más solicitados
SELECT s.nombre_servicio,
COUNT(*) AS veces_en_proformas,
SUM(sp.cantidad_servicio) AS cantidad_total,
SUM(sp.cantidad_servicio * s.precio_unitario) AS monto_estimado
FROM servicio_proforma sp
JOIN servicio s ON s.id_servicio = sp.id_servicio
GROUP BY s.id_servicio, s.nombre_servicio
ORDER BY veces_en_proformas DESC, monto_estimado DESC
LIMIT 5;

-- Consulta 11: Repuestos más solicitados
SELECT r.nombre_repuesto,
COUNT(*) AS veces_en_proformas,
SUM(rp.cantidad_repuesto) AS unidades_totales,
SUM(rp.cantidad_repuesto * r.precio_unitario) AS monto_estimado
FROM repuesto_proforma rp
JOIN repuesto r ON r.id_repuesto = rp.id_repuesto
GROUP BY r.id_repuesto, r.nombre_repuesto
ORDER BY unidades_totales DESC
LIMIT 5;

-- Consulta 12: Clientes frecuentes
SELECT c.id_cliente, c.nombre_cliente,
COUNT(p.id_proforma) AS cantidad_proformas,
SUM(p.total) AS monto_acumulado,
MAX(p.fecha) AS ultima_proforma
FROM cliente c
JOIN proforma p ON p.id_cliente = c.id_cliente
GROUP BY c.id_cliente, c.nombre_cliente
HAVING COUNT(p.id_proforma) >= 2
ORDER BY monto_acumulado DESC;
