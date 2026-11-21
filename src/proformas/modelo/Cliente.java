package proformas.modelo;

/** Cliente del taller: persona natural (DNI) o empresa (RUC). Tabla {@code cliente}. */
public class Cliente {

    private String idCliente;   // DNI de 8 dígitos o RUC de 11 dígitos
    private String nombre;
    private String telefono;

    public Cliente() {
    }

    public Cliente(String idCliente, String nombre, String telefono) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.telefono = telefono;
    }

    public String getIdCliente() { return idCliente; }
    public void setIdCliente(String idCliente) { this.idCliente = idCliente; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    /** @return true si el documento del cliente es un RUC (11 dígitos) */
    public boolean tieneRuc() {
        return idCliente != null && idCliente.length() == 11;
    }

    /** Texto que se muestra en las listas desplegables. */
    @Override
    public String toString() {
        return nombre + "  (" + idCliente + ")";
    }
}
