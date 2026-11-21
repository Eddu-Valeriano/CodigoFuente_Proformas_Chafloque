package proformas.vista;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JButton;

import proformas.util.Estilo;

/**
 * Botón plano con esquinas redondeadas, dibujado con Java 2D.
 * Se usa en todo el aplicativo para que los botones luzcan igual en
 * cualquier sistema operativo.
 */
public class BotonPlano extends JButton {

    private final Color fondo;
    private final Color borde;

    /**
     * @param texto  texto del botón
     * @param fondo  color de fondo
     * @param letra  color del texto
     * @param borde  color del borde (null si no tiene)
     */
    public BotonPlano(String texto, Color fondo, Color letra, Color borde) {
        super(texto);
        this.fondo = fondo;
        this.borde = borde;
        setForeground(letra);
        setFont(Estilo.fuente(Font.BOLD, 13f));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new java.awt.Insets(0, 0, 0, 0));
    }

    /** Botón azul para la acción principal (Guardar, Nuevo, Ejecutar). */
    public static BotonPlano primario(String texto) {
        return new BotonPlano(texto, Estilo.AZUL, Color.WHITE, null);
    }

    /** Botón rojo para acciones que borran datos. */
    public static BotonPlano peligro(String texto) {
        return new BotonPlano(texto, Estilo.ROJO, Color.WHITE, null);
    }

    /** Botón verde para confirmar. */
    public static BotonPlano exito(String texto) {
        return new BotonPlano(texto, Estilo.VERDE, Color.WHITE, null);
    }

    /** Botón blanco con borde para acciones secundarias. */
    public static BotonPlano secundario(String texto) {
        return new BotonPlano(texto, Color.WHITE, Estilo.TEXTO, Estilo.BORDE);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        int ancho = getFontMetrics(getFont()).stringWidth(getText()) + 36;
        return new Dimension(Math.max(ancho, 90), 34);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color color = fondo;
        if (!isEnabled()) {
            color = new Color(0xC9CED8);
        } else if (getModel().isPressed()) {
            color = fondo.darker();
        } else if (getModel().isRollover()) {
            color = (fondo.equals(Color.WHITE)) ? Estilo.AZUL_CLARO : mezclar(fondo, Color.BLACK, 0.12f);
        }
        g2.setColor(color);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        if (borde != null) {
            g2.setColor(borde);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        }
        g2.dispose();
        super.paintComponent(g);
    }

    /** Mezcla dos colores; se usa para el efecto al pasar el mouse. */
    private static Color mezclar(Color a, Color b, float proporcion) {
        float p = 1 - proporcion;
        return new Color((int) (a.getRed() * p + b.getRed() * proporcion),
                         (int) (a.getGreen() * p + b.getGreen() * proporcion),
                         (int) (a.getBlue() * p + b.getBlue() * proporcion));
    }
}
