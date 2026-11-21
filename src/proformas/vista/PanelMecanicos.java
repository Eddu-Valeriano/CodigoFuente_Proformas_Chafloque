package proformas.vista;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import proformas.dao.MecanicoDAO;
import proformas.modelo.Mecanico;
import proformas.util.Mensajes;

/** Mantenimiento de mecánicos. El código lo asigna MySQL (AUTO_INCREMENT). */
public class PanelMecanicos extends PanelCrud {

    private final MecanicoDAO dao = new MecanicoDAO();

    public PanelMecanicos() {
        super("Mecánicos",
              "Técnicos que evalúan el motor y elaboran la proforma.",
              new String[] {"Código", "Nombre del mecánico"});
        tabla.getColumnModel().getColumn(0).setMaxWidth(120);
    }

    @Override
    protected void cargarDatos(String filtro) throws Exception {
        for (Mecanico m : dao.listar(filtro)) {
            modelo.addRow(new Object[] {m.getIdMecanico(), m.getNombre()});
        }
    }

    @Override
    protected boolean nuevo() {
        return abrirFormulario(null);
    }

    @Override
    protected boolean editar(int fila) {
        return abrirFormulario(new Mecanico((Integer) modelo.getValueAt(fila, 0), (String) modelo.getValueAt(fila, 1)));
    }

    @Override
    protected void eliminar(int fila) throws Exception {
        dao.eliminar((Integer) modelo.getValueAt(fila, 0));
    }

    private boolean abrirFormulario(final Mecanico existente) {
        final boolean esNuevo = existente == null;
        DialogoFormulario d = new DialogoFormulario(SwingUtilities.getWindowAncestor(this),
                esNuevo ? "Nuevo mecánico" : "Modificar mecánico",
                esNuevo ? "El código se genera automáticamente." : "Código del mecánico: " + existente.getIdMecanico());
        final JTextField txtNombre = d.agregarCampo("Nombre", new JTextField(), "Nombres y apellidos");
        txtNombre.setName("txtNombre");
        if (!esNuevo) {
            txtNombre.setText(existente.getNombre());
        }
        d.setValidador(() -> txtNombre.getText().trim().isEmpty() ? "Ingrese el nombre del mecánico." : null);
        d.setAccionGuardar(() -> {
            if (esNuevo) {
                int codigo = dao.insertar(new Mecanico(0, txtNombre.getText().trim()));
                Mensajes.info(this, "Mecánico registrado con el código " + codigo + ".");
            } else {
                existente.setNombre(txtNombre.getText().trim());
                dao.actualizar(existente);
            }
        });
        return d.mostrar();
    }
}
