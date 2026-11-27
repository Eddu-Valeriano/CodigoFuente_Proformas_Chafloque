package proformas.vista;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import proformas.conexion.ConexionBD;
import proformas.conexion.ConfiguracionBD;
import proformas.util.Estilo;
import proformas.util.Mensajes;

/**
 * Ventana de conexión que aparece al iniciar el aplicativo.
 * Solo pide el usuario y la contraseña de MySQL. Los datos del servidor
 * (host, puerto y base de datos) vienen cargados por defecto y quedan
 * ocultos; se pueden cambiar con el botón de la tuerca de la esquina superior.
 */
public class DialogoConexion extends JDialog {

    // Valores por defecto del servidor
    private static final String HOST_DEFECTO = "localhost";
    private static final String PUERTO_DEFECTO = "3306";
    private static final String BASE_DEFECTO = "proformataller";

    private final JTextField txtHost = new JTextField();
    private final JTextField txtPuerto = new JTextField();
    private final JTextField txtBase = new JTextField();
    private final JTextField txtUsuario = new JTextField();
    private final JPasswordField txtClave = new JPasswordField();
    private final JLabel lblEstado = new JLabel(" ");
    private final JLabel lblServidor = new JLabel();
    private final JTextArea txtDetalle = new JTextArea(4, 30);
    private final JPanel panelServidor = new JPanel(new BorderLayout(0, 8));
    private final JButton btnTuerca = new JButton();
    private boolean aceptado = false;

    public DialogoConexion(Window padre, ConfiguracionBD config) {
        super(padre, "Conexión a la base de datos", ModalityType.APPLICATION_MODAL);
        construirInterfaz();
        // El servidor se toma de config.properties (por defecto localhost:3306/proformataller);
        // el usuario y la contraseña se escriben cada vez.
        txtHost.setText(config.getHost());
        txtPuerto.setText(String.valueOf(config.getPuerto()));
        txtBase.setText(config.getBaseDatos());
        actualizarResumenServidor();
    }

