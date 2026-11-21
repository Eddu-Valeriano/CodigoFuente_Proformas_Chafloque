package proformas.modelo;

/** Mecánico responsable de evaluar el motor y elaborar la proforma. Tabla {@code mecanico}. */
public class Mecanico {

    private int idMecanico;     // autoincremental
    private String nombre;

    public Mecanico() {
    }

    public Mecanico(int idMecanico, String nombre) {
        this.idMecanico = idMecanico;
        this.nombre = nombre;
    }

    public int getIdMecanico() { return idMecanico; }
    public void setIdMecanico(int idMecanico) { this.idMecanico = idMecanico; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    @Override
    public String toString() {
        return nombre;
    }
}
