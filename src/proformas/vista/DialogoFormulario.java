package proformas.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import proformas.util.Estilo;
import proformas.util.Mensajes;

/**
 * Ventana modal genérica para registrar o modificar datos.
 * <p>
 * Cada pantalla agrega sus campos con {@link #agregarCampo}, define cómo se
 * validan ({@link Validador}) y qué procedimiento se ejecuta al guardar
 * ({@link AccionGuardar}). Si la base de datos rechaza los datos, el error
 * se muestra y la ventana sigue abierta para corregirlos.
 */
public class DialogoFormulario extends JDialog {

    /** Revisa los datos antes de enviarlos; devuelve el mensaje de error o null. */
    public interface Validador {
        String validar();
    }

    /** Operación que se ejecuta al pulsar Guardar (normalmente llama a un DAO). */
    public interface AccionGuardar {
        void guardar() throws Exception;
    }

    private final JPanel campos = new JPanel(new GridBagLayout());
    private int filaActual = 0;
    private boolean guardado = false;
    private Validador validador;
    private AccionGuardar accion;

    public DialogoFormulario(Window padre, String titulo, String descripcion) {
        super(padre, titulo, ModalityType.APPLICATION_MODAL);
        JPanel raiz = new JPanel(new BorderLayout(0, 14));
        raiz.setBackground(Estilo.BLANCO);
        raiz.setBorder(Estilo.margen(20, 24, 18, 24));

        JPanel cabecera = new JPanel(new BorderLayout(0, 4));
        cabecera.setOpaque(false);
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(Estilo.fuente(Font.BOLD, 18f));
        lblTitulo.setForeground(Estilo.AZUL_OSCURO);
        cabecera.add(lblTitulo, BorderLayout.NORTH);
        cabecera.add(Estilo.subtitulo(descripcion), BorderLayout.SOUTH);

        campos.setOpaque(false);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        BotonPlano btnCancelar = BotonPlano.secundario("Cancelar");
        BotonPlano btnGuardar = BotonPlano.primario("Guardar");
        btnGuardar.setName("btnGuardar");
        btnCancelar.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> guardar());
        botones.add(btnCancelar);
        botones.add(btnGuardar);
        getRootPane().setDefaultButton(btnGuardar);

        raiz.add(cabecera, BorderLayout.NORTH);
        raiz.add(campos, BorderLayout.CENTER);
        raiz.add(botones, BorderLayout.SOUTH);
        setContentPane(raiz);
        setResizable(false);
    }

    /**
     * Agrega una fila «etiqueta: campo» con un texto de ayuda opcional debajo.
     *
     * @return el mismo componente, para guardarlo en una variable
     */
    public <T extends JComponent> T agregarCampo(String etiqueta, T campo, String ayuda) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = filaActual;
        c.insets = new Insets(6, 0, ayuda == null ? 6 : 0, 12);
        c.anchor = GridBagConstraints.WEST;
        JLabel l = new JLabel(etiqueta);
        l.setFont(Estilo.fuente(Font.BOLD, 13f));
        campos.add(l, c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, ayuda == null ? 6 : 0, 0);
        campo.setPreferredSize(new java.awt.Dimension(320, 30));
        campos.add(campo, c);
        filaActual++;

        if (ayuda != null) {
            GridBagConstraints a = new GridBagConstraints();
            a.gridx = 1;
            a.gridy = filaActual++;
            a.anchor = GridBagConstraints.WEST;
            a.insets = new Insets(2, 2, 6, 0);
            JLabel lblAyuda = new JLabel(ayuda);
            lblAyuda.setFont(Estilo.fuente(Font.PLAIN, 11.5f));
            lblAyuda.setForeground(Estilo.TEXTO_SUAVE);
            campos.add(lblAyuda, a);
        }
        return campo;
    }

    public void setValidador(Validador validador) {
        this.validador = validador;
    }

    public void setAccionGuardar(AccionGuardar accion) {
        this.accion = accion;
    }

    /** Valida, ejecuta la acción y cierra la ventana si todo salió bien. */
    private void guardar() {
        if (validador != null) {
            String error = validador.validar();
            if (error != null) {
                Mensajes.aviso(this, error);
                return;
            }
        }
        try {
            if (accion != null) {
                accion.guardar();
            }
            guardado = true;
            dispose();
        } catch (Exception ex) {
            Mensajes.error(this, ex);
        }
    }

    /**
     * Muestra la ventana y espera a que se cierre.
     *
     * @return true si se guardó correctamente
     */
    public boolean mostrar() {
        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
        return guardado;
    }
}
