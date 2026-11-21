package proformas.dao;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import proformas.conexion.ConexionBD;
import proformas.modelo.Cliente;

/**
 * Acceso a datos de la tabla {@code cliente}.
 * Cada método llama a un procedimiento almacenado; el aplicativo no
 * ejecuta sentencias INSERT, UPDATE ni DELETE directamente.
 */
public class ClienteDAO {

    /**
     * Lista los clientes (procedimiento sp_cliente_listar).
     *
     * @param filtro parte del nombre o inicio del DNI/RUC; vacío para traer todos
     */
    public List<Cliente> listar(String filtro) throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall("{call sp_cliente_listar(?)}")) {
            cs.setString(1, filtro);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Cliente(rs.getString("id_cliente"),
                                          rs.getString("nombre_cliente"),
                                          rs.getString("telefono")));
                }
            }
        }
        return lista;
    }

    /** Registra un cliente nuevo (sp_cliente_insertar). */
    public void insertar(Cliente c) throws SQLException {
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall("{call sp_cliente_insertar(?, ?, ?)}")) {
            cs.setString(1, c.getIdCliente());
            cs.setString(2, c.getNombre());
            cs.setString(3, c.getTelefono());
            cs.execute();
        }
    }

    /** Modifica el nombre y el teléfono de un cliente (sp_cliente_actualizar). */
    public void actualizar(Cliente c) throws SQLException {
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall("{call sp_cliente_actualizar(?, ?, ?)}")) {
            cs.setString(1, c.getIdCliente());
            cs.setString(2, c.getNombre());
            cs.setString(3, c.getTelefono());
            cs.execute();
        }
    }

    /** Elimina un cliente sin proformas (sp_cliente_eliminar). */
    public void eliminar(String idCliente) throws SQLException {
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall("{call sp_cliente_eliminar(?)}")) {
            cs.setString(1, idCliente);
            cs.execute();
        }
    }
}
