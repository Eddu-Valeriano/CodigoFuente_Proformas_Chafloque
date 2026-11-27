package proformas.conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Administra la conexión del aplicativo con la base de datos proformataller.
 * <p>
 * Se mantiene una sola conexión abierta mientras el programa está en uso
 * (patrón Singleton). Si la conexión se pierde, por ejemplo porque el
 * servidor se reinició, se vuelve a abrir automáticamente en la siguiente
 * solicitud.
 * <p>
 * El aplicativo se conecta con el usuario {@code proformataller_app}, que solo
 * tiene permiso de EJECUTAR procedimientos almacenados. Por eso todas las
 * operaciones pasan por los procedimientos de la base de datos.
 */
public final class ConexionBD {

    /** Segundos de espera máximos al intentar conectar. */
    private static final int TIEMPO_ESPERA = 5;

    /** Conexión compartida por todo el aplicativo. */
    private static Connection conexion;

    /** Datos de conexión en uso. */
    private static ConfiguracionBD configuracion = ConfiguracionBD.cargar();

    /** Clase de utilidad: no se crean objetos de ella. */
    private ConexionBD() {
    }

    /**
     * Devuelve la conexión abierta; si no existe o dejó de ser válida, la abre.
     *
     * @return conexión lista para usarse
     * @throws SQLException si el servidor no responde o las credenciales son incorrectas
     */
    public static synchronized Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed() || !conexion.isValid(TIEMPO_ESPERA)) {
            conexion = abrir(configuracion);
        }
        return conexion;
    }

    /**
     * Abre una conexión nueva con los datos indicados.
     *
     * @param config host, puerto, base de datos, usuario y clave
     * @return conexión abierta
     * @throws SQLException si no se puede conectar
     */
    private static Connection abrir(ConfiguracionBD config) throws SQLException {
        DriverManager.setLoginTimeout(TIEMPO_ESPERA);
        return DriverManager.getConnection(config.getUrl(), config.getUsuario(), config.getClave());
    }

    /**
     * Prueba una configuración sin modificar la conexión en uso.
     * Abre una conexión temporal, consulta los datos del servidor y la cierra.
     *
     * @param config configuración que se desea probar
     * @return texto con la versión de MySQL, la base de datos, el usuario y la hora del servidor
     * @throws SQLException si la conexión falla
     */
    public static String probarConexion(ConfiguracionBD config) throws SQLException {
        long inicio = System.currentTimeMillis();
        try (Connection prueba = abrir(config);
             Statement st = prueba.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT VERSION(), DATABASE(), USER(), DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')")) {
            rs.next();
            long tiempo = System.currentTimeMillis() - inicio;
            return "Servidor MySQL : " + rs.getString(1) + "\n"
                 + "Base de datos  : " + rs.getString(2) + "\n"
                 + "Usuario        : " + rs.getString(3) + "\n"
                 + "Hora servidor  : " + rs.getString(4) + "\n"
                 + "Tiempo         : " + tiempo + " ms";
        }
    }

    /**
     * Cambia la configuración en uso y cierra la conexión anterior.
     * La siguiente llamada a {@link #getConexion()} usará los nuevos datos.
     *
     * @param nueva configuración que se usará desde ahora
     */
    public static synchronized void setConfiguracion(ConfiguracionBD nueva) {
        configuracion = nueva;
        cerrar();
    }

    /** @return configuración en uso */
    public static ConfiguracionBD getConfiguracion() {
        return configuracion;
    }

    /** Cierra la conexión compartida, si está abierta. Se llama al salir del programa. */
    public static synchronized void cerrar() {
        if (conexion != null) {
            try {
                conexion.close();
            } catch (SQLException e) {
                System.err.println("Error al cerrar la conexión: " + e.getMessage());
            }
            conexion = null;
        }
    }
}