    private void construirInterfaz() {
        JPanel raiz = new JPanel(new BorderLayout(0, 14));
        raiz.setBackground(Estilo.BLANCO);
        raiz.setBorder(Estilo.margen(10, 26, 18, 14));

        // Botón de la tuerca en la esquina superior derecha
        btnTuerca.setIcon(new IconoTuerca(20, Estilo.TEXTO_SUAVE));
        btnTuerca.setRolloverIcon(new IconoTuerca(20, Estilo.AZUL));
        btnTuerca.setToolTipText("Configuración del servidor (host, puerto y base de datos)");
        btnTuerca.setName("btnTuerca");
        btnTuerca.setContentAreaFilled(false);
        btnTuerca.setBorderPainted(false);
        btnTuerca.setFocusPainted(false);
        btnTuerca.setOpaque(false);
        btnTuerca.setMargin(new Insets(4, 4, 4, 4));
        btnTuerca.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel barraSuperior = new JPanel(new BorderLayout());
        barraSuperior.setOpaque(false);
        barraSuperior.add(btnTuerca, BorderLayout.EAST);

        // Logo y título
        JPanel cabecera = new JPanel(new BorderLayout(0, 6));
        cabecera.setOpaque(false);
        cabecera.setBorder(Estilo.margen(0, 0, 0, 12));
        ImageIcon logo = Estilo.imagen("logo.png", 110);
        if (logo != null) {
            JLabel lblLogo = new JLabel(logo);
            lblLogo.setHorizontalAlignment(SwingConstants.CENTER);
            cabecera.add(lblLogo, BorderLayout.NORTH);
        }
        JLabel titulo = new JLabel("ProformaTaller", SwingConstants.CENTER);
        titulo.setFont(Estilo.fuente(Font.BOLD, 20f));
        titulo.setForeground(Estilo.AZUL_OSCURO);
        JLabel sub = Estilo.subtitulo("Ingrese su usuario y contraseña de MySQL");
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        lblServidor.setFont(Estilo.fuente(Font.PLAIN, 12f));
        lblServidor.setForeground(Estilo.TEXTO_SUAVE);
        lblServidor.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel textos = new JPanel(new BorderLayout(0, 2));
        textos.setOpaque(false);
        textos.add(titulo, BorderLayout.NORTH);
        textos.add(sub, BorderLayout.CENTER);
        textos.add(lblServidor, BorderLayout.SOUTH);
        cabecera.add(textos, BorderLayout.SOUTH);

        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.add(barraSuperior, BorderLayout.NORTH);
        norte.add(cabecera, BorderLayout.CENTER);

        // Usuario y contraseña (siempre visibles)
        JPanel credenciales = new JPanel(new GridBagLayout());
        credenciales.setOpaque(false);
        txtHost.setName("txtHost");
        txtUsuario.setName("txtUsuario");
        txtClave.setName("txtClave");
        fila(credenciales, 0, "Usuario", txtUsuario);
        fila(credenciales, 1, "Contraseña", txtClave);

        // Datos del servidor (ocultos hasta pulsar la tuerca)
        JPanel camposServidor = new JPanel(new GridBagLayout());
        camposServidor.setOpaque(false);
        fila(camposServidor, 0, "Servidor (host)", txtHost);
        fila(camposServidor, 1, "Puerto", txtPuerto);
        fila(camposServidor, 2, "Base de datos", txtBase);
        JLabel tituloServidor = new JLabel("Configuración del servidor");
        tituloServidor.setFont(Estilo.fuente(Font.BOLD, 13f));
        tituloServidor.setForeground(Estilo.AZUL_OSCURO);
        JButton btnRestablecer = new JButton("<html><u>Restablecer valores por defecto</u></html>");
        btnRestablecer.setForeground(Estilo.AZUL);
        btnRestablecer.setContentAreaFilled(false);
        btnRestablecer.setBorderPainted(false);
        btnRestablecer.setFocusPainted(false);
        btnRestablecer.setMargin(new Insets(0, 0, 0, 0));
        btnRestablecer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel tituloFila = new JPanel(new BorderLayout());
        tituloFila.setOpaque(false);
        tituloFila.add(tituloServidor, BorderLayout.WEST);
        tituloFila.add(btnRestablecer, BorderLayout.EAST);
        panelServidor.setBackground(new Color(0xF6F8FB));
        panelServidor.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE), Estilo.margen(8, 12, 6, 12)));
        panelServidor.add(tituloFila, BorderLayout.NORTH);
        panelServidor.add(camposServidor, BorderLayout.CENTER);
        panelServidor.setVisible(false);

        JPanel formulario = new JPanel(new BorderLayout(0, 10));
        formulario.setOpaque(false);
        formulario.add(credenciales, BorderLayout.NORTH);
        formulario.add(panelServidor, BorderLayout.CENTER);

        // Resultado de la prueba
        lblEstado.setFont(Estilo.fuente(Font.BOLD, 13.5f));
        txtDetalle.setEditable(false);
        txtDetalle.setLineWrap(true);
        txtDetalle.setWrapStyleWord(true);
        txtDetalle.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        txtDetalle.setBackground(new Color(0xF6F8FB));
        txtDetalle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE), Estilo.margen(6, 8, 6, 8)));
        txtDetalle.setText("Escriba su usuario y contraseña y pulse «Ingresar».");
        JPanel resultado = new JPanel(new BorderLayout(0, 6));
        resultado.setOpaque(false);
        resultado.add(lblEstado, BorderLayout.NORTH);
        resultado.add(txtDetalle, BorderLayout.CENTER);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.setBorder(Estilo.margen(0, 0, 0, 12));
        centro.add(formulario, BorderLayout.NORTH);
        centro.add(resultado, BorderLayout.CENTER);

        // Botones
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.setBorder(Estilo.margen(0, 0, 0, 4));
        BotonPlano btnSalir = BotonPlano.secundario("Salir");
        BotonPlano btnProbar = BotonPlano.secundario("Probar conexión");
        BotonPlano btnIngresar = BotonPlano.primario("Ingresar");
        btnProbar.setName("btnProbar");
        btnIngresar.setName("btnIngresar");
        botones.add(btnSalir);
        botones.add(btnProbar);
        botones.add(btnIngresar);

        raiz.add(norte, BorderLayout.NORTH);
        raiz.add(centro, BorderLayout.CENTER);
        raiz.add(botones, BorderLayout.SOUTH);
        setContentPane(raiz);
        getRootPane().setDefaultButton(btnIngresar);

        btnTuerca.addActionListener(e -> alternarServidor());
        btnRestablecer.addActionListener(e -> restablecerServidor());
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
        l.setPreferredSize(new Dimension(115, 20));
        panel.add(l, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 0, 5, 0);
        campo.setPreferredSize(new Dimension(280, 30));
        panel.add(campo, c);
    }

    /** Muestra u oculta los datos del servidor. */
    private void alternarServidor() {
        mostrarServidor(!panelServidor.isVisible());
    }

    private void mostrarServidor(boolean visible) {
        panelServidor.setVisible(visible);
        btnTuerca.setIcon(new IconoTuerca(20, visible ? Estilo.AZUL : Estilo.TEXTO_SUAVE));
        actualizarResumenServidor();
        pack();
        if (visible) {
            txtHost.requestFocusInWindow();
        }
    }

    /** Vuelve a poner localhost, 3306 y proformataller. */
    private void restablecerServidor() {
        txtHost.setText(HOST_DEFECTO);
        txtPuerto.setText(PUERTO_DEFECTO);
        txtBase.setText(BASE_DEFECTO);
        actualizarResumenServidor();
    }

    /** Texto pequeño bajo el título con el servidor al que se conectará. */
    private void actualizarResumenServidor() {
        lblServidor.setText("Servidor: " + txtHost.getText().trim() + ":" + txtPuerto.getText().trim()
                + " / " + txtBase.getText().trim());
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
        actualizarResumenServidor();
        if (txtUsuario.getText().trim().isEmpty()) {
            lblEstado.setForeground(Estilo.ROJO);
            lblEstado.setText("Ingrese el usuario");
            txtDetalle.setText("Escriba el usuario de MySQL, por ejemplo: proformataller_app");
            txtUsuario.requestFocusInWindow();
            pack();
            return false;
        }
        try {
            String detalle = ConexionBD.probarConexion(leerCampos());
            lblEstado.setForeground(Estilo.VERDE);
            lblEstado.setText("Conexión exitosa");
            txtDetalle.setText(detalle);
            pack();
            return true;
        } catch (NumberFormatException e) {
            lblEstado.setForeground(Estilo.ROJO);
            lblEstado.setText("El puerto debe ser un número");
            txtDetalle.setText("Ejemplo: 3306");
            mostrarServidor(true);
        } catch (Exception e) {
            lblEstado.setForeground(Estilo.ROJO);
            lblEstado.setText("No se pudo conectar");
            txtDetalle.setText(Mensajes.traducir(e)
                    + "\n\nSi el servidor no es " + txtHost.getText().trim() + ":" + txtPuerto.getText().trim()
                    + ", cámbielo con el botón de la tuerca (esquina superior).");
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
        SwingUtilities.invokeLater(txtUsuario::requestFocusInWindow);
        setVisible(true);
        return aceptado;
    }

    /** Usado por las capturas de evidencia: ejecuta la prueba sin pulsar el botón. */
    public boolean probarConexion() {
        return probar();
    }

    /** Icono de tuerca (engranaje) dibujado con Java2D, sin imágenes externas. */
    private static class IconoTuerca implements Icon {
        private final int tam;
        private final Color color;

        IconoTuerca(int tam, Color color) {
            this.tam = tam;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            double centro = tam / 2.0;
            double radio = tam * 0.34;
            Area engranaje = new Area(new Ellipse2D.Double(centro - radio, centro - radio, radio * 2, radio * 2));
            double anchoDiente = tam * 0.18;
            double largoDiente = tam * 0.5;
            for (int i = 0; i < 8; i++) {
                Rectangle2D diente = new Rectangle2D.Double(centro - anchoDiente / 2, centro - largoDiente,
                        anchoDiente, largoDiente);
                engranaje.add(new Area(AffineTransform.getRotateInstance(Math.toRadians(i * 45), centro, centro)
                        .createTransformedShape(diente)));
            }
            double hueco = tam * 0.15;
            engranaje.subtract(new Area(new Ellipse2D.Double(centro - hueco, centro - hueco, hueco * 2, hueco * 2)));
            g2.setColor(color);
            g2.fill(engranaje);
            g2.setStroke(new BasicStroke(1f));
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return tam;
        }

        @Override
        public int getIconHeight() {
            return tam;
        }
    }
}
