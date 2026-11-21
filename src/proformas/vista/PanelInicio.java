package proformas.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import proformas.dao.ProformaDAO;
import proformas.dao.ReporteDAO;
import proformas.modelo.Proforma;
import proformas.util.Estilo;
import proformas.util.Mensajes;
import proformas.util.TablaUtil;
import proformas.util.TablaUtil.ModeloTabla;

/**
 * Pantalla de inicio: indicadores generales (procedimiento sp_resumen_general)
 * y las últimas proformas registradas.
 */
public class PanelInicio extends JPanel {

    private final JLabel valorProformas = new JLabel("-");
    private final JLabel valorMes = new JLabel("-");
    private final JLabel valorMontoMes = new JLabel("-");
    private final JLabel valorClientes = new JLabel("-");
    private final JLabel detalleProformas = new JLabel(" ");
    private final JLabel detalleMes = new JLabel(" ");
    private final JLabel detalleMontoMes = new JLabel(" ");
    private final JLabel detalleClientes = new JLabel(" ");
    private final ModeloTabla modeloUltimas = new ModeloTabla(
            new String[] {"N.° proforma", "Fecha", "Cliente", "Motor", "Total"});

    /**
     * @param accionNueva   acción del botón «Nueva proforma»
     * @param accionReportes acción del botón «Ver reportes»
     */
    public PanelInicio(Runnable accionNueva, Runnable accionReportes) {
        super(new BorderLayout(0, 18));
        setBackground(Estilo.FONDO);
        setBorder(Estilo.margen(22, 26, 18, 26));

        String hoy = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy",
                new Locale("es", "PE")));
        JPanel cabecera = new JPanel(new BorderLayout(0, 4));
        cabecera.setOpaque(false);
        cabecera.add(Estilo.titulo("Bienvenido al sistema de proformas"), BorderLayout.NORTH);
        cabecera.add(Estilo.subtitulo(Character.toUpperCase(hoy.charAt(0)) + hoy.substring(1)
                + "  ·  Importaciones Chafloque E.I.R.L., Arequipa"), BorderLayout.SOUTH);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        BotonPlano btnNueva = BotonPlano.primario("+ Nueva proforma");
        BotonPlano btnReportes = BotonPlano.secundario("Ver reportes");
        btnNueva.addActionListener(e -> accionNueva.run());
        btnReportes.addActionListener(e -> accionReportes.run());
        acciones.add(btnReportes);
        acciones.add(btnNueva);

        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.add(cabecera, BorderLayout.WEST);
        norte.add(acciones, BorderLayout.EAST);

        // Tarjetas de indicadores
        JPanel tarjetas = new JPanel(new GridLayout(1, 4, 14, 0));
        tarjetas.setOpaque(false);
        tarjetas.add(tarjeta("Proformas emitidas", valorProformas, detalleProformas, Estilo.AZUL));
        tarjetas.add(tarjeta("Proformas del mes", valorMes, detalleMes, Estilo.VERDE));
        tarjetas.add(tarjeta("Monto proformado del mes", valorMontoMes, detalleMontoMes, Estilo.NARANJA));
        tarjetas.add(tarjeta("Clientes registrados", valorClientes, detalleClientes, Estilo.ROJO));

        // Últimas proformas
        JTable tabla = new JTable(modeloUltimas);
        TablaUtil.estilizar(tabla);
        TablaUtil.anchos(tabla, 115, 105, 330, 220, 130);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));
        JLabel lblUltimas = new JLabel("Últimas proformas registradas");
        lblUltimas.setFont(Estilo.fuente(Font.BOLD, 15f));
        lblUltimas.setForeground(Estilo.AZUL_OSCURO);
        JPanel ultimas = new JPanel(new BorderLayout(0, 8));
        ultimas.setOpaque(false);
        ultimas.add(lblUltimas, BorderLayout.NORTH);
        ultimas.add(scroll, BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout(0, 18));
        centro.setOpaque(false);
        centro.add(tarjetas, BorderLayout.NORTH);
        centro.add(ultimas, BorderLayout.CENTER);

        add(norte, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
    }

    /** Crea una tarjeta con una franja de color, un título, un valor grande y un detalle. */
    private JPanel tarjeta(String titulo, JLabel valor, JLabel detalle, Color color) {
        JPanel t = new JPanel(new BorderLayout(0, 6));
        t.setBackground(Estilo.BLANCO);
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 0, 0, 0, color),
                BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, Estilo.BORDE),
                        Estilo.margen(14, 16, 14, 16))));
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(Estilo.fuente(Font.BOLD, 13f));
        lblTitulo.setForeground(Estilo.TEXTO_SUAVE);
        valor.setFont(Estilo.fuente(Font.BOLD, 28f));
        valor.setForeground(Estilo.AZUL_OSCURO);
        detalle.setFont(Estilo.fuente(Font.PLAIN, 12f));
        detalle.setForeground(Estilo.TEXTO_SUAVE);
        t.add(lblTitulo, BorderLayout.NORTH);
        t.add(valor, BorderLayout.CENTER);
        t.add(detalle, BorderLayout.SOUTH);
        return t;
    }

    /** Actualiza los indicadores y la lista de últimas proformas. */
    public void recargar() {
        try {
            Map<String, Object> r = new ReporteDAO().resumen();
            valorProformas.setText(String.valueOf(r.get("proformas")));
            detalleProformas.setText("Monto acumulado: " + TablaUtil.soles((BigDecimal) r.get("monto_total")));
            valorMes.setText(String.valueOf(r.get("proformas_mes")));
            detalleMes.setText("En el mes actual");
            valorMontoMes.setText(TablaUtil.soles((BigDecimal) r.get("monto_mes")));
            detalleMontoMes.setText("Suma de los totales del mes");
            valorClientes.setText(String.valueOf(r.get("clientes")));
            detalleClientes.setText(r.get("mecanicos") + " mecánicos registrados");

            modeloUltimas.setRowCount(0);
            List<Proforma> lista = new ProformaDAO().listar("", null, null);
            for (int i = 0; i < Math.min(8, lista.size()); i++) {
                Proforma p = lista.get(i);
                modeloUltimas.addRow(new Object[] {p.getNumero(), p.getFecha(), p.getNombreCliente(),
                        p.getMarcaMotor(), p.getTotal()});
            }
        } catch (Exception e) {
            Mensajes.error(this, e);
        }
    }
}
