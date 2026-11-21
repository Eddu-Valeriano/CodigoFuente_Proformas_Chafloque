package proformas.vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import proformas.dao.ProformaDAO;
import proformas.modelo.DetalleProforma;
import proformas.modelo.ItemCatalogo;
import proformas.modelo.Proforma;
import proformas.util.Estilo;
import proformas.util.Fechas;
import proformas.util.Mensajes;
import proformas.util.TablaUtil;
import proformas.util.TablaUtil.ModeloTabla;

/**
 * Pantalla principal de la funcionalidad: lista de proformas con filtros
 * y, debajo, el detalle de la proforma seleccionada.
 */
public class PanelProformas extends JPanel {

    private final ProformaDAO dao = new ProformaDAO();
    private final List<Proforma> proformas = new ArrayList<>();

    private final JTextField txtBuscar = new JTextField();
    private final JTextField txtDesde = new JTextField();
    private final JTextField txtHasta = new JTextField();

    private final ModeloTabla modelo = new ModeloTabla(
            new String[] {"N.° proforma", "Fecha", "Cliente", "Mecánico", "Motor", "Total"});
    private final JTable tabla = new JTable(modelo);

    private final ModeloTabla modeloDetalle = new ModeloTabla(
            new String[] {"Tipo", "Descripción", "Cantidad", "Precio unitario", "Subtotal"});
    private final JTable tablaDetalle = new JTable(modeloDetalle);
    private final JLabel lblDetalle = new JLabel("Seleccione una proforma para ver su detalle");
    private final JLabel lblTotalDetalle = new JLabel(" ");
    private final JLabel lblConteo = new JLabel(" ");

    public PanelProformas() {
        super(new BorderLayout(0, 14));
        setBackground(Estilo.FONDO);
        setBorder(Estilo.margen(22, 26, 18, 26));

        JPanel cabecera = new JPanel(new BorderLayout(0, 4));
        cabecera.setOpaque(false);
        cabecera.add(Estilo.titulo("Proformas"), BorderLayout.NORTH);
        cabecera.add(Estilo.subtitulo("Proformas de servicios y repuestos emitidas por el taller."), BorderLayout.SOUTH);

        // Filtros
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);
        txtBuscar.setName("txtBuscar");
        txtBuscar.setPreferredSize(new Dimension(200, 32));
        txtDesde.setPreferredSize(new Dimension(105, 32));
        txtHasta.setPreferredSize(new Dimension(105, 32));
        txtDesde.setToolTipText("dd/mm/aaaa");
        txtHasta.setToolTipText("dd/mm/aaaa");
        BotonPlano btnFiltrar = BotonPlano.secundario("Filtrar");
        BotonPlano btnLimpiar = BotonPlano.secundario("Limpiar");
        filtros.add(etiqueta("Cliente / motor / N.°:"));
        filtros.add(txtBuscar);
        filtros.add(etiqueta("Desde:"));
        filtros.add(txtDesde);
        filtros.add(etiqueta("Hasta:"));
        filtros.add(txtHasta);
        filtros.add(btnFiltrar);
        filtros.add(btnLimpiar);

        // Acciones
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        BotonPlano btnNueva = BotonPlano.primario("+ Nueva proforma");
        BotonPlano btnEditar = BotonPlano.secundario("Editar");
        BotonPlano btnEliminar = BotonPlano.peligro("Eliminar");
        btnNueva.setName("btnNueva");
        btnEditar.setName("btnEditar");
        btnEliminar.setName("btnEliminar");
        acciones.add(btnEliminar);
        acciones.add(btnEditar);
        acciones.add(btnNueva);

        JPanel filaTitulo = new JPanel(new BorderLayout());
        filaTitulo.setOpaque(false);
        filaTitulo.add(cabecera, BorderLayout.WEST);
        filaTitulo.add(acciones, BorderLayout.EAST);

        JPanel norte = new JPanel(new BorderLayout(0, 16));
        norte.setOpaque(false);
        norte.add(filaTitulo, BorderLayout.NORTH);
        norte.add(filtros, BorderLayout.SOUTH);

