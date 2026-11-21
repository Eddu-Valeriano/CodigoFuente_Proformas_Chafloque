package proformas.vista;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import proformas.conexion.ConexionBD;
import proformas.conexion.ConfiguracionBD;
import proformas.modelo.ItemCatalogo;
import proformas.util.Estilo;
import proformas.util.Mensajes;

/**
 * Ventana principal del aplicativo.
 * <ul>
 *   <li>Arriba: logo, nombre del sistema y datos de la conexión.</li>
 *   <li>Izquierda: menú con los módulos.</li>
 *   <li>Centro: la pantalla del módulo elegido (CardLayout).</li>
 * </ul>
 */
public class VentanaPrincipal extends JFrame {

    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenido = new JPanel(tarjetas);
    private final Map<String, BotonMenu> botonesMenu = new LinkedHashMap<>();
    private final JLabel lblConexion = new JLabel();

    // Módulos del aplicativo
    private final PanelInicio panelInicio;
    private final PanelProformas panelProformas = new PanelProformas();
    private final PanelClientes panelClientes = new PanelClientes();
    private final PanelMecanicos panelMecanicos = new PanelMecanicos();
    private final PanelCatalogo panelServicios = new PanelCatalogo(ItemCatalogo.Tipo.SERVICIO);
    private final PanelCatalogo panelRepuestos = new PanelCatalogo(ItemCatalogo.Tipo.REPUESTO);
    private final PanelReportes panelReportes = new PanelReportes();
    private final PanelAuditoria panelAuditoria = new PanelAuditoria();

