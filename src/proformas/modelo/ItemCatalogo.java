package proformas.modelo;

import java.math.BigDecimal;

/**
 * Elemento del catálogo del taller: un servicio (tabla {@code servicio})
 * o un repuesto (tabla {@code repuesto}). Ambas tablas tienen la misma
 * estructura: código, nombre y precio unitario.
 */
public class ItemCatalogo {

    /** Tipo de elemento del catálogo. */
    public enum Tipo { SERVICIO, REPUESTO }

    private Tipo tipo;
    private int id;
    private String nombre;
    private BigDecimal precioUnitario;

    public ItemCatalogo() {
    }

    public ItemCatalogo(Tipo tipo, int id, String nombre, BigDecimal precioUnitario) {
        this.tipo = tipo;
        this.id = id;
        this.nombre = nombre;
        this.precioUnitario = precioUnitario;
    }

    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    /** Texto de la lista desplegable: nombre y precio. */
    @Override
    public String toString() {
        return nombre + "  -  S/ " + precioUnitario;
    }
}
