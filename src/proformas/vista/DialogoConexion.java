package proformas.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import proformas.conexion.ConexionBD;
import proformas.conexion.ConfiguracionBD;
import proformas.util.Estilo;
import proformas.util.Mensajes;

/**
 * Ventana de conexión que aparece al iniciar el aplicativo.
 * Permite escribir los datos del servidor MySQL, probar la conexión y
 * guardarlos en config.properties antes de entrar al sistema.
 */
public class DialogoConexion extends JDialog {

    private final JTextField txtHost = new JTextField();
    private final JTextField txtPuerto = new JTextField();
    private final JTextField txtBase = new JTextField();
    private final JTextField txtUsuario = new JTextField();
    private final JPasswordField txtClave = new JPasswordField();
    private final JLabel lblEstado = new JLabel(" ");
    private final JTextArea txtDetalle = new JTextArea(5, 30);
    private boolean aceptado = false;

    public DialogoConexion(Window padre, ConfiguracionBD config) {
        super(padre, "Conexión a la base de datos", ModalityType.APPLICATION_MODAL);
        construirInterfaz();
        txtHost.setText(config.getHost());
        txtPuerto.setText(String.valueOf(config.getPuerto()));
        txtBase.setText(config.getBaseDatos());
        txtUsuario.setText(config.getUsuario());
        txtClave.setText(config.getClave());
    }

    private void construirInterfaz() {
        JPanel raiz = new JPanel(new BorderLayout(0, 14));
        raiz.setBackground(Estilo.BLANCO);
        raiz.setBorder(Estilo.margen(18, 26, 18, 26));

        // Logo y título
        JPanel cabecera = new JPanel(new BorderLayout(0, 6));
        cabecera.setOpaque(false);
        ImageIcon logo = Estilo.imagen("logo.png", 110);
        if (logo != null) {
            JLabel lblLogo = new JLabel(logo);
            lblLogo.setHorizontalAlignment(SwingConstants.CENTER);
            cabecera.add(lblLogo, BorderLayout.NORTH);
        }
        JLabel titulo = new JLabel("Sistema de Proformas", SwingConstants.CENTER);
        titulo.setFont(Estilo.fuente(Font.BOLD, 20f));
        titulo.setForeground(Estilo.AZUL_OSCURO);
        JLabel sub = Estilo.subtitulo("Ingrese los datos de conexión al servidor MySQL");
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel textos = new JPanel(new BorderLayout(0, 2));
        textos.setOpaque(false);
        textos.add(titulo, BorderLayout.NORTH);
        textos.add(sub, BorderLayout.SOUTH);
        cabecera.add(textos, BorderLayout.SOUTH);

        // Campos
        JPanel campos = new JPanel(new GridBagLayout());
        campos.setOpaque(false);
        txtHost.setName("txtHost");
        txtUsuario.setName("txtUsuario");
        txtClave.setName("txtClave");
        fila(campos, 0, "Servidor (host)", txtHost);
        fila(campos, 1, "Puerto", txtPuerto);
        fila(campos, 2, "Base de datos", txtBase);
        fila(campos, 3, "Usuario", txtUsuario);
        fila(campos, 4, "Contraseña", txtClave);

        // Resultado de la prueba
        lblEstado.setFont(Estilo.fuente(Font.BOLD, 13.5f));
        txtDetalle.setEditable(false);
        txtDetalle.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        txtDetalle.setBackground(new Color(0xF6F8FB));
        txtDetalle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE), Estilo.margen(6, 8, 6, 8)));
        txtDetalle.setText("Pulse «Probar conexión» para verificar los datos.");
        JPanel resultado = new JPanel(new BorderLayout(0, 6));
        resultado.setOpaque(false);
        resultado.add(lblEstado, BorderLayout.NORTH);
        resultado.add(txtDetalle, BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.add(campos, BorderLayout.NORTH);
        centro.add(resultado, BorderLayout.CENTER);

        // Botones
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        BotonPlano btnSalir = BotonPlano.secundario("Salir");
        BotonPlano btnProbar = BotonPlano.secundario("Probar conexión");
        BotonPlano btnIngresar = BotonPlano.primario("Guardar e ingresar");
        btnProbar.setName("btnProbar");
        btnIngresar.setName("btnIngresar");
        botones.add(btnSalir);
        botones.add(btnProbar);
        botones.add(btnIngresar);

        raiz.add(cabecera, BorderLayout.NORTH);
        raiz.add(centro, BorderLayout.CENTER);
        raiz.add(botones, BorderLayout.SOUTH);
        setContentPane(raiz);
        getRootPane().setDefaultButton(btnIngresar);

        btnSalir.addActionListener(e -> dispose());
        btnProbar.addActionListener(e -> probar());
        btnIngresar.addActionListener(e -> ingresar());
        setResizable(false);
    }

    private void fila(JPanel panel, int fila, String etiqueta, JComponent campo) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = fila;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 0, 5, 14);
        JLabel l = new JLabel(etiqueta);
        l.setFont(Estilo.fuente(Font.BOLD, 13f));
        panel.add(l, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 0, 5, 0);
        campo.setPreferredSize(new Dimension(280, 30));
        panel.add(campo, c);
    }

    /** Arma una configuración con lo escrito en pantalla. */
    private ConfiguracionBD leerCampos() {
        ConfiguracionBD c = new ConfiguracionBD();
        c.setHost(txtHost.getText().trim());
        c.setPuerto(Integer.parseInt(txtPuerto.getText().trim()));
        c.setBaseDatos(txtBase.getText().trim());
        c.setUsuario(txtUsuario.getText().trim());
        c.setClave(new String(txtClave.getPassword()));
        return c;
    }

    /**
     * Prueba la conexión y muestra el resultado en pantalla.
     *
     * @return true si la conexión fue exitosa
     */
    private boolean probar() {
        try {
            String detalle = ConexionBD.probarConexion(leerCampos());
            lblEstado.setForeground(Estilo.VERDE);
            lblEstado.setText("Conexión exitosa");
            txtDetalle.setText(detalle);
            return true;
        } catch (NumberFormatException e) {
            lblEstado.setForeground(Estilo.ROJO);
            lblEstado.setText("El puerto debe ser un número");
            txtDetalle.setText("Ejemplo: 3306");
        } catch (Exception e) {
            lblEstado.setForeground(Estilo.ROJO);
            lblEstado.setText("No se pudo conectar");
            txtDetalle.setText(Mensajes.traducir(e));
        }
        pack();
        return false;
    }

    /** Si la conexión funciona, guarda los datos y entra al sistema. */
    private void ingresar() {
        if (!probar()) {
            return;
        }
        ConfiguracionBD config = leerCampos();
        try {
            config.guardar();
        } catch (IOException e) {
            Mensajes.aviso(this, "La conexión funciona, pero no se pudo guardar " + ConfiguracionBD.ARCHIVO
                    + ".\n" + e.getMessage());
        }
        ConexionBD.setConfiguracion(config);
        aceptado = true;
        dispose();
    }

    /**
     * Muestra la ventana y espera.
     *
     * @return true si el usuario ingresó con una conexión válida
     */
    public boolean mostrar() {
        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
        return aceptado;
    }

    /** Usado por las capturas de evidencia: ejecuta la prueba sin pulsar el botón. */
    public boolean probarConexion() {
        return probar();
    }
}
