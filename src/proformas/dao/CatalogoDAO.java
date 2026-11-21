package proformas.dao;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import proformas.conexion.ConexionBD;
import proformas.modelo.ItemCatalogo;

/**
 * Acceso a datos de los catálogos {@code servicio} y {@code repuesto}.
 * Como las dos tablas tienen la misma estructura, una sola clase atiende
 * a ambas: el tipo decide qué procedimientos se llaman
 * (sp_servicio_* o sp_repuesto_*).
 */
public class CatalogoDAO {

    private final ItemCatalogo.Tipo tipo;
    private final String tabla;     // "servicio" o "repuesto"

    public CatalogoDAO(ItemCatalogo.Tipo tipo) {
        this.tipo = tipo;
        this.tabla = (tipo == ItemCatalogo.Tipo.SERVICIO) ? "servicio" : "repuesto";
    }

    /** Lista el catálogo (sp_servicio_listar o sp_repuesto_listar). */
    public List<ItemCatalogo> listar(String filtro) throws SQLException {
        List<ItemCatalogo> lista = new ArrayList<>();
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall("{call sp_" + tabla + "_listar(?)}")) {
            cs.setString(1, filtro);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ItemCatalogo(tipo,
                                               rs.getInt("id_" + tabla),
                                               rs.getString("nombre_" + tabla),
                                               rs.getBigDecimal("precio_unitario")));
                }
            }
        }
        return lista;
    }

    /**
     * Registra un servicio o repuesto.
     *
     * @return código asignado por MySQL (parámetro OUT)
     */
    public int insertar(ItemCatalogo item) throws SQLException {
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall("{call sp_" + tabla + "_insertar(?, ?, ?)}")) {
            cs.setString(1, item.getNombre());
            cs.setBigDecimal(2, item.getPrecioUnitario());
            cs.registerOutParameter(3, Types.INTEGER);
            cs.execute();
            return cs.getInt(3);
        }
    }

    /** Modifica el nombre y el precio. */
    public void actualizar(ItemCatalogo item) throws SQLException {
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall("{call sp_" + tabla + "_actualizar(?, ?, ?)}")) {
            cs.setInt(1, item.getId());
            cs.setString(2, item.getNombre());
            cs.setBigDecimal(3, item.getPrecioUnitario());
            cs.execute();
        }
    }

    /** Elimina un elemento que no figure en ninguna proforma. */
    public void eliminar(int id) throws SQLException {
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall("{call sp_" + tabla + "_eliminar(?)}")) {
            cs.setInt(1, id);
            cs.execute();
        }
    }
}
