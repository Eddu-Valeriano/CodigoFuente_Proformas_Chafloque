package proformas.vista;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import proformas.dao.ClienteDAO;
import proformas.modelo.Cliente;

/** Mantenimiento de clientes: registrar, listar, modificar y eliminar. */
public class PanelClientes extends PanelCrud {

    private final ClienteDAO dao = new ClienteDAO();

    public PanelClientes() {
        super("Clientes",
              "Personas y empresas que solicitan proformas. Se identifican con su DNI (8 dígitos) o RUC (11 dígitos).",
              new String[] {"DNI / RUC", "Nombre o razón social", "Teléfono"});
        tabla.getColumnModel().getColumn(0).setPreferredWidth(140);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(420);
    }

    @Override
    protected void cargarDatos(String filtro) throws Exception {
        for (Cliente c : dao.listar(filtro)) {
            modelo.addRow(new Object[] {c.getIdCliente(), c.getNombre(), c.getTelefono()});
        }
    }

    @Override
    protected boolean nuevo() {
        return abrirFormulario(null);
    }

    @Override
    protected boolean editar(int fila) {
        Cliente c = new Cliente((String) modelo.getValueAt(fila, 0),
                                (String) modelo.getValueAt(fila, 1),
                                (String) modelo.getValueAt(fila, 2));
        return abrirFormulario(c);
    }

    @Override
    protected void eliminar(int fila) throws Exception {
        dao.eliminar((String) modelo.getValueAt(fila, 0));
    }

    /**
     * Formulario de cliente. Si {@code existente} es null se registra uno
     * nuevo; si no, se modifica (el DNI/RUC no se puede cambiar).
     */
    private boolean abrirFormulario(Cliente existente) {
        boolean esNuevo = existente == null;
        DialogoFormulario d = new DialogoFormulario(SwingUtilities.getWindowAncestor(this),
                esNuevo ? "Nuevo cliente" : "Modificar cliente",
                esNuevo ? "Registre los datos del cliente que solicita la proforma."
                        : "Actualice el nombre o el teléfono del cliente.");
        final JTextField txtId = d.agregarCampo("DNI / RUC", new JTextField(), "8 dígitos (DNI) u 11 dígitos (RUC)");
        final JTextField txtNombre = d.agregarCampo("Nombre", new JTextField(), "Nombre completo o razón social");
        final JTextField txtTelefono = d.agregarCampo("Teléfono", new JTextField(), "9 dígitos");
        txtId.setName("txtId");
        txtNombre.setName("txtNombre");
        txtTelefono.setName("txtTelefono");

        if (!esNuevo) {
            txtId.setText(existente.getIdCliente());
            txtId.setEditable(false);
            txtNombre.setText(existente.getNombre());
            txtTelefono.setText(existente.getTelefono());
        }

        // Validación en pantalla; la base de datos vuelve a validar en sus triggers.
        d.setValidador(() -> {
            if (!txtId.getText().trim().matches("\\d{8}|\\d{11}")) {
                return "El DNI debe tener 8 dígitos o el RUC 11 dígitos.";
            }
            if (txtNombre.getText().trim().isEmpty()) {
                return "Ingrese el nombre del cliente.";
            }
            if (!txtTelefono.getText().trim().matches("\\d{9}")) {
                return "El teléfono debe tener 9 dígitos.";
            }
            return null;
        });
        d.setAccionGuardar(() -> {
            Cliente c = new Cliente(txtId.getText().trim(), txtNombre.getText().trim(), txtTelefono.getText().trim());
            if (esNuevo) {
                dao.insertar(c);
            } else {
                dao.actualizar(c);
            }
        });
        return d.mostrar();
    }
}
