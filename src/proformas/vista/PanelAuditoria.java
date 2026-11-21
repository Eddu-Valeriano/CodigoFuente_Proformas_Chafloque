package proformas.vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import proformas.dao.AuditoriaDAO;
import proformas.util.Estilo;
import proformas.util.Mensajes;
import proformas.util.TablaUtil;
import proformas.util.TablaUtil.ModeloTabla;

/**
 * Muestra la tabla de auditoría. Cada fila la escribió un trigger AFTER
 * INSERT, AFTER UPDATE o AFTER DELETE, con el usuario y la hora del cambio.
 */
public class PanelAuditoria extends JPanel {

    private final AuditoriaDAO dao = new AuditoriaDAO();
    private final JComboBox<String> cmbTabla = new JComboBox<>(new String[] {
        "Todas", "cliente", "mecanico", "servicio", "repuesto", "proforma", "servicio_proforma", "repuesto_proforma"});
    private final JTable tabla = new JTable();
    private final JLabel lblConteo = new JLabel(" ");

    public PanelAuditoria() {
        super(new BorderLayout(0, 14));
        setBackground(Estilo.FONDO);
        setBorder(Estilo.margen(22, 26, 18, 26));

        JPanel cabecera = new JPanel(new BorderLayout(0, 4));
        cabecera.setOpaque(false);
        cabecera.add(Estilo.titulo("Auditoría"), BorderLayout.NORTH);
        cabecera.add(Estilo.subtitulo("Movimientos registrados automáticamente por los triggers de la base de datos."),
                BorderLayout.SOUTH);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barra.setOpaque(false);
        JLabel l = new JLabel("Tabla:");
        l.setFont(Estilo.fuente(Font.BOLD, 13f));
        cmbTabla.setPreferredSize(new Dimension(190, 32));
        BotonPlano btnActualizar = BotonPlano.secundario("Actualizar");
        barra.add(l);
        barra.add(cmbTabla);
        barra.add(btnActualizar);

        JPanel norte = new JPanel(new BorderLayout(0, 16));
        norte.setOpaque(false);
        norte.add(cabecera, BorderLayout.NORTH);
        norte.add(barra, BorderLayout.SOUTH);

        TablaUtil.estilizar(tabla);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));
        lblConteo.setForeground(Estilo.TEXTO_SUAVE);

        add(norte, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(lblConteo, BorderLayout.SOUTH);

        cmbTabla.addActionListener(e -> recargar());
        btnActualizar.addActionListener(e -> recargar());
    }

    /** Lee los últimos movimientos de la tabla elegida. */
    public void recargar() {
        try {
            String filtro = cmbTabla.getSelectedIndex() == 0 ? "" : (String) cmbTabla.getSelectedItem();
            ModeloTabla m = dao.listar(filtro);
            tabla.setModel(m);
            tabla.setAutoCreateRowSorter(true);
            TablaUtil.anchos(tabla, 55, 150, 140, 75, 90, 420, 165);
            lblConteo.setText(m.getRowCount() + " movimiento(s) (se muestran los últimos 500)");
        } catch (Exception e) {
            Mensajes.error(this, e);
        }
    }
}
