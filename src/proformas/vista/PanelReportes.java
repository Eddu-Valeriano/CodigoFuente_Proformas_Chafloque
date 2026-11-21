package proformas.vista;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import proformas.dao.ReporteDAO;
import proformas.modelo.Reporte;
import proformas.util.Estilo;
import proformas.util.Mensajes;
import proformas.util.TablaUtil;
import proformas.util.TablaUtil.ModeloTabla;

/**
 * Pantalla de reportes. A la izquierda se elige uno de los 12 reportes;
 * a la derecha se escriben sus parámetros, se ejecuta el procedimiento
 * almacenado y se muestra el resultado, que puede exportarse a CSV.
 */
public class PanelReportes extends JPanel {

    private final ReporteDAO dao = new ReporteDAO();
    private final JList<Reporte> lista = new JList<>(ReporteDAO.REPORTES.toArray(new Reporte[0]));

    private final JLabel lblTitulo = new JLabel();
    private final JLabel txtDescripcion = new JLabel();
    private final JLabel lblProcedimiento = new JLabel();
    private final JPanel panelParametros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final List<JTextField> camposParametros = new ArrayList<>();

    private final JTable tabla = new JTable();
    private final JLabel lblResultado = new JLabel(" ");

    public PanelReportes() {
        super(new BorderLayout(0, 14));
        setBackground(Estilo.FONDO);
        setBorder(Estilo.margen(22, 26, 18, 26));

        JPanel cabecera = new JPanel(new BorderLayout(0, 4));
        cabecera.setOpaque(false);
        cabecera.add(Estilo.titulo("Reportes"), BorderLayout.NORTH);
        cabecera.add(Estilo.subtitulo("Consultas frecuentes del taller. Cada reporte ejecuta un procedimiento almacenado."),
                BorderLayout.SOUTH);

        // Lista de reportes
        lista.setName("listaReportes");
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setFixedCellHeight(34);
        lista.setFont(Estilo.fuente(Font.PLAIN, 13f));
        lista.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> l, Object valor, int i,
                                                                   boolean sel, boolean foco) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, valor, i, sel, foco);
                c.setBorder(Estilo.margen(0, 10, 0, 6));
                c.setOpaque(true);
                c.setBackground(sel ? Estilo.AZUL : (i % 2 == 0 ? Estilo.BLANCO : Estilo.FILA_ALTERNA));
                c.setForeground(sel ? java.awt.Color.WHITE : Estilo.TEXTO);
                Reporte r = (Reporte) valor;
                String colorJoin = sel ? "#C9D6FF" : "#2846A8";
                c.setText("<html><b>R" + String.format("%02d", r.getNumero()) + "</b>&nbsp; " + r.getTitulo()
                        + (r.isUsaJoin() ? " <font color='" + colorJoin + "'>· JOIN</font>" : "") + "</html>");
                return c;
            }
        });
        JScrollPane scrollLista = new JScrollPane(lista);
        scrollLista.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));
        scrollLista.setPreferredSize(new Dimension(330, 0));
        scrollLista.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Detalle del reporte elegido
        lblTitulo.setFont(Estilo.fuente(Font.BOLD, 17f));
        lblTitulo.setForeground(Estilo.AZUL_OSCURO);
        txtDescripcion.setFont(Estilo.fuente(Font.PLAIN, 13f));
        txtDescripcion.setForeground(Estilo.TEXTO_SUAVE);
        lblProcedimiento.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        lblProcedimiento.setForeground(Estilo.AZUL);
        panelParametros.setOpaque(false);

        BotonPlano btnEjecutar = BotonPlano.primario("Ejecutar reporte");
        BotonPlano btnExportar = BotonPlano.secundario("Exportar CSV");
        btnEjecutar.setName("btnEjecutar");

        JPanel infoReporte = new JPanel();
        infoReporte.setOpaque(false);
        infoReporte.setLayout(new BorderLayout(0, 6));
        JPanel textos = new JPanel(new BorderLayout(0, 4));
        textos.setOpaque(false);
        textos.add(lblTitulo, BorderLayout.NORTH);
        textos.add(txtDescripcion, BorderLayout.CENTER);
        textos.add(lblProcedimiento, BorderLayout.SOUTH);
        JPanel filaParametros = new JPanel(new BorderLayout());
        filaParametros.setOpaque(false);
        filaParametros.add(panelParametros, BorderLayout.CENTER);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(btnExportar);
        botones.add(btnEjecutar);
        filaParametros.add(botones, BorderLayout.EAST);
        infoReporte.add(textos, BorderLayout.NORTH);
        infoReporte.add(filaParametros, BorderLayout.SOUTH);
        infoReporte.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE), Estilo.margen(12, 14, 12, 14)));
        JPanel tarjeta = new JPanel(new BorderLayout());
        tarjeta.setBackground(Estilo.BLANCO);
        tarjeta.add(infoReporte);

        TablaUtil.estilizar(tabla);
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setBorder(BorderFactory.createLineBorder(Estilo.BORDE));
        lblResultado.setForeground(Estilo.TEXTO_SUAVE);

        JPanel derecha = new JPanel(new BorderLayout(0, 10));
        derecha.setOpaque(false);
        derecha.add(tarjeta, BorderLayout.NORTH);
        derecha.add(scrollTabla, BorderLayout.CENTER);
        derecha.add(lblResultado, BorderLayout.SOUTH);

        JPanel centro = new JPanel(new BorderLayout(14, 0));
        centro.setOpaque(false);
        centro.add(scrollLista, BorderLayout.WEST);
        centro.add(derecha, BorderLayout.CENTER);

        add(cabecera, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);

        // Eventos
        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarReporte();
            }
        });
        btnEjecutar.addActionListener(e -> ejecutar());
        btnExportar.addActionListener(e -> exportar());
        lista.setSelectedIndex(0);
    }

    /** Muestra la descripción y los campos de parámetros del reporte elegido. */
    private void mostrarReporte() {
        Reporte r = lista.getSelectedValue();
        if (r == null) {
            return;
        }
        lblTitulo.setText(String.format("R%02d. %s", r.getNumero(), r.getTitulo()));
        txtDescripcion.setText("<html><body style='width:500px'>" + r.getDescripcion() + "</body></html>");
        lblProcedimiento.setText("CALL " + r.getProcedimiento() + "(...)" + (r.isUsaJoin() ? "   —   usa JOIN" : ""));
        panelParametros.removeAll();
        camposParametros.clear();
        if (r.getParametros().isEmpty()) {
            panelParametros.add(Estilo.subtitulo("Este reporte no necesita parámetros."));
        }
        for (Reporte.Parametro p : r.getParametros()) {
            JLabel l = new JLabel(p.getEtiqueta() + ":");
            l.setFont(Estilo.fuente(Font.BOLD, 13f));
            JTextField campo = new JTextField(p.getValorInicial());
            campo.setPreferredSize(new Dimension(p.getTipo() == Reporte.TipoParametro.TEXTO ? 170 : 105, 30));
            campo.addActionListener(e -> ejecutar());
            panelParametros.add(l);
            panelParametros.add(campo);
            camposParametros.add(campo);
        }
        panelParametros.revalidate();
        panelParametros.repaint();
        ejecutar();
    }

    /** Ejecuta el procedimiento del reporte con los valores escritos. */
    private void ejecutar() {
        Reporte r = lista.getSelectedValue();
        if (r == null) {
            return;
        }
        List<String> valores = new ArrayList<>();
        for (JTextField campo : camposParametros) {
            valores.add(campo.getText());
        }
        try {
            long inicio = System.currentTimeMillis();
            ModeloTabla resultado = dao.ejecutar(r, valores);
            tabla.setModel(resultado);
            tabla.setAutoCreateRowSorter(true);
            TablaUtil.ajustarColumnas(tabla);
            long tiempo = System.currentTimeMillis() - inicio;
            lblResultado.setText(resultado.getRowCount() + " fila(s)  ·  " + tiempo + " ms  ·  procedimiento "
                    + r.getProcedimiento());
        } catch (IllegalArgumentException e) {
            Mensajes.aviso(this, e.getMessage());
        } catch (Exception e) {
            Mensajes.error(this, e);
        }
    }

    /** Guarda el resultado en un archivo CSV que se puede abrir con Excel. */
    private void exportar() {
        if (tabla.getRowCount() == 0) {
            Mensajes.aviso(this, "No hay datos para exportar.");
            return;
        }
        Reporte r = lista.getSelectedValue();
        JFileChooser selector = new JFileChooser();
        selector.setSelectedFile(new File(String.format("reporte_R%02d.csv", r.getNumero())));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = selector.getSelectedFile();
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(archivo), StandardCharsets.UTF_8))) {
            out.print('﻿');   // marca BOM para que Excel reconozca las tildes
            for (int c = 0; c < tabla.getColumnCount(); c++) {
                out.print((c > 0 ? ";" : "") + tabla.getColumnName(c));
            }
            out.println();
            for (int f = 0; f < tabla.getRowCount(); f++) {
                for (int c = 0; c < tabla.getColumnCount(); c++) {
                    Object v = tabla.getValueAt(f, c);
                    out.print((c > 0 ? ";" : "") + (v == null ? "" : v.toString().replace(";", ",")));
                }
                out.println();
            }
            Mensajes.info(this, "Reporte exportado en:\n" + archivo.getAbsolutePath());
        } catch (IOException e) {
            Mensajes.error(this, e);
        }
    }
}
