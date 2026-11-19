package com.grupo1.biometria.mapa;

import java.util.Locale;

/**
 * --------------------
 * CD: Categoria = { BUENA, MODERADA, MALA, MUY_MALA, SIN_DATOS }
 *     Paleta = ( categoria: Text --> color: Text, etiqueta: Text, intensidad: R )
 * CC: El color de la API prevalece; si falta se usa UMBRAL (bd/seed.sql:60-76).
 * GR: Clase pura, sin estado y sin dependencias de Android: segura entre hilos.
 * --------------------
 */
public final class Paleta {
    public static final String BUENA = "#2ECC71";
    public static final String MODERADA = "#F1C40F";
    public static final String MALA = "#E67E22";
    public static final String MUY_MALA = "#E74C3C";
    public static final String SIN_DATOS = "#95A5A6";

    public static final String[] CATEGORIAS = { "buena", "moderada", "mala", "muy_mala", "sin_datos" };

    private Paleta() {
    }

    /** -------------------- categoria: Text, colorApi: Text --> color: Text -------------------- */
    public static String color(String categoria, String colorApi) {
        if (colorApi != null && !colorApi.trim().isEmpty()) {
            return colorApi.trim();
        }
        return fallback(categoria);
    }

    /** -------------------- categoria: Text --> color: Text -------------------- */
    public static String fallback(String categoria) {
        switch (normalizar(categoria)) {
            case "buena":
                return BUENA;
            case "moderada":
                return MODERADA;
            case "mala":
                return MALA;
            case "muy_mala":
                return MUY_MALA;
            default:
                return SIN_DATOS;
        }
    }

    /** -------------------- categoria: Text --> etiqueta: Text -------------------- */
    public static String etiqueta(String categoria) {
        switch (normalizar(categoria)) {
            case "buena":
                return "Buena";
            case "moderada":
                return "Moderada";
            case "mala":
                return "Mala";
            case "muy_mala":
                return "Muy mala";
            default:
                return "Sin datos";
        }
    }

    /** -------------------- categoria: Text --> intensidad: R -------------------- */
    public static double intensidad(String categoria) {
        switch (normalizar(categoria)) {
            case "buena":
                return 0.25;
            case "moderada":
                return 0.5;
            case "mala":
                return 0.8;
            case "muy_mala":
                return 1.0;
            default:
                return 0.3;
        }
    }

    /** -------------------- categoria: Text --> categoria_normalizada: Text -------------------- */
    private static String normalizar(String categoria) {
        return categoria == null ? "" : categoria.toLowerCase(Locale.ROOT);
    }
}
