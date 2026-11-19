package com.grupo1.biometria.mapa.model;

import com.grupo1.biometria.mapa.Paleta;

import java.io.Serializable;

/**
 * --------------------
 * CD: PuntoMapa = ( id_nodo: N, nombre: Text, latitud: R, longitud: R,
 *                   contaminante: Text, unidad: Text, valor: R, categoria: Text,
 *                   color: Text, fecha_hora: Text )
 * CC: Un punto corresponde a la última medición de un nodo activo (GET /api/mapa).
 * GR: Inmutable; la UI solo lo lee.
 * --------------------
 */
public final class PuntoMapa implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int idNodo;
    private final String nombre;
    private final double latitud;
    private final double longitud;
    private final String contaminante;
    private final String unidad;
    private final double valor;
    private final String categoria;
    private final String color;
    private final String fechaHora;

    /** -------------------- datos API --> PuntoMapa -------------------- */
    public PuntoMapa(int idNodo, String nombre, double latitud, double longitud, String contaminante,
                     String unidad, double valor, String categoria, String colorApi, String fechaHora) {
        this.idNodo = idNodo;
        this.nombre = nombre;
        this.latitud = latitud;
        this.longitud = longitud;
        this.contaminante = contaminante;
        this.unidad = unidad;
        this.valor = valor;
        this.categoria = categoria == null ? "sin_datos" : categoria;
        this.color = Paleta.color(this.categoria, colorApi);
        this.fechaHora = fechaHora;
    }

    public int getIdNodo() {
        return idNodo;
    }

    public String getNombre() {
        return nombre;
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public String getContaminante() {
        return contaminante;
    }

    public String getUnidad() {
        return unidad;
    }

    public double getValor() {
        return valor;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getColor() {
        return color;
    }

    public String getFechaHora() {
        return fechaHora;
    }
}
