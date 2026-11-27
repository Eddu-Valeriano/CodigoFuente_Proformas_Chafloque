package proformas.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 * Colores, fuentes e imágenes comunes a todas las pantallas.
 * Centralizar el estilo aquí mantiene una apariencia uniforme y permite
 * cambiarla desde un solo lugar.
 */
public final class Estilo {

    // Paleta de colores del sistema (azul institucional del logo de ProformaTaller)
    public static final Color AZUL         = new Color(0x2846A8);
    public static final Color AZUL_OSCURO  = new Color(0x17244A);
    public static final Color AZUL_MEDIO   = new Color(0x22346A);
    public static final Color AZUL_CLARO   = new Color(0xE8EEFB);
    public static final Color FONDO        = new Color(0xF3F5F9);
    public static final Color BLANCO       = Color.WHITE;
    public static final Color BORDE        = new Color(0xD5DBE6);
    public static final Color TEXTO        = new Color(0x1F2937);
    public static final Color TEXTO_SUAVE  = new Color(0x6B7280);
    public static final Color ROJO         = new Color(0xC0392B);
    public static final Color VERDE        = new Color(0x1E8449);
    public static final Color NARANJA      = new Color(0xD68910);
    public static final Color FILA_ALTERNA = new Color(0xF7F9FC);

    private Estilo() {
    }

    /**
     * Aplica el aspecto Nimbus (incluido en Java, no es un framework) con
     * los colores del taller. Debe llamarse antes de crear cualquier ventana.
     */
    public static void aplicarTema() {
        try {
            UIManager.put("nimbusBase", AZUL);
            UIManager.put("nimbusBlueGrey", new Color(0xAEB8CC));
            UIManager.put("control", FONDO);
            UIManager.put("nimbusSelectionBackground", AZUL);
            UIManager.put("nimbusFocus", new Color(0x7E97E0));
            UIManager.put("text", TEXTO);
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            UIManager.getLookAndFeelDefaults().put("defaultFont", fuente(Font.PLAIN, 13f));
            UIManager.put("Table.alternateRowColor", FILA_ALTERNA);
            UIManager.put("OptionPane.messageFont", fuente(Font.PLAIN, 13f));
        } catch (Exception e) {
            // Si Nimbus no está disponible se usa el aspecto por defecto de Java.
            System.err.println("No se pudo aplicar el tema: " + e.getMessage());
        }
    }

    /** Familia de letra: Segoe UI (la de Windows) si está instalada; si no, la sans serif del sistema. */
    private static String familia;

    /** Fuente común del aplicativo con el estilo y tamaño indicados. */
    public static Font fuente(int estilo, float tamano) {
        if (familia == null) {
            familia = Font.SANS_SERIF;
            for (String f : java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
                if ("Segoe UI".equals(f)) {
                    familia = f;
                    break;
                }
            }
        }
        return new Font(familia, estilo, Math.round(tamano));
    }

    /** Título grande de una pantalla. */
    public static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(fuente(Font.BOLD, 22f));
        l.setForeground(AZUL_OSCURO);
        return l;
    }

    /** Texto descriptivo bajo el título. */
    public static JLabel subtitulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(fuente(Font.PLAIN, 13f));
        l.setForeground(TEXTO_SUAVE);
        return l;
    }

    /** Margen interno vacío. */
    public static Border margen(int arriba, int izquierda, int abajo, int derecha) {
        return BorderFactory.createEmptyBorder(arriba, izquierda, abajo, derecha);
    }

    /** Borde de tarjeta: línea gris clara con margen interno. */
    public static Border bordeTarjeta(int margenInterno) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDE),
                margen(margenInterno, margenInterno, margenInterno, margenInterno));
    }

    /**
     * Carga una imagen de la carpeta /proformas/recursos y la escala a la altura indicada.
     *
     * @return el ícono o null si la imagen no existe
     */
    public static ImageIcon imagen(String archivo, int alto) {
        URL url = Estilo.class.getResource("/proformas/recursos/" + archivo);
        if (url == null) {
            return null;
        }
        ImageIcon original = new ImageIcon(url);
        int ancho = original.getIconWidth() * alto / original.getIconHeight();
        return new ImageIcon(original.getImage().getScaledInstance(ancho, alto, Image.SCALE_SMOOTH));
    }
}
