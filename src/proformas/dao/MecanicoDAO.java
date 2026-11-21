package proformas.dao;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import proformas.conexion.ConexionBD;
import proformas.modelo.Mecanico;

/** Acceso a datos de la tabla {@code mecanico} mediante procedimientos almacenados. */
public class MecanicoDAO {

    /** Lista los mecánicos (sp_mecanico_listar). */
    public List<Mecanico> listar(String filtro) throws SQLException {
        List<Mecanico> lista = new ArrayList<>();
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall("{call sp_mecanico_listar(?)}")) {
            cs.setString(1, filtro);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Mecanico(rs.getInt("id_mecanico"), rs.getString("nombre_mecanico")));
                }
            }
        }
        return lista;
    }

    /**
     * Registra un mecánico (sp_mecanico_insertar).
     *
     * @return código autoincremental asignado por MySQL (parámetro OUT)
     */
    public int insertar(Mecanico m) throws SQLException {
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall("{call sp_mecanico_insertar(?, ?)}")) {
            cs.setString(1, m.getNombre());
            cs.registerOutParameter(2, Types.INTEGER);
            cs.execute();
            return cs.getInt(2);
        }
    }

    /** Modifica el nombre de un mecánico (sp_mecanico_actualizar). */
    public void actualizar(Mecanico m) throws SQLException {
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall("{call sp_mecanico_actualizar(?, ?)}")) {
            cs.setInt(1, m.getIdMecanico());
            cs.setString(2, m.getNombre());
            cs.execute();
        }
    }

    /** Elimina un mecánico sin proformas (sp_mecanico_eliminar). */
    public void eliminar(int idMecanico) throws SQLException {
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall("{call sp_mecanico_eliminar(?)}")) {
            cs.setInt(1, idMecanico);
            cs.execute();
        }
    }
}
