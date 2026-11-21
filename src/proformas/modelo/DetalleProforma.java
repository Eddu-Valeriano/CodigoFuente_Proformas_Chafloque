package proformas.modelo;

import java.math.BigDecimal;

/** Línea de la proforma: un servicio o un repuesto con su cantidad. */
public class DetalleProforma {

    private ItemCatalogo item;
    private int cantidad;

    public DetalleProforma(ItemCatalogo item, int cantidad) {
        this.item = item;
        this.cantidad = cantidad;
    }

    /** @return cantidad multiplicada por el precio unitario */
    public BigDecimal getSubtotal() {
        return item.getPrecioUnitario().multiply(BigDecimal.valueOf(cantidad));
    }

    public ItemCatalogo getItem() { return item; }
    public void setItem(ItemCatalogo item) { this.item = item; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}
