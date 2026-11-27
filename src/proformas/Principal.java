package proformas;

import java.util.Locale;

import javax.swing.SwingUtilities;

import proformas.conexion.ConfiguracionBD;
import proformas.util.Estilo;
import proformas.vista.DialogoConexion;
import proformas.vista.VentanaPrincipal;

/**
 * Punto de entrada del sistema ProformaTaller (Sistema de Proformas para Talleres)
 * <p>
 * Aplicativo de escritorio en Java SE (Swing + JDBC), sin frameworks.
 * Flujo de inicio:
 * <ol>
 *   <li>Se aplica el tema visual.</li>
 *   <li>Se muestra la ventana de conexión con los datos de config.properties.</li>
 *   <li>Si la conexión es correcta, se abre la ventana principal.</li>
 * </ol>
 */
public class Principal {

    public static void main(String[] args) {
        // Textos estándar de Java (Aceptar, Cancelar, Sí, No) en español.
        Locale.setDefault(new Locale("es", "PE"));
        // Swing debe usarse desde su propio hilo (Event Dispatch Thread).
        SwingUtilities.invokeLater(() -> {
            Estilo.aplicarTema();
            DialogoConexion conexion = new DialogoConexion(null, ConfiguracionBD.cargar());
            if (conexion.mostrar()) {
                new VentanaPrincipal().setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }
}