        // Tabla de proformas
        TablaUtil.estilizar(tabla);
        TablaUtil.anchos(tabla, 115, 105, 300, 210, 180, 130);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));
        JPanel arriba = new JPanel(new BorderLayout(0, 4));
        arriba.setOpaque(false);
        arriba.add(scroll, BorderLayout.CENTER);
        lblConteo.setForeground(Estilo.TEXTO_SUAVE);
        arriba.add(lblConteo, BorderLayout.SOUTH);

        // Detalle de la proforma seleccionada
        TablaUtil.estilizar(tablaDetalle);
        TablaUtil.anchos(tablaDetalle, 100, 380, 100, 140, 140);
        lblDetalle.setFont(Estilo.fuente(Font.BOLD, 14f));
        lblDetalle.setForeground(Estilo.AZUL_OSCURO);
        lblTotalDetalle.setFont(Estilo.fuente(Font.BOLD, 15f));
        lblTotalDetalle.setForeground(Estilo.AZUL_OSCURO);
        JPanel cabDetalle = new JPanel(new BorderLayout());
        cabDetalle.setOpaque(false);
        cabDetalle.add(lblDetalle, BorderLayout.WEST);
        cabDetalle.add(lblTotalDetalle, BorderLayout.EAST);
        JScrollPane scrollDetalle = new JScrollPane(tablaDetalle);
        scrollDetalle.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));
        JPanel abajo = new JPanel(new BorderLayout(0, 6));
        abajo.setOpaque(false);
        abajo.setBorder(Estilo.margen(8, 0, 0, 0));
        abajo.add(cabDetalle, BorderLayout.NORTH);
        abajo.add(scrollDetalle, BorderLayout.CENTER);

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, arriba, abajo);
        division.setResizeWeight(0.55);
        division.setBorder(null);
        division.setOpaque(false);

        add(norte, BorderLayout.NORTH);
        add(division, BorderLayout.CENTER);

        // Eventos
        btnFiltrar.addActionListener(e -> recargar());
        txtBuscar.addActionListener(e -> recargar());
        btnLimpiar.addActionListener(e -> {
            txtBuscar.setText("");
            txtDesde.setText("");
            txtHasta.setText("");
            recargar();
        });
        btnNueva.addActionListener(e -> nueva());
        btnEditar.addActionListener(e -> editar());
        btnEliminar.addActionListener(e -> eliminar());
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarDetalle();
            }
        });
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editar();
                }
            }
        });
    }

    private JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Estilo.fuente(Font.BOLD, 13f));
        return l;
    }

    /** Lee las proformas que cumplen los filtros. */
    public void recargar() {
        try {
            LocalDate desde = Fechas.parsear(txtDesde.getText());
            LocalDate hasta = Fechas.parsear(txtHasta.getText());
            proformas.clear();
            proformas.addAll(dao.listar(txtBuscar.getText().trim(), desde, hasta));
            modelo.setRowCount(0);
            BigDecimal suma = BigDecimal.ZERO;
            for (Proforma p : proformas) {
                modelo.addRow(new Object[] {p.getNumero(), p.getFecha(), p.getNombreCliente(),
                        p.getNombreMecanico(), p.getMarcaMotor(), p.getTotal()});
                suma = suma.add(p.getTotal());
            }
            lblConteo.setText(proformas.size() + " proforma(s)  ·  Monto total: " + TablaUtil.soles(suma));
            modeloDetalle.setRowCount(0);
            lblDetalle.setText("Seleccione una proforma para ver su detalle");
            lblTotalDetalle.setText(" ");
        } catch (Exception e) {
            Mensajes.error(this, e);
        }
    }

    /** Proforma seleccionada o null. */
    private Proforma seleccionada() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? null : proformas.get(tabla.convertRowIndexToModel(fila));
    }

    /** Muestra los servicios y repuestos de la proforma seleccionada. */
    private void mostrarDetalle() {
        Proforma p = seleccionada();
        modeloDetalle.setRowCount(0);
        if (p == null) {
            return;
        }
        try {
            for (DetalleProforma d : dao.obtenerDetalle(p.getIdProforma())) {
                modeloDetalle.addRow(new Object[] {
                    d.getItem().getTipo() == ItemCatalogo.Tipo.SERVICIO ? "Servicio" : "Repuesto",
                    d.getItem().getNombre(), d.getCantidad(), d.getItem().getPrecioUnitario(), d.getSubtotal()});
            }
            lblDetalle.setText("Detalle de la proforma N.° " + p.getNumero() + "  ·  " + p.getNombreCliente());
            lblTotalDetalle.setText("Total: " + TablaUtil.soles(p.getTotal()));
        } catch (Exception e) {
            Mensajes.error(this, e);
        }
    }

    private void nueva() {
        DialogoProforma d = new DialogoProforma(SwingUtilities.getWindowAncestor(this), null);
        if (d.mostrar()) {
            recargar();
            seleccionar(d.getIdGuardado());
        }
    }

    private void editar() {
        Proforma p = seleccionada();
        if (p == null) {
            Mensajes.aviso(this, "Seleccione una proforma de la tabla.");
            return;
        }
        DialogoProforma d = new DialogoProforma(SwingUtilities.getWindowAncestor(this), p.getIdProforma());
        if (d.mostrar()) {
            recargar();
            seleccionar(p.getIdProforma());
        }
    }

    private void eliminar() {
        Proforma p = seleccionada();
        if (p == null) {
            Mensajes.aviso(this, "Seleccione una proforma de la tabla.");
            return;
        }
        if (!Mensajes.confirmar(this, "¿Desea eliminar la proforma N.° " + p.getNumero() + " de "
                + p.getNombreCliente() + "?\nTambién se eliminará su detalle de servicios y repuestos.")) {
            return;
        }
        try {
            dao.eliminar(p.getIdProforma());
            recargar();
            Mensajes.info(this, "Proforma eliminada correctamente.");
        } catch (Exception e) {
            Mensajes.error(this, e);
        }
    }

    /** Selecciona en la tabla la proforma indicada. */
    public void seleccionar(int idProforma) {
        for (int i = 0; i < proformas.size(); i++) {
            if (proformas.get(i).getIdProforma() == idProforma) {
                int vista = tabla.convertRowIndexToView(i);
                tabla.setRowSelectionInterval(vista, vista);
                tabla.scrollRectToVisible(tabla.getCellRect(vista, 0, true));
                return;
            }
        }
    }
}
