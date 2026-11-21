package proformas.vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import proformas.dao.CatalogoDAO;
import proformas.dao.ClienteDAO;
import proformas.dao.MecanicoDAO;
import proformas.dao.ProformaDAO;
import proformas.modelo.Cliente;
import proformas.modelo.DetalleProforma;
import proformas.modelo.ItemCatalogo;
import proformas.modelo.Mecanico;
import proformas.modelo.Proforma;
import proformas.util.Estilo;
import proformas.util.Fechas;
import proformas.util.Mensajes;
import proformas.util.TablaUtil;
import proformas.util.TablaUtil.ModeloTabla;

/**
 * Ventana para elaborar una proforma nueva o modificar una existente.
 * <p>
 * Reproduce la tarea «Elaboración de la proforma» del proceso del taller:
 * se eligen el cliente, el mecánico y el motor, y se agregan los servicios
 * y repuestos con su cantidad. Al guardar, la cabecera y el detalle se
 * envían juntos a un procedimiento con transacción, y el total lo calculan
 * los triggers de la base de datos.
 */
public class DialogoProforma extends JDialog {

    private final ProformaDAO proformaDAO = new ProformaDAO();
    private final Integer idProforma;            // null = proforma nueva
    private final List<DetalleProforma> lineas = new ArrayList<>();
    private List<ItemCatalogo> servicios = new ArrayList<>();
    private List<ItemCatalogo> repuestos = new ArrayList<>();
    private boolean guardado = false;
    private int idGuardado;

    // Cabecera
    private final JComboBox<Cliente> cmbCliente = new JComboBox<>();
    private final JComboBox<Mecanico> cmbMecanico = new JComboBox<>();
    private final JTextField txtMotor = new JTextField();
    private final JTextField txtFecha = new JTextField();
    private final JTextField txtRuc = new JTextField();

