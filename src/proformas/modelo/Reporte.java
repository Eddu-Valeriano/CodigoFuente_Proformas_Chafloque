package proformas.modelo;

import java.util.Arrays;
import java.util.List;

/**
 * Describe uno de los reportes del aplicativo: qué procedimiento almacenado
 * lo genera, qué parámetros pide y si utiliza JOIN entre tablas.
 */
public class Reporte {

    /** Tipos de dato que puede tener un parámetro. */
    public enum TipoParametro { TEXTO, ENTERO, FECHA }

    /** Parámetro de entrada de un reporte. */
    public static class Parametro {
        private final String etiqueta;
        private final TipoParametro tipo;
        private final String valorInicial;

        public Parametro(String etiqueta, TipoParametro tipo, String valorInicial) {
            this.etiqueta = etiqueta;
            this.tipo = tipo;
            this.valorInicial = valorInicial;
        }

        public String getEtiqueta() { return etiqueta; }
        public TipoParametro getTipo() { return tipo; }
        public String getValorInicial() { return valorInicial; }
    }

    private final int numero;
    private final String titulo;
    private final String descripcion;
    private final String procedimiento;
    private final boolean usaJoin;
    private final List<Parametro> parametros;

    public Reporte(int numero, String titulo, String descripcion, String procedimiento,
                   boolean usaJoin, Parametro... parametros) {
        this.numero = numero;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.procedimiento = procedimiento;
        this.usaJoin = usaJoin;
        this.parametros = Arrays.asList(parametros);
    }

    /** @return llamada JDBC, por ejemplo {call sp_rep05_proformas_periodo(?, ?)} */
    public String getLlamada() {
        StringBuilder sb = new StringBuilder("{call ").append(procedimiento).append("(");
        for (int i = 0; i < parametros.size(); i++) {
            sb.append(i == 0 ? "?" : ", ?");
        }
        return sb.append(")}").toString();
    }

    public int getNumero() { return numero; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public String getProcedimiento() { return procedimiento; }
    public boolean isUsaJoin() { return usaJoin; }
    public List<Parametro> getParametros() { return parametros; }

    @Override
    public String toString() {
        return String.format("R%02d  %s", numero, titulo);
    }
}
