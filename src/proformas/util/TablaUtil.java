package proformas.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * Utilidades para tablas: convierte un ResultSet en un modelo de tabla,
 * da formato a montos y fechas, y aplica el estilo común a los JTable.
 */
public final class TablaUtil {

    /** Formato de montos: 1,610.00 */
    private static final DecimalFormat MONTO =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    /** Formato de fecha y hora: 14/11/2026 08:15:30 */
    private static final java.time.format.DateTimeFormatter FECHA_HORA =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private TablaUtil() {
    }

    /** Modelo de tabla de solo lectura que conoce el tipo de dato de cada columna. */
    public static class ModeloTabla extends DefaultTableModel {

        public ModeloTabla(String[] columnas) {
            super(columnas, 0);
        }

        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }

        /** El tipo se deduce del primer valor no nulo; sirve para ordenar y alinear. */
        @Override
        public Class<?> getColumnClass(int columna) {
            for (int fila = 0; fila < getRowCount(); fila++) {
                Object valor = getValueAt(fila, columna);
                if (valor != null) {
                    return valor.getClass();
                }
            }
            return Object.class;
        }
    }

    /**
     * Copia todas las filas de un ResultSet en un modelo de tabla.
     * Los nombres de columna se convierten a un texto legible
     * (nombre_cliente pasa a «Nombre cliente»).
     */
    public static ModeloTabla desdeResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int n = md.getColumnCount();
        String[] columnas = new String[n];
        for (int i = 0; i < n; i++) {
            columnas[i] = nombreLegible(md.getColumnLabel(i + 1));
        }
        ModeloTabla modelo = new ModeloTabla(columnas);
        while (rs.next()) {
            Object[] fila = new Object[n];
            for (int i = 0; i < n; i++) {
                Object valor = rs.getObject(i + 1);
                // Los enteros grandes y los conteos se muestran como Long
                if (valor instanceof java.math.BigInteger) {
                    valor = ((java.math.BigInteger) valor).longValue();
                }
                fila[i] = valor;
            }
            modelo.addRow(fila);
        }
        return modelo;
    }

    /** Convierte un nombre de columna de la base de datos en un encabezado legible. */
    public static String nombreLegible(String columna) {
        switch (columna) {
            case "nro_proforma":       return "N.° proforma";
            case "id_cliente":         return "DNI / RUC";
            case "ruc":                return "RUC";
            case "anio":               return "Año";
            case "id_auditoria":       return "N.°";
            case "fecha_hora":         return "Fecha y hora";
            case "accion":             return "Acción";
            case "id_mecanico":
            case "id_servicio":
            case "id_repuesto":        return "Código";
            case "nombre_cliente":     return "Cliente";
            case "nombre_mecanico":    return "Mecánico";
            case "nombre_servicio":    return "Servicio";
            case "nombre_repuesto":    return "Repuesto";
            case "marca_motor":        return "Motor";
            case "cantidad_proformas": return "N.° de proformas";
            case "ultima_proforma":    return "Última proforma";
            case "precio_unitario":    return "Precio unitario";
            case "descripcion":        return "Descripción";
            case "id_registro":        return "Registro";
            default:                   break;
        }
        String texto = columna.replace("id_", "código_").replace('_', ' ');
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    /** @return true si la columna contiene montos en soles */
    private static boolean esMonto(String encabezado) {
        String e = encabezado.toLowerCase();
        if (e.contains("cantidad") || e.contains("unidades")) {
            return false;
        }
        return e.contains("total") || e.contains("monto") || e.contains("precio") || e.contains("subtotal");
    }

    /**
     * Aplica el estilo común: filas más altas, encabezado azul, colores de
     * selección y formato de montos («S/ 1,610.00») y fechas (dd/mm/aaaa).
     */
    public static void estilizar(final JTable tabla) {
        tabla.setRowHeight(28);
        tabla.setFont(Estilo.fuente(Font.PLAIN, 13f));
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(Estilo.BORDE);
        tabla.setSelectionBackground(new Color(0xCFDBF7));
        tabla.setSelectionForeground(Estilo.TEXTO);
        tabla.setFillsViewportHeight(true);
        tabla.setAutoCreateRowSorter(true);
        tabla.setIntercellSpacing(new java.awt.Dimension(0, 1));

        JTableHeader encabezado = tabla.getTableHeader();
        encabezado.setReorderingAllowed(false);
        encabezado.setPreferredSize(new java.awt.Dimension(0, 32));
        encabezado.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel,
                                                           boolean foco, int fila, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, valor, sel, foco, fila, col);
                l.setBackground(Estilo.AZUL_OSCURO);
                l.setForeground(Color.WHITE);
                l.setFont(Estilo.fuente(Font.BOLD, 12.5f));
                l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
        });

        // Un solo renderizador para todas las celdas: alinea y da formato según el tipo.
        DefaultTableCellRenderer celdas = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel,
                                                           boolean foco, int fila, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, valor, sel, foco, fila, col);
                l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                String encabezadoCol = t.getColumnName(col);
                if (valor instanceof BigDecimal && esMonto(encabezadoCol)) {
                    l.setText("S/ " + MONTO.format(valor));
                    l.setHorizontalAlignment(SwingConstants.RIGHT);
                } else if (valor instanceof Number) {
                    l.setHorizontalAlignment(SwingConstants.RIGHT);
                } else if (valor instanceof java.sql.Timestamp) {
                    l.setText(((java.sql.Timestamp) valor).toLocalDateTime().format(FECHA_HORA));
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                } else if (valor instanceof java.time.LocalDateTime) {
                    l.setText(((java.time.LocalDateTime) valor).format(FECHA_HORA));
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                } else if (valor instanceof Date) {
                    l.setText(Fechas.formatear(((Date) valor).toLocalDate()));
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                } else if (valor instanceof java.time.LocalDate) {
                    l.setText(Fechas.formatear((java.time.LocalDate) valor));
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                } else {
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                }
                if (!sel) {
                    l.setBackground(fila % 2 == 0 ? Color.WHITE : Estilo.FILA_ALTERNA);
                }
                return l;
            }
        };
        tabla.setDefaultRenderer(Object.class, celdas);
        tabla.setDefaultRenderer(Number.class, celdas);
        tabla.setDefaultRenderer(BigDecimal.class, celdas);
        tabla.setDefaultRenderer(Integer.class, celdas);
        tabla.setDefaultRenderer(Long.class, celdas);
        tabla.setDefaultRenderer(Date.class, celdas);
        tabla.setDefaultRenderer(java.time.LocalDate.class, celdas);
        tabla.setDefaultRenderer(java.sql.Timestamp.class, celdas);
        tabla.setDefaultRenderer(java.time.LocalDateTime.class, celdas);
    }

    /**
     * Fija el ancho preferido de cada columna (en píxeles). Las columnas
     * pequeñas también reciben ese ancho como mínimo para que no se corten.
     */
    public static void anchos(JTable tabla, int... anchos) {
        for (int i = 0; i < anchos.length && i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
            if (anchos[i] <= 160) {
                tabla.getColumnModel().getColumn(i).setMinWidth(anchos[i]);
            }
        }
    }

    /**
     * Ajusta el ancho de cada columna según su contenido (encabezado y
     * primeras filas). Se usa en los reportes, cuyas columnas cambian.
     */
    public static void ajustarColumnas(JTable tabla) {
        for (int c = 0; c < tabla.getColumnCount(); c++) {
            javax.swing.table.TableColumn col = tabla.getColumnModel().getColumn(c);
            Component h = tabla.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(tabla, col.getHeaderValue(), false, false, -1, c);
            int ancho = h.getPreferredSize().width;
            for (int f = 0; f < Math.min(tabla.getRowCount(), 200); f++) {
                Component celda = tabla.prepareRenderer(tabla.getCellRenderer(f, c), f, c);
                ancho = Math.max(ancho, celda.getPreferredSize().width);
            }
            col.setPreferredWidth(ancho + 16);
        }
    }

    /** Formatea un monto como «S/ 1,610.00». */
    public static String soles(BigDecimal monto) {
        return "S/ " + MONTO.format(monto == null ? BigDecimal.ZERO : monto);
    }
}
