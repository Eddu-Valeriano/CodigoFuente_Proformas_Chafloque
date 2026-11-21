package proformas.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import proformas.util.Estilo;
import proformas.util.Mensajes;
import proformas.util.TablaUtil;
import proformas.util.TablaUtil.ModeloTabla;

/**
 * Plantilla de las pantallas de mantenimiento (clientes, mecánicos,
 * servicios y repuestos). Ofrece la tabla, el buscador y los botones
 * Nuevo, Editar, Eliminar y Actualizar; cada pantalla concreta solo
 * define cómo cargar, registrar, modificar y eliminar sus datos.
 */
public abstract class PanelCrud extends JPanel {

    protected final ModeloTabla modelo;
    protected final JTable tabla;
    protected final JTextField txtBuscar = new JTextField(22);
    private final JLabel lblConteo = new JLabel(" ");

    protected PanelCrud(String titulo, String descripcion, String[] columnas) {
        super(new BorderLayout(0, 14));
        setBackground(Estilo.FONDO);
        setBorder(Estilo.margen(22, 26, 18, 26));

        modelo = new ModeloTabla(columnas);
        tabla = new JTable(modelo);
        TablaUtil.estilizar(tabla);

        // Cabecera: título y descripción
        JPanel cabecera = new JPanel(new BorderLayout(0, 4));
        cabecera.setOpaque(false);
        cabecera.add(Estilo.titulo(titulo), BorderLayout.NORTH);
        cabecera.add(Estilo.subtitulo(descripcion), BorderLayout.SOUTH);

        // Barra de herramientas: buscador a la izquierda, acciones a la derecha
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        busqueda.setOpaque(false);
        JLabel lblBuscar = new JLabel("Buscar:");
        lblBuscar.setFont(Estilo.fuente(Font.BOLD, 13f));
        txtBuscar.setName("txtBuscar");
        txtBuscar.setPreferredSize(new java.awt.Dimension(240, 32));
        BotonPlano btnBuscar = BotonPlano.secundario("Buscar");
        busqueda.add(lblBuscar);
        busqueda.add(txtBuscar);
        busqueda.add(btnBuscar);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.setOpaque(false);
        BotonPlano btnNuevo = BotonPlano.primario("+ Nuevo");
        BotonPlano btnEditar = BotonPlano.secundario("Editar");
        BotonPlano btnEliminar = BotonPlano.peligro("Eliminar");
        BotonPlano btnActualizar = BotonPlano.secundario("Actualizar");
        btnNuevo.setName("btnNuevo");
        btnEditar.setName("btnEditar");
        btnEliminar.setName("btnEliminar");
        acciones.add(btnActualizar);
        acciones.add(btnEliminar);
        acciones.add(btnEditar);
        acciones.add(btnNuevo);

        barra.add(busqueda, BorderLayout.WEST);
        barra.add(acciones, BorderLayout.EAST);

        JPanel norte = new JPanel(new BorderLayout(0, 16));
        norte.setOpaque(false);
        norte.add(cabecera, BorderLayout.NORTH);
        norte.add(barra, BorderLayout.SOUTH);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));

        lblConteo.setForeground(Estilo.TEXTO_SUAVE);

        add(norte, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(lblConteo, BorderLayout.SOUTH);

        // Eventos
        btnBuscar.addActionListener(e -> recargar());
        txtBuscar.addActionListener(e -> recargar());
        btnActualizar.addActionListener(e -> { txtBuscar.setText(""); recargar(); });
        btnNuevo.addActionListener(e -> { if (nuevo()) recargar(); });
        btnEditar.addActionListener(e -> editarSeleccion());
        btnEliminar.addActionListener(e -> eliminarSeleccion());
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    editarSeleccion();
                }
            }
        });
    }

    /** Vuelve a leer los datos desde la base de datos. */
    public void recargar() {
        try {
            modelo.setRowCount(0);
            cargarDatos(txtBuscar.getText().trim());
            lblConteo.setText(modelo.getRowCount() + " registro(s)");
        } catch (Exception e) {
            Mensajes.error(this, e);
        }
    }

    /** Devuelve la fila seleccionada en el modelo, o -1 si no hay selección. */
    protected int filaSeleccionada() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? -1 : tabla.convertRowIndexToModel(fila);
    }

    private void editarSeleccion() {
        int fila = filaSeleccionada();
        if (fila < 0) {
            Mensajes.aviso(this, "Seleccione un registro de la tabla.");
            return;
        }
        if (editar(fila)) {
            recargar();
        }
    }

    private void eliminarSeleccion() {
        int fila = filaSeleccionada();
        if (fila < 0) {
            Mensajes.aviso(this, "Seleccione un registro de la tabla.");
            return;
        }
        if (!Mensajes.confirmar(this, "¿Desea eliminar el registro seleccionado?\n" + descripcionFila(fila))) {
            return;
        }
        try {
            eliminar(fila);
            recargar();
            Mensajes.info(this, "Registro eliminado correctamente.");
        } catch (Exception e) {
            Mensajes.error(this, e);
        }
    }

    // ------------------------------------------------ métodos que define cada pantalla

    /** Llena {@link #modelo} con los datos que cumplen el filtro. */
    protected abstract void cargarDatos(String filtro) throws Exception;

    /** Abre el formulario de registro. @return true si se guardó */
    protected abstract boolean nuevo();

    /** Abre el formulario de modificación de la fila. @return true si se guardó */
    protected abstract boolean editar(int fila);

    /** Elimina el registro de la fila llamando al procedimiento correspondiente. */
    protected abstract void eliminar(int fila) throws Exception;

    /** Texto que identifica la fila en el mensaje de confirmación. */
    protected String descripcionFila(int fila) {
        return modelo.getValueAt(fila, 0) + " - " + modelo.getValueAt(fila, 1);
    }
}
