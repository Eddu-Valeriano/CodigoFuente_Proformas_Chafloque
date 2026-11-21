package proformas.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Conversión de fechas entre el formato de pantalla (dd/mm/aaaa) y LocalDate. */
public final class Fechas {

    /** Formato usado en Perú para mostrar y escribir fechas. */
    public static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Fechas() {
    }

    /** @return la fecha como texto dd/mm/aaaa, o vacío si es null */
    public static String formatear(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(FORMATO);
    }

    /**
     * Convierte un texto en fecha. Acepta dd/mm/aaaa y también aaaa-mm-dd.
     *
     * @return la fecha, o null si el texto está vacío
     * @throws IllegalArgumentException si el texto no es una fecha válida
     */
    public static LocalDate parsear(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        String t = texto.trim();
        try {
            return t.contains("-") ? LocalDate.parse(t) : LocalDate.parse(t, FORMATO);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha «" + t + "» no es válida. Use el formato dd/mm/aaaa.");
        }
    }
}
