package proformas.util;

import java.awt.Component;
import java.sql.SQLException;

import javax.swing.JOptionPane;

/**
 * Muestra mensajes al usuario y traduce los errores de MySQL a un lenguaje
 * claro. Los errores de las reglas de negocio llegan con SQLSTATE 45000
 * desde los triggers y procedimientos (SIGNAL), y se muestran tal cual.
 */
public final class Mensajes {

    private Mensajes() {
    }

    /** Muestra un mensaje informativo. */
    public static void info(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "ProformaTaller", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Muestra un aviso de validación. */
    public static void aviso(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Revise los datos", JOptionPane.WARNING_MESSAGE);
    }

    /** Muestra un error ya traducido. */
    public static void error(Component padre, Exception e) {
        JOptionPane.showMessageDialog(padre, traducir(e), "No se pudo completar la operación",
                JOptionPane.ERROR_MESSAGE);
    }

    /** Pide confirmación (Sí / No). */
    public static boolean confirmar(Component padre, String mensaje) {
        return JOptionPane.showConfirmDialog(padre, mensaje, "Confirmar", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /**
     * Convierte una excepción en un mensaje comprensible.
     *
     * @param e excepción producida
     * @return texto para el usuario
     */
    public static String traducir(Exception e) {
        if (!(e instanceof SQLException)) {
            return e.getMessage();
        }
        SQLException s = (SQLException) e;
        // El controlador antepone "(conn=N)" al mensaje del servidor; se quita.
        String mensaje = String.valueOf(s.getMessage()).replaceFirst("^\\(conn[=:]\\d+\\)\\s*", "");
        String estado = s.getSQLState() == null ? "" : s.getSQLState();

        if ("45000".equals(estado)) {
            return mensaje;                         // regla de negocio (SIGNAL)
        }
        switch (s.getErrorCode()) {
            case 1045: return "Usuario o contraseña de MySQL incorrectos.";
            case 1049: return "La base de datos no existe. Ejecute primero los scripts de la carpeta sql.";
            case 1062: return "Ya existe un registro con ese valor.";
            case 1142:
            case 1370: return "El usuario de MySQL no tiene permiso para esta operación.";
            case 1451: return "No se puede eliminar porque tiene registros relacionados.";
            case 1452: return "Uno de los datos relacionados (cliente, mecánico, servicio o repuesto) no existe.";
            case 3819: return "Un dato no cumple las reglas de la base de datos.\n" + mensaje;
            default:   break;
        }
        if (estado.startsWith("08")) {
            return "No se pudo conectar con el servidor MySQL.\n"
                 + "Verifique que el servicio MySQL esté iniciado y que el host y el puerto sean correctos.";
        }
        return mensaje;
    }
}
