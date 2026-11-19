package com.grupo1.biometria.mapa;

import com.grupo1.biometria.mapa.model.PuntoMapa;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * --------------------
 * CD: Formato = ( valor: R --> Text, fecha: Text --> Text )
 * CC: La API devuelve fechas UTC "yyyy-MM-dd HH:mm:ss"; se muestra como en la web.
 * GR: Sin estado; todos los formateadores se crean por llamada (seguros entre hilos).
 * --------------------
 */
public final class Formato {
    private Formato() {
    }

    /** -------------------- valor: R --> texto: Text -------------------- */
    public static String valor(double valor) {
        if (!Double.isFinite(valor)) {
            return "—";
        }
        if (valor == Math.rint(valor)) {
            return String.format(Locale.US, "%.0f", valor);
        }
        return String.format(Locale.US, "%.2f", valor);
    }

    /** -------------------- fechaApi: Text --> fechaLegible: Text -------------------- */
    public static String fecha(String fechaApi) {
        Date fecha = parsear(fechaApi);
        if (fecha == null) {
            return fechaApi == null || fechaApi.isEmpty() ? "—" : fechaApi;
        }
        SimpleDateFormat salida = new SimpleDateFormat("dd/MM/yyyy HH:mm 'UTC'", Locale.US);
        salida.setTimeZone(TimeZone.getTimeZone("UTC"));
        return salida.format(fecha);
    }

    /** -------------------- fechaApi: Text --> fechaCorta: Text -------------------- */
    public static String fechaCorta(String fechaApi) {
        Date fecha = parsear(fechaApi);
        if (fecha == null) {
            return "";
        }
        SimpleDateFormat salida = new SimpleDateFormat("dd/MM HH:mm", Locale.US);
        salida.setTimeZone(TimeZone.getTimeZone("UTC"));
        return salida.format(fecha);
    }

    /** -------------------- puntos: [ PuntoMapa ] --> fechaMaxima: Text -------------------- */
    public static String maximaFecha(List<PuntoMapa> puntos) {
        String maxima = null;
        if (puntos != null) {
            for (PuntoMapa punto : puntos) {
                String fecha = punto.getFechaHora();
                if (fecha != null && (maxima == null || fecha.compareTo(maxima) > 0)) {
                    maxima = fecha;
                }
            }
        }
        return maxima;
    }

    /** -------------------- fechaApi: Text --> fecha: Date | NULL -------------------- */
    private static Date parsear(String fechaApi) {
        if (fechaApi == null || fechaApi.length() < 16) {
            return null;
        }
        String normalizada = fechaApi.substring(0, 16).replace('T', ' ');
        SimpleDateFormat entrada = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        entrada.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return entrada.parse(normalizada);
        } catch (ParseException e) {
            return null;
        }
    }
}
