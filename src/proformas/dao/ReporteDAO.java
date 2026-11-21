package proformas.dao;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import proformas.conexion.ConexionBD;
import proformas.modelo.Reporte;
import proformas.modelo.Reporte.Parametro;
import proformas.modelo.Reporte.TipoParametro;
import proformas.util.Fechas;
import proformas.util.TablaUtil;
import proformas.util.TablaUtil.ModeloTabla;

/**
 * Ejecuta los reportes del aplicativo. Cada reporte corresponde a una de
 * las doce consultas frecuentes del informe y se implementa como un
 * procedimiento almacenado con parámetros.
 */
public class ReporteDAO {

    /** Catálogo de reportes. Usan JOIN los reportes 2, 3, 4, 5, 9, 10, 11 y 12. */
    public static final List<Reporte> REPORTES = Arrays.asList(
        new Reporte(1, "Búsqueda de cliente por nombre",
            "Verifica si un cliente ya está registrado antes de crear uno nuevo. "
          + "Busca por el inicio del nombre y usa el índice idx_cliente_nombre.",
            "sp_rep01_buscar_cliente", false,
            new Parametro("Inicio del nombre", TipoParametro.TEXTO, "Transportes")),
        new Reporte(2, "Encabezado de una proforma",
            "Reconstruye la cabecera del documento que se entrega al cliente: número, "
          + "fecha, cliente, RUC, mecánico, motor y total.",
            "sp_rep02_encabezado_proforma", true,
            new Parametro("N.° de proforma", TipoParametro.ENTERO, "1")),
        new Reporte(3, "Detalle de servicios y repuestos",
            "Cuerpo de la proforma: cada servicio y repuesto con su cantidad, precio "
          + "unitario y subtotal (JOIN y UNION ALL).",
            "sp_rep03_detalle_proforma", true,
            new Parametro("N.° de proforma", TipoParametro.ENTERO, "1")),
        new Reporte(4, "Historial de proformas de un cliente",
            "Trabajos cotizados antes a un cliente: fecha, motor, mecánico y monto.",
            "sp_rep04_historial_cliente", true,
            new Parametro("DNI / RUC del cliente", TipoParametro.TEXTO, "20456789123")),
        new Reporte(5, "Proformas emitidas en un periodo",
            "Todas las proformas de un rango de fechas, útil para el cierre mensual.",
            "sp_rep05_proformas_periodo", true,
            new Parametro("Desde", TipoParametro.FECHA, "01/10/2026"),
            new Parametro("Hasta", TipoParametro.FECHA, "31/10/2026")),
        new Reporte(6, "Catálogo de servicios con precios",
            "Lista de servicios del taller con su precio unitario vigente.",
            "sp_rep06_catalogo_servicios", false),
        new Reporte(7, "Búsqueda de repuestos por nombre",
            "Repuestos cuyo nombre contiene una palabra, por ejemplo «válvula».",
            "sp_rep07_buscar_repuestos", false,
            new Parametro("Palabra a buscar", TipoParametro.TEXTO, "válvula")),
        new Reporte(8, "Monto total proformado por mes",
            "Cantidad de proformas, monto total y monto promedio de cada mes del año.",
            "sp_rep08_monto_por_mes", false,
            new Parametro("Año", TipoParametro.ENTERO, String.valueOf(LocalDate.now().getYear()))),
        new Reporte(9, "Carga de trabajo por mecánico",
            "Proformas elaboradas por cada mecánico y el monto que suman (LEFT JOIN).",
            "sp_rep09_carga_mecanico", true),
        new Reporte(10, "Servicios más solicitados",
            "Servicios que más se repiten en las proformas, con su monto estimado.",
            "sp_rep10_servicios_top", true,
            new Parametro("Cantidad a mostrar", TipoParametro.ENTERO, "5")),
        new Reporte(11, "Repuestos más solicitados",
            "Repuestos con más unidades cotizadas; apoya la planificación de compras.",
            "sp_rep11_repuestos_top", true,
            new Parametro("Cantidad a mostrar", TipoParametro.ENTERO, "5")),
        new Reporte(12, "Clientes frecuentes",
            "Clientes con varias proformas, ordenados por el monto acumulado.",
            "sp_rep12_clientes_frecuentes", true,
            new Parametro("Mínimo de proformas", TipoParametro.ENTERO, "2"))
    );

    /**
     * Ejecuta un reporte con los valores escritos por el usuario.
     *
     * @param reporte reporte elegido
     * @param valores un valor por parámetro, en el mismo orden
     * @return modelo de tabla listo para mostrarse en un JTable
     * @throws SQLException si falla el procedimiento
     * @throws IllegalArgumentException si un valor no tiene el formato esperado
     */
    public ModeloTabla ejecutar(Reporte reporte, List<String> valores) throws SQLException {
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall(reporte.getLlamada())) {
            for (int i = 0; i < reporte.getParametros().size(); i++) {
                Parametro p = reporte.getParametros().get(i);
                String valor = valores.get(i).trim();
                try {
                    switch (p.getTipo()) {
                        case ENTERO:
                            cs.setInt(i + 1, Integer.parseInt(valor));
                            break;
                        case FECHA:
                            cs.setString(i + 1, Fechas.parsear(valor).toString());   // aaaa-mm-dd
                            break;
                        default:
                            cs.setString(i + 1, valor);
                    }
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException("El valor de «" + p.getEtiqueta()
                            + "» no es válido: " + valor);
                }
            }
            try (ResultSet rs = cs.executeQuery()) {
                return TablaUtil.desdeResultSet(rs);
            }
        }
    }

    /** Indicadores de la pantalla de inicio (sp_resumen_general). */
    public Map<String, Object> resumen() throws SQLException {
        Map<String, Object> datos = new LinkedHashMap<>();
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall("{call sp_resumen_general()}");
             ResultSet rs = cs.executeQuery()) {
            if (rs.next()) {
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    datos.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                }
            }
        }
        return datos;
    }
}