    public VentanaPrincipal() {
        super("Sistema de Proformas - Importaciones Chafloque E.I.R.L.");
        panelInicio = new PanelInicio(this::nuevaProformaDesdeInicio, () -> mostrar("Reportes"));

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                salir();
            }
        });
        ImageIcon icono = Estilo.imagen("icono.png", 64);
        if (icono != null) {
            setIconImage(icono.getImage());
        }

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(crearEncabezado(), BorderLayout.NORTH);
        raiz.add(crearMenu(), BorderLayout.WEST);
        raiz.add(contenido, BorderLayout.CENTER);
        setContentPane(raiz);

        contenido.add(panelInicio, "Inicio");
        contenido.add(panelProformas, "Proformas");
        contenido.add(panelClientes, "Clientes");
        contenido.add(panelMecanicos, "Mecánicos");
        contenido.add(panelServicios, "Servicios");
        contenido.add(panelRepuestos, "Repuestos");
        contenido.add(panelReportes, "Reportes");
        contenido.add(panelAuditoria, "Auditoría");

        actualizarDatosConexion();
        setMinimumSize(new Dimension(1100, 680));
        setSize(1280, 760);
        setLocationRelativeTo(null);
        mostrar("Inicio");
    }

    /** Barra superior con el logo y el usuario conectado. */
    private JPanel crearEncabezado() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Estilo.BLANCO);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Estilo.BORDE), Estilo.margen(8, 18, 8, 22)));

        JPanel marca = new JPanel(new BorderLayout(12, 0));
        marca.setOpaque(false);
        ImageIcon logo = Estilo.imagen("logo.png", 52);
        if (logo != null) {
            marca.add(new JLabel(logo), BorderLayout.WEST);
        }
        JPanel textos = new JPanel(new BorderLayout(0, 2));
        textos.setOpaque(false);
        JLabel nombre = new JLabel("Sistema de Proformas");
        nombre.setFont(Estilo.fuente(Font.BOLD, 19f));
        nombre.setForeground(Estilo.AZUL_OSCURO);
        JLabel empresa = new JLabel("Servicios y repuestos  ·  Importaciones Chafloque E.I.R.L.");
        empresa.setFont(Estilo.fuente(Font.PLAIN, 12.5f));
        empresa.setForeground(Estilo.TEXTO_SUAVE);
        textos.add(nombre, BorderLayout.NORTH);
        textos.add(empresa, BorderLayout.SOUTH);
        marca.add(textos, BorderLayout.CENTER);

        lblConexion.setFont(Estilo.fuente(Font.PLAIN, 12.5f));
        lblConexion.setForeground(Estilo.TEXTO_SUAVE);
        lblConexion.setHorizontalAlignment(SwingConstants.RIGHT);

        barra.add(marca, BorderLayout.WEST);
        barra.add(lblConexion, BorderLayout.EAST);
        return barra;
    }

    /** Menú lateral con una opción por módulo. */
    private JPanel crearMenu() {
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(Estilo.AZUL_OSCURO);
        menu.setPreferredSize(new Dimension(220, 0));
        menu.setBorder(Estilo.margen(18, 0, 18, 0));

        menu.add(seccionMenu("PRINCIPAL"));
        agregarOpcion(menu, "Inicio");
        agregarOpcion(menu, "Proformas");
        menu.add(Box.createVerticalStrut(12));
        menu.add(seccionMenu("MANTENIMIENTO"));
        agregarOpcion(menu, "Clientes");
        agregarOpcion(menu, "Mecánicos");
        agregarOpcion(menu, "Servicios");
        agregarOpcion(menu, "Repuestos");
        menu.add(Box.createVerticalStrut(12));
        menu.add(seccionMenu("CONSULTAS"));
        agregarOpcion(menu, "Reportes");
        agregarOpcion(menu, "Auditoría");
        menu.add(Box.createVerticalGlue());

        BotonMenu btnConexion = new BotonMenu("Conexión");
        btnConexion.addActionListener(e -> cambiarConexion());
        BotonMenu btnSalir = new BotonMenu("Salir");
        btnSalir.addActionListener(e -> salir());
        menu.add(btnConexion);
        menu.add(btnSalir);
        return menu;
    }

    private JLabel seccionMenu(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Estilo.fuente(Font.BOLD, 11f));
        l.setForeground(new Color(0x8C9BC4));
        l.setBorder(Estilo.margen(8, 24, 6, 0));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private void agregarOpcion(JPanel menu, final String nombre) {
        BotonMenu b = new BotonMenu(nombre);
        b.setName("menu" + nombre);
        b.addActionListener(e -> mostrar(nombre));
        botonesMenu.put(nombre, b);
        menu.add(b);
    }

    /** Abre un módulo y recarga sus datos desde la base de datos. */
    public void mostrar(String nombre) {
        for (Map.Entry<String, BotonMenu> e : botonesMenu.entrySet()) {
            e.getValue().setSeleccionado(e.getKey().equals(nombre));
        }
        tarjetas.show(contenido, nombre);
        switch (nombre) {
            case "Inicio":    panelInicio.recargar(); break;
            case "Proformas": panelProformas.recargar(); break;
            case "Clientes":  panelClientes.recargar(); break;
            case "Mecánicos": panelMecanicos.recargar(); break;
            case "Servicios": panelServicios.recargar(); break;
            case "Repuestos": panelRepuestos.recargar(); break;
            case "Auditoría": panelAuditoria.recargar(); break;
            default: break;   // Reportes se ejecuta al elegir cada reporte
        }
    }

    /** Desde el inicio: abre el formulario de proforma y luego muestra la lista. */
    private void nuevaProformaDesdeInicio() {
        DialogoProforma d = new DialogoProforma(this, null);
        if (d.mostrar()) {
            mostrar("Proformas");
            panelProformas.seleccionar(d.getIdGuardado());
        }
    }

    private void actualizarDatosConexion() {
        ConfiguracionBD c = ConexionBD.getConfiguracion();
        lblConexion.setText("<html><div style='text-align:right'><font color='#1E8449'>● Conectado</font> a MySQL<br>"
                + c.getUsuario() + "@" + c.getHost() + ":" + c.getPuerto() + " / " + c.getBaseDatos() + "</div></html>");
    }

    /** Permite cambiar el servidor o el usuario sin cerrar el programa. */
    private void cambiarConexion() {
        DialogoConexion d = new DialogoConexion(this, ConexionBD.getConfiguracion());
        if (d.mostrar()) {
            actualizarDatosConexion();
            mostrar("Inicio");
        }
    }

    private void salir() {
        if (Mensajes.confirmar(this, "¿Desea salir del sistema de proformas?")) {
            ConexionBD.cerrar();
            dispose();
            System.exit(0);
        }
    }
}