    // Agregar líneas
    private final JComboBox<String> cmbTipo = new JComboBox<>(new String[] {"Servicio", "Repuesto"});
    private final JComboBox<ItemCatalogo> cmbItem = new JComboBox<>();
    private final JSpinner spnCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));

    // Detalle
    private final ModeloTabla modeloLineas = new ModeloTabla(
            new String[] {"Tipo", "Código", "Descripción", "Cantidad", "Precio unitario", "Subtotal"});
    private final JTable tablaLineas = new JTable(modeloLineas);
    private final JLabel lblTotal = new JLabel();

    /**
     * @param padre      ventana principal
     * @param idProforma número de la proforma a modificar, o null para una nueva
     */
    public DialogoProforma(Window padre, Integer idProforma) {
        super(padre, idProforma == null ? "Nueva proforma" : "Modificar proforma", ModalityType.APPLICATION_MODAL);
        this.idProforma = idProforma;
        construirInterfaz();
        cargarListas();
        if (idProforma != null) {
            cargarProforma(idProforma);
        } else {
            txtFecha.setText(Fechas.formatear(LocalDate.now()));
        }
        actualizarTablaLineas();
    }

    // ------------------------------------------------------------------ interfaz

    private void construirInterfaz() {
        JPanel raiz = new JPanel(new BorderLayout(0, 12));
        raiz.setBackground(Estilo.BLANCO);
        raiz.setBorder(Estilo.margen(18, 22, 16, 22));

        // Título
        JPanel cabecera = new JPanel(new BorderLayout(0, 3));
        cabecera.setOpaque(false);
        JLabel titulo = new JLabel(idProforma == null ? "Nueva proforma"
                : "Modificar proforma N.° " + String.format("%06d", idProforma));
        titulo.setFont(Estilo.fuente(Font.BOLD, 20f));
        titulo.setForeground(Estilo.AZUL_OSCURO);
        cabecera.add(titulo, BorderLayout.NORTH);
        cabecera.add(Estilo.subtitulo("Complete los datos del cliente y del motor, y agregue los servicios y repuestos."),
                BorderLayout.SOUTH);

        // Datos de la cabecera en dos columnas
        JPanel datos = new JPanel(new GridBagLayout());
        datos.setOpaque(false);
        datos.setBorder(seccion("Datos de la proforma"));
        cmbCliente.setName("cmbCliente");
        cmbMecanico.setName("cmbMecanico");
        txtMotor.setName("txtMotor");
        txtFecha.setName("txtFecha");
        txtRuc.setName("txtRuc");
        agregar(datos, 0, 0, "Cliente", cmbCliente, 3);
        agregar(datos, 1, 0, "Mecánico", cmbMecanico, 1);
        agregar(datos, 1, 2, "Fecha", txtFecha, 1);
        agregar(datos, 2, 0, "Marca / modelo del motor", txtMotor, 1);
        agregar(datos, 2, 2, "RUC (opcional)", txtRuc, 1);

        // Fila para agregar servicios o repuestos
        JPanel agregarItem = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        agregarItem.setOpaque(false);
        agregarItem.setBorder(seccion("Agregar servicio o repuesto"));
        cmbTipo.setName("cmbTipo");
        cmbItem.setName("cmbItem");
        spnCantidad.setName("spnCantidad");
        cmbTipo.setPreferredSize(new Dimension(120, 30));
        cmbItem.setPreferredSize(new Dimension(380, 30));
        spnCantidad.setPreferredSize(new Dimension(80, 30));
        BotonPlano btnAgregar = BotonPlano.primario("Agregar");
        BotonPlano btnQuitar = BotonPlano.secundario("Quitar línea");
        btnAgregar.setName("btnAgregar");
        agregarItem.add(new JLabel("Tipo:"));
        agregarItem.add(cmbTipo);
        agregarItem.add(new JLabel("Ítem:"));
        agregarItem.add(cmbItem);
        agregarItem.add(new JLabel("Cant.:"));
        agregarItem.add(spnCantidad);
        agregarItem.add(btnAgregar);
        agregarItem.add(btnQuitar);

        // Tabla de líneas y total
        TablaUtil.estilizar(tablaLineas);
        tablaLineas.setAutoCreateRowSorter(false);
        tablaLineas.getColumnModel().getColumn(0).setMaxWidth(100);
        tablaLineas.getColumnModel().getColumn(1).setMaxWidth(80);
        tablaLineas.getColumnModel().getColumn(2).setPreferredWidth(330);
        JScrollPane scroll = new JScrollPane(tablaLineas);
        scroll.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));
        scroll.setPreferredSize(new Dimension(860, 220));

        lblTotal.setFont(Estilo.fuente(Font.BOLD, 20f));
        lblTotal.setForeground(Estilo.AZUL_OSCURO);
        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        JLabel notaTotal = Estilo.subtitulo("El total definitivo lo calcula la base de datos al guardar (trigger).");

        JPanel detalle = new JPanel(new BorderLayout(0, 6));
        detalle.setOpaque(false);
        detalle.add(scroll, BorderLayout.CENTER);
        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(notaTotal, BorderLayout.WEST);
        pie.add(lblTotal, BorderLayout.EAST);
        detalle.add(pie, BorderLayout.SOUTH);

        JPanel centro = new JPanel(new BorderLayout(0, 10));
        centro.setOpaque(false);
        JPanel arriba = new JPanel(new BorderLayout(0, 10));
        arriba.setOpaque(false);
        arriba.add(datos, BorderLayout.NORTH);
        arriba.add(agregarItem, BorderLayout.SOUTH);
        centro.add(arriba, BorderLayout.NORTH);
        centro.add(detalle, BorderLayout.CENTER);

        // Botones
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        BotonPlano btnCancelar = BotonPlano.secundario("Cancelar");
        BotonPlano btnGuardar = BotonPlano.primario(idProforma == null ? "Registrar proforma" : "Guardar cambios");
        btnGuardar.setName("btnGuardar");
        botones.add(btnCancelar);
        botones.add(btnGuardar);

        raiz.add(cabecera, BorderLayout.NORTH);
        raiz.add(centro, BorderLayout.CENTER);
        raiz.add(botones, BorderLayout.SOUTH);
        setContentPane(raiz);

        // Eventos
        cmbTipo.addActionListener(e -> cambiarTipo());
        cmbCliente.addActionListener(e -> sugerirRuc());
        btnAgregar.addActionListener(e -> agregarLinea());
        btnQuitar.addActionListener(e -> quitarLinea());
        btnCancelar.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> guardar());

        setMinimumSize(new Dimension(920, 640));
    }

    /** Borde con título para agrupar campos. */
    private javax.swing.border.Border seccion(String titulo) {
        javax.swing.border.TitledBorder t = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Estilo.BORDE), " " + titulo + " ");
        t.setTitleFont(Estilo.fuente(Font.BOLD, 12.5f));
        t.setTitleColor(Estilo.AZUL);
        return BorderFactory.createCompoundBorder(t, Estilo.margen(4, 10, 8, 10));
    }

    /** Agrega «etiqueta + campo» en la posición indicada de la cuadrícula. */
    private void agregar(JPanel panel, int fila, int columna, String etiqueta, JComponent campo, int ancho) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = columna;
        c.gridy = fila;
        c.insets = new Insets(5, columna == 0 ? 0 : 18, 5, 8);
        c.anchor = GridBagConstraints.WEST;
        JLabel l = new JLabel(etiqueta);
        l.setFont(Estilo.fuente(Font.BOLD, 13f));
        panel.add(l, c);
        c.gridx = columna + 1;
        c.gridwidth = ancho;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 0, 5, 0);
        campo.setPreferredSize(new Dimension(220, 30));
        panel.add(campo, c);
    }

    // ------------------------------------------------------------------ datos

    /** Llena las listas desplegables con los datos de la base de datos. */
    private void cargarListas() {
        try {
            cmbCliente.setModel(new DefaultComboBoxModel<>(new ClienteDAO().listar("").toArray(new Cliente[0])));
            cmbMecanico.setModel(new DefaultComboBoxModel<>(new MecanicoDAO().listar("").toArray(new Mecanico[0])));
            servicios = new CatalogoDAO(ItemCatalogo.Tipo.SERVICIO).listar("");
            repuestos = new CatalogoDAO(ItemCatalogo.Tipo.REPUESTO).listar("");
            cambiarTipo();
            cmbCliente.setSelectedIndex(-1);
            cmbMecanico.setSelectedIndex(-1);
        } catch (Exception e) {
            Mensajes.error(this, e);
        }
    }

    /** Carga una proforma existente para modificarla. */
    private void cargarProforma(int id) {
        try {
            Proforma p = proformaDAO.obtener(id);
            if (p == null) {
                Mensajes.aviso(this, "La proforma ya no existe.");
                return;
            }
            seleccionarCliente(p.getIdCliente());
            seleccionarMecanico(p.getIdMecanico());
            txtMotor.setText(p.getMarcaMotor());
            txtFecha.setText(Fechas.formatear(p.getFecha()));
            txtRuc.setText(p.getRuc() == null ? "" : p.getRuc());
            lineas.addAll(p.getDetalle());
        } catch (Exception e) {
            Mensajes.error(this, e);
        }
    }

    private void seleccionarCliente(String idCliente) {
        for (int i = 0; i < cmbCliente.getItemCount(); i++) {
            if (cmbCliente.getItemAt(i).getIdCliente().equals(idCliente)) {
                cmbCliente.setSelectedIndex(i);
                return;
            }
        }
    }

    private void seleccionarMecanico(int idMecanico) {
        for (int i = 0; i < cmbMecanico.getItemCount(); i++) {
            if (cmbMecanico.getItemAt(i).getIdMecanico() == idMecanico) {
                cmbMecanico.setSelectedIndex(i);
                return;
            }
        }
    }

    /** Muestra servicios o repuestos según el tipo elegido. */
    private void cambiarTipo() {
        List<ItemCatalogo> lista = cmbTipo.getSelectedIndex() == 0 ? servicios : repuestos;
        cmbItem.setModel(new DefaultComboBoxModel<>(lista.toArray(new ItemCatalogo[0])));
    }

    /** Si el cliente es una empresa (RUC), se propone su RUC en la proforma. */
    private void sugerirRuc() {
        Cliente c = (Cliente) cmbCliente.getSelectedItem();
        if (c != null && c.tieneRuc()) {
            txtRuc.setText(c.getIdCliente());
        } else if (c != null) {
            txtRuc.setText("");
        }
    }

    /** Agrega el ítem elegido; si ya estaba en la lista, suma la cantidad. */
    private void agregarLinea() {
        ItemCatalogo item = (ItemCatalogo) cmbItem.getSelectedItem();
        if (item == null) {
            return;
        }
        int cantidad = (Integer) spnCantidad.getValue();
        for (DetalleProforma d : lineas) {
            if (d.getItem().getTipo() == item.getTipo() && d.getItem().getId() == item.getId()) {
                d.setCantidad(d.getCantidad() + cantidad);
                actualizarTablaLineas();
                return;
            }
        }
        lineas.add(new DetalleProforma(item, cantidad));
        spnCantidad.setValue(1);
        actualizarTablaLineas();
    }

    private void quitarLinea() {
        int fila = tablaLineas.getSelectedRow();
        if (fila < 0) {
            Mensajes.aviso(this, "Seleccione la línea que desea quitar.");
            return;
        }
        lineas.remove(fila);
        actualizarTablaLineas();
    }

    /** Vuelve a dibujar la tabla de líneas y la vista previa del total. */
    private void actualizarTablaLineas() {
        modeloLineas.setRowCount(0);
        Proforma vistaPrevia = new Proforma();
        for (DetalleProforma d : lineas) {
            ItemCatalogo i = d.getItem();
            modeloLineas.addRow(new Object[] {
                i.getTipo() == ItemCatalogo.Tipo.SERVICIO ? "Servicio" : "Repuesto",
                i.getId(), i.getNombre(), d.getCantidad(), i.getPrecioUnitario(), d.getSubtotal()});
            vistaPrevia.getDetalle().add(d);
        }
        lblTotal.setText("Total: " + TablaUtil.soles(vistaPrevia.calcularTotal()));
    }

    // ------------------------------------------------------------------ guardar

    /** Valida los datos y llama al procedimiento con transacción. */
    private void guardar() {
        Cliente cliente = (Cliente) cmbCliente.getSelectedItem();
        Mecanico mecanico = (Mecanico) cmbMecanico.getSelectedItem();
        String motor = txtMotor.getText().trim();
        String ruc = txtRuc.getText().trim();
        LocalDate fecha;
        try {
            fecha = Fechas.parsear(txtFecha.getText());
        } catch (IllegalArgumentException e) {
            Mensajes.aviso(this, e.getMessage());
            return;
        }
        String error = null;
        if (cliente == null) {
            error = "Seleccione el cliente.";
        } else if (mecanico == null) {
            error = "Seleccione el mecánico responsable.";
        } else if (motor.isEmpty()) {
            error = "Ingrese la marca y el modelo del motor.";
        } else if (fecha == null) {
            error = "Ingrese la fecha de la proforma.";
        } else if (!ruc.isEmpty() && !ruc.matches("\\d{11}")) {
            error = "El RUC debe tener 11 dígitos.";
        } else if (lineas.isEmpty()) {
            error = "Agregue al menos un servicio o un repuesto.";
        }
        if (error != null) {
            Mensajes.aviso(this, error);
            return;
        }

        Proforma p = new Proforma();
        p.setIdCliente(cliente.getIdCliente());
        p.setIdMecanico(mecanico.getIdMecanico());
        p.setMarcaMotor(motor);
        p.setFecha(fecha);
        p.setRuc(ruc);
        p.getDetalle().addAll(lineas);

        try {
            if (idProforma == null) {
                idGuardado = proformaDAO.registrar(p);
            } else {
                p.setIdProforma(idProforma);
                proformaDAO.actualizar(p);
                idGuardado = idProforma;
            }
            // Se vuelve a leer la proforma para mostrar el total calculado por los triggers.
            Proforma guardada = proformaDAO.obtener(idGuardado);
            Mensajes.info(this, "Proforma N.° " + guardada.getNumero()
                    + (idProforma == null ? " registrada" : " actualizada") + " correctamente.\n"
                    + "Total calculado por la base de datos: " + TablaUtil.soles(guardada.getTotal()));
            guardado = true;
            dispose();
        } catch (Exception e) {
            Mensajes.error(this, e);
        }
    }

    /**
     * Muestra la ventana y espera a que se cierre.
     *
     * @return true si la proforma se guardó
     */
    public boolean mostrar() {
        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
        return guardado;
    }

    /** @return número de la proforma guardada */
    public int getIdGuardado() {
        return idGuardado;
    }
}
