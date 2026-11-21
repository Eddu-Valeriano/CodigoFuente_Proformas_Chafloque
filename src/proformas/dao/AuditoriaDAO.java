package proformas.dao;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import proformas.conexion.ConexionBD;
import proformas.util.TablaUtil;
import proformas.util.TablaUtil.ModeloTabla;

/** Consulta la tabla {@code auditoria}, que llenan automáticamente los triggers. */
public class AuditoriaDAO {

    /**
     * Últimos movimientos registrados (sp_auditoria_listar).
     *
     * @param tabla nombre de la tabla o vacío para todas
     */
    public ModeloTabla listar(String tabla) throws SQLException {
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall("{call sp_auditoria_listar(?)}")) {
            cs.setString(1, tabla);
            try (ResultSet rs = cs.executeQuery()) {
                return TablaUtil.desdeResultSet(rs);
            }
        }
    }
}
