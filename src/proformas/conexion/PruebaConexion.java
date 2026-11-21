package proformas.conexion;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Prueba de conexión por consola (avance del 30 % del aplicativo).
 * <p>
 * Se ejecuta con: {@code java -cp ChafloqueProformas.jar proformas.conexion.PruebaConexion}
 * <ol>
 *   <li>Lee config.properties.</li>
 *   <li>Abre la conexión con MySQL y muestra los datos del servidor.</li>
 *   <li>Llama al procedimiento sp_resumen_general para comprobar que el
 *       usuario del aplicativo puede ejecutar procedimientos.</li>
 * </ol>
 */
public class PruebaConexion {

    public static void main(String[] args) {
        ConfiguracionBD config = ConfiguracionBD.cargar();
        System.out.println("=== PRUEBA DE CONEXION - SISTEMA DE PROFORMAS CHAFLOQUE ===");
        System.out.println("URL            : " + config.getUrl());
        System.out.println("Usuario        : " + config.getUsuario());
        System.out.println();

        try {
            // Paso 1: conexión y datos del servidor
            System.out.println(ConexionBD.probarConexion(config));
            System.out.println();

            // Paso 2: ejecución de un procedimiento almacenado
            Connection cn = ConexionBD.getConexion();
            try (CallableStatement cs = cn.prepareCall("{call sp_resumen_general()}");
                 ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Procedimiento sp_resumen_general():");
                    System.out.println("  Clientes   : " + rs.getInt("clientes"));
                    System.out.println("  Mecanicos  : " + rs.getInt("mecanicos"));
                    System.out.println("  Servicios  : " + rs.getInt("servicios"));
                    System.out.println("  Repuestos  : " + rs.getInt("repuestos"));
                    System.out.println("  Proformas  : " + rs.getInt("proformas"));
                    System.out.println("  Monto total: S/ " + rs.getBigDecimal("monto_total"));
                }
            }
            System.out.println();
            System.out.println("RESULTADO: CONEXION EXITOSA");
        } catch (SQLException e) {
            System.out.println("RESULTADO: ERROR DE CONEXION");
            System.out.println("Codigo " + e.getErrorCode() + " - " + e.getMessage());
            System.exit(1);
        } finally {
            ConexionBD.cerrar();
        }
    }
}
