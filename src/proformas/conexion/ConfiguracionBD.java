package proformas.conexion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Guarda los datos necesarios para conectarse al servidor MySQL.
 * <p>
 * Los valores se leen del archivo {@code config.properties}, que se ubica
 * en la misma carpeta desde la que se ejecuta el aplicativo. Si el archivo
 * no existe, se usan los valores por defecto del proyecto.
 */
public class ConfiguracionBD {

    /** Nombre del archivo de configuración. */
    public static final String ARCHIVO = "config.properties";

    private String host = "localhost";
    private int puerto = 3306;
    private String baseDatos = "proformataller";
    private String usuario = "proformataller_app";
    private String clave = "ProformaTaller.2026";

    /**
     * Lee la configuración desde {@code config.properties}.
     *
     * @return la configuración leída o, si no hay archivo, la configuración por defecto
     */
    public static ConfiguracionBD cargar() {
        ConfiguracionBD config = new ConfiguracionBD();
        File archivo = new File(ARCHIVO);
        if (!archivo.exists()) {
            return config;
        }
        Properties props = new Properties();
        try (InputStream entrada = new FileInputStream(archivo)) {
            props.load(entrada);
            config.host = props.getProperty("db.host", config.host).trim();
            config.puerto = Integer.parseInt(props.getProperty("db.puerto", "3306").trim());
            config.baseDatos = props.getProperty("db.base", config.baseDatos).trim();
            config.usuario = props.getProperty("db.usuario", config.usuario).trim();
            config.clave = props.getProperty("db.clave", config.clave);
        } catch (IOException | NumberFormatException e) {
            // Si el archivo está dañado se trabaja con los valores por defecto.
            System.err.println("No se pudo leer " + ARCHIVO + ": " + e.getMessage());
        }
        return config;
    }

    /**
     * Guarda la configuración actual en {@code config.properties}.
     *
     * @throws IOException si no se puede escribir el archivo
     */
    public void guardar() throws IOException {
        Properties props = new Properties();
        props.setProperty("db.host", host);
        props.setProperty("db.puerto", String.valueOf(puerto));
        props.setProperty("db.base", baseDatos);
        props.setProperty("db.usuario", usuario);
        props.setProperty("db.clave", clave);
        try (OutputStream salida = new FileOutputStream(ARCHIVO)) {
            props.store(salida, "Conexion a MySQL - ProformaTaller");
        }
    }

    /**
     * Arma la URL JDBC con los parámetros que necesita MySQL 8.
     * <ul>
     *   <li>{@code allowPublicKeyRetrieval}: permite la autenticación caching_sha2_password.</li>
     *   <li>{@code serverTimezone}: zona horaria de Arequipa (UTC-5).</li>
     *   <li>{@code characterEncoding}: tildes y eñes correctas.</li>
     * </ul>
     *
     * @return URL de conexión, por ejemplo {@code jdbc:mysql://localhost:3306/proformataller?...}
     */
    public String getUrl() {
        return "jdbc:mysql://" + host + ":" + puerto + "/" + baseDatos
                + "?useSSL=false&allowPublicKeyRetrieval=true"
                + "&serverTimezone=America/Lima&characterEncoding=UTF-8";
    }

    // ----------------------------------------------------------------- getters y setters

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPuerto() { return puerto; }
    public void setPuerto(int puerto) { this.puerto = puerto; }

    public String getBaseDatos() { return baseDatos; }
    public void setBaseDatos(String baseDatos) { this.baseDatos = baseDatos; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
}
