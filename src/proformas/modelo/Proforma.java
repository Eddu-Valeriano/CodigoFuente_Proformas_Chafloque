package proformas.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Cabecera de la proforma (tabla {@code proforma}) con su lista de líneas
 * de detalle (tablas {@code servicio_proforma} y {@code repuesto_proforma}).
 */
public class Proforma {

    private int idProforma;
    private String idCliente;
    private String nombreCliente;
    private int idMecanico;
    private String nombreMecanico;
    private String marcaMotor;
    private LocalDate fecha;
    private String ruc;                 // opcional
    private BigDecimal total = BigDecimal.ZERO;   // lo calcula la base de datos
    private final List<DetalleProforma> detalle = new ArrayList<>();

    /** @return número de proforma con seis dígitos, por ejemplo 000021 */
    public String getNumero() {
        return String.format("%06d", idProforma);
    }

    /**
     * Suma los subtotales de las líneas. Sirve como vista previa en pantalla;
     * el total oficial lo guardan los triggers de la base de datos.
     */
    public BigDecimal calcularTotal() {
        BigDecimal suma = BigDecimal.ZERO;
        for (DetalleProforma d : detalle) {
            suma = suma.add(d.getSubtotal());
        }
        return suma;
    }

    public int getIdProforma() { return idProforma; }
    public void setIdProforma(int idProforma) { this.idProforma = idProforma; }

    public String getIdCliente() { return idCliente; }
    public void setIdCliente(String idCliente) { this.idCliente = idCliente; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public int getIdMecanico() { return idMecanico; }
    public void setIdMecanico(int idMecanico) { this.idMecanico = idMecanico; }

    public String getNombreMecanico() { return nombreMecanico; }
    public void setNombreMecanico(String nombreMecanico) { this.nombreMecanico = nombreMecanico; }

    public String getMarcaMotor() { return marcaMotor; }
    public void setMarcaMotor(String marcaMotor) { this.marcaMotor = marcaMotor; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public List<DetalleProforma> getDetalle() { return detalle; }
}
