package proformas.vista;

import java.math.BigDecimal;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import proformas.dao.CatalogoDAO;
import proformas.modelo.ItemCatalogo;
import proformas.util.Mensajes;

/**
 * Mantenimiento del catálogo de servicios o de repuestos.
 * La misma pantalla sirve para los dos catálogos; el tipo decide los textos
 * y los procedimientos que se llaman.
 */
public class PanelCatalogo extends PanelCrud {

    private final ItemCatalogo.Tipo tipo;
    private final CatalogoDAO dao;
    private final String nombreSingular;

    public PanelCatalogo(ItemCatalogo.Tipo tipo) {
        super(tipo == ItemCatalogo.Tipo.SERVICIO ? "Servicios" : "Repuestos",
              tipo == ItemCatalogo.Tipo.SERVICIO
                  ? "Trabajos que realiza el taller (rectificado, cepillado, tornería...) con su precio unitario."
                  : "Repuestos que vende la empresa (válvulas, pistones, anillos...) con su precio unitario.",
              new String[] {"Código", tipo == ItemCatalogo.Tipo.SERVICIO ? "Servicio" : "Repuesto", "Precio unitario"});
        this.tipo = tipo;
        this.dao = new CatalogoDAO(tipo);
        this.nombreSingular = tipo == ItemCatalogo.Tipo.SERVICIO ? "servicio" : "repuesto";
        tabla.getColumnModel().getColumn(0).setMaxWidth(120);
        tabla.getColumnModel().getColumn(2).setMaxWidth(200);
    }

    @Override
    protected void cargarDatos(String filtro) throws Exception {
        for (ItemCatalogo i : dao.listar(filtro)) {
            modelo.addRow(new Object[] {i.getId(), i.getNombre(), i.getPrecioUnitario()});
        }
    }

    @Override
    protected boolean nuevo() {
        return abrirFormulario(null);
    }

    @Override
    protected boolean editar(int fila) {
        return abrirFormulario(new ItemCatalogo(tipo, (Integer) modelo.getValueAt(fila, 0),
                (String) modelo.getValueAt(fila, 1), (BigDecimal) modelo.getValueAt(fila, 2)));
    }

    @Override
    protected void eliminar(int fila) throws Exception {
        dao.eliminar((Integer) modelo.getValueAt(fila, 0));
    }

    private boolean abrirFormulario(final ItemCatalogo existente) {
        final boolean esNuevo = existente == null;
        DialogoFormulario d = new DialogoFormulario(SwingUtilities.getWindowAncestor(this),
                (esNuevo ? "Nuevo " : "Modificar ") + nombreSingular,
                esNuevo ? "El código se genera automáticamente."
                        : "Código del " + nombreSingular + ": " + existente.getId());
        final JTextField txtNombre = d.agregarCampo("Nombre", new JTextField(), "Máximo 100 caracteres, no se puede repetir");
        final JTextField txtPrecio = d.agregarCampo("Precio (S/)", new JTextField(), "Use punto decimal, por ejemplo 120.00");
        txtNombre.setName("txtNombre");
        txtPrecio.setName("txtPrecio");
        if (!esNuevo) {
            txtNombre.setText(existente.getNombre());
            txtPrecio.setText(existente.getPrecioUnitario().toPlainString());
        }
        d.setValidador(() -> {
            if (txtNombre.getText().trim().isEmpty()) {
                return "Ingrese el nombre del " + nombreSingular + ".";
            }
            try {
                if (new BigDecimal(txtPrecio.getText().trim()).signum() < 0) {
                    return "El precio no puede ser negativo.";
                }
            } catch (NumberFormatException e) {
                return "El precio debe ser un número, por ejemplo 120.00";
            }
            return null;
        });
        d.setAccionGuardar(() -> {
            ItemCatalogo item = new ItemCatalogo(tipo, esNuevo ? 0 : existente.getId(),
                    txtNombre.getText().trim(), new BigDecimal(txtPrecio.getText().trim()));
            if (esNuevo) {
                int codigo = dao.insertar(item);
                Mensajes.info(this, "El " + nombreSingular + " se registró con el código " + codigo + ".");
            } else {
                dao.actualizar(item);
            }
        });
        return d.mostrar();
    }
}
