package proformas.vista;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.SwingConstants;

import proformas.util.Estilo;

/** Opción del menú lateral. Resalta la pantalla que está abierta. */
public class BotonMenu extends JButton {

    private boolean seleccionado;

    public BotonMenu(String texto) {
        super(texto);
        setForeground(new Color(0xDCE3F5));
        setFont(Estilo.fuente(Font.PLAIN, 14f));
        setHorizontalAlignment(SwingConstants.LEFT);
        setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 10));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        setPreferredSize(new Dimension(210, 44));
        setAlignmentX(LEFT_ALIGNMENT);
    }

    /** Marca o desmarca la opción como pantalla activa. */
    public void setSeleccionado(boolean seleccionado) {
        this.seleccionado = seleccionado;
        setForeground(seleccionado ? Color.WHITE : new Color(0xDCE3F5));
        setFont(Estilo.fuente(seleccionado ? Font.BOLD : Font.PLAIN, 14f));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (seleccionado) {
            g2.setColor(Estilo.AZUL);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, 4, getHeight());
        } else if (getModel().isRollover()) {
            g2.setColor(Estilo.AZUL_MEDIO);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
