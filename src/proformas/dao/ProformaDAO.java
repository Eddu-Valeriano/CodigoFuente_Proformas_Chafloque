package proformas.dao;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import proformas.conexion.ConexionBD;
import proformas.modelo.DetalleProforma;
import proformas.modelo.ItemCatalogo;
import proformas.modelo.Proforma;

/**
 * Acceso a datos de la proforma y de su detalle.
 * <p>
 * El registro y la modificación se hacen con un solo procedimiento
 * (sp_proforma_registrar / sp_proforma_actualizar) que recibe la cabecera
 * y el detalle en formato JSON. Ese procedimiento trabaja dentro de una
 * transacción: se guarda todo o no se guarda nada.
 */
public class ProformaDAO {

    /**
     * Lista proformas con filtros opcionales (sp_proforma_listar).
     *
     * @param texto cliente, DNI/RUC, motor o número de proforma (puede ser vacío)
     * @param desde fecha inicial o null
     * @param hasta fecha final o null
     */
    public List<Proforma> listar(String texto, LocalDate desde, LocalDate hasta) throws SQLException {
        List<Proforma> lista = new ArrayList<>();
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall("{call sp_proforma_listar(?, ?, ?)}")) {
            cs.setString(1, texto);
            setFecha(cs, 2, desde);
            setFecha(cs, 3, hasta);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    Proforma p = new Proforma();
                    p.setIdProforma(rs.getInt("id_proforma"));
                    p.setFecha(rs.getDate("fecha").toLocalDate());
                    p.setNombreCliente(rs.getString("nombre_cliente"));
                    p.setNombreMecanico(rs.getString("nombre_mecanico"));
                    p.setMarcaMotor(rs.getString("marca_motor"));
                    p.setTotal(rs.getBigDecimal("total"));
                    lista.add(p);
                }
            }
        }
        return lista;
    }

    /**
     * Obtiene una proforma completa: cabecera (sp_proforma_obtener)
     * y detalle de servicios y repuestos (sp_proforma_detalle).
     *
     * @return la proforma o null si no existe
     */
    public Proforma obtener(int idProforma) throws SQLException {
        Proforma p = null;
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall("{call sp_proforma_obtener(?)}")) {
            cs.setInt(1, idProforma);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    p = new Proforma();
                    p.setIdProforma(rs.getInt("id_proforma"));
                    p.setFecha(rs.getDate("fecha").toLocalDate());
                    p.setIdCliente(rs.getString("id_cliente"));
                    p.setNombreCliente(rs.getString("nombre_cliente"));
                    p.setRuc(rs.getString("ruc"));
                    p.setIdMecanico(rs.getInt("id_mecanico"));
                    p.setNombreMecanico(rs.getString("nombre_mecanico"));
                    p.setMarcaMotor(rs.getString("marca_motor"));
                    p.setTotal(rs.getBigDecimal("total"));
                }
            }
        }
        if (p != null) {
            p.getDetalle().addAll(obtenerDetalle(idProforma));
        }
        return p;
    }

    /** Líneas de servicios y repuestos de una proforma (sp_proforma_detalle). */
    public List<DetalleProforma> obtenerDetalle(int idProforma) throws SQLException {
        List<DetalleProforma> lineas = new ArrayList<>();
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall("{call sp_proforma_detalle(?)}")) {
            cs.setInt(1, idProforma);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    ItemCatalogo.Tipo tipo = "Servicio".equals(rs.getString("tipo"))
                            ? ItemCatalogo.Tipo.SERVICIO : ItemCatalogo.Tipo.REPUESTO;
                    ItemCatalogo item = new ItemCatalogo(tipo, rs.getInt("id_item"),
                            rs.getString("descripcion"), rs.getBigDecimal("precio_unitario"));
                    lineas.add(new DetalleProforma(item, rs.getInt("cantidad")));
                }
            }
        }
        return lineas;
    }

    /**
     * Registra una proforma nueva con su detalle (sp_proforma_registrar).
     *
     * @return número de proforma asignado (parámetro OUT)
     */
    public int registrar(Proforma p) throws SQLException {
        String sql = "{call sp_proforma_registrar(?, ?, ?, ?, ?, ?, ?, ?)}";
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall(sql)) {
            cs.setString(1, p.getIdCliente());
            cs.setInt(2, p.getIdMecanico());
            cs.setString(3, p.getMarcaMotor());
            setFecha(cs, 4, p.getFecha());
            cs.setString(5, vacioANulo(p.getRuc()));
            cs.setString(6, detalleJson(p, ItemCatalogo.Tipo.SERVICIO));
            cs.setString(7, detalleJson(p, ItemCatalogo.Tipo.REPUESTO));
            cs.registerOutParameter(8, Types.INTEGER);
            cs.execute();
            return cs.getInt(8);
        }
    }

    /** Modifica la cabecera y sincroniza el detalle (sp_proforma_actualizar). */
    public void actualizar(Proforma p) throws SQLException {
        String sql = "{call sp_proforma_actualizar(?, ?, ?, ?, ?, ?, ?, ?)}";
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall(sql)) {
            cs.setInt(1, p.getIdProforma());
            cs.setString(2, p.getIdCliente());
            cs.setInt(3, p.getIdMecanico());
            cs.setString(4, p.getMarcaMotor());
            setFecha(cs, 5, p.getFecha());
            cs.setString(6, vacioANulo(p.getRuc()));
            cs.setString(7, detalleJson(p, ItemCatalogo.Tipo.SERVICIO));
            cs.setString(8, detalleJson(p, ItemCatalogo.Tipo.REPUESTO));
            cs.execute();
        }
    }

    /** Elimina una proforma; su detalle se borra en cascada (sp_proforma_eliminar). */
    public void eliminar(int idProforma) throws SQLException {
        try (CallableStatement cs = ConexionBD.getConexion().prepareCall("{call sp_proforma_eliminar(?)}")) {
            cs.setInt(1, idProforma);
            cs.execute();
        }
    }

    // ------------------------------------------------------------------ auxiliares

    /**
     * Convierte las líneas de un tipo en un arreglo JSON que entiende el
     * procedimiento, por ejemplo: [{"id":2,"cantidad":1},{"id":3,"cantidad":8}].
     * Solo contiene números, por eso se arma sin librerías externas.
     */
    private String detalleJson(Proforma p, ItemCatalogo.Tipo tipo) {
        StringBuilder json = new StringBuilder("[");
        for (DetalleProforma d : p.getDetalle()) {
            if (d.getItem().getTipo() != tipo) {
                continue;
            }
            if (json.length() > 1) {
                json.append(',');
            }
            json.append("{\"id\":").append(d.getItem().getId())
                .append(",\"cantidad\":").append(d.getCantidad()).append('}');
        }
        return json.append(']').toString();
    }

    /**
     * Envía una fecha o NULL al procedimiento. La fecha se envía como texto
     * aaaa-mm-dd, que MySQL convierte a DATE; así funciona igual con
     * cualquier controlador JDBC.
     */
    private void setFecha(CallableStatement cs, int indice, LocalDate fecha) throws SQLException {
        if (fecha == null) {
            cs.setNull(indice, Types.VARCHAR);
        } else {
            cs.setString(indice, fecha.toString());
        }
    }

    /** Un texto vacío se guarda como NULL. */
    private String vacioANulo(String texto) {
        return (texto == null || texto.trim().isEmpty()) ? null : texto.trim();
    }
}
