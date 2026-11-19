package com.grupo1.biometria.mapa.model;

import com.grupo1.biometria.mapa.Paleta;

import java.io.Serializable;

/**
 * --------------------
 * CD: Medicion = ( valor: R, categoria: Text, color: Text, fecha_hora: Text )
 * CC: color se resuelve con Paleta.color; el resto viene de la API.
 * GR: Inmutable; se puede compartir entre hilos sin sincronización.
 * --------------------
 */
public final class Medicion implements Serializable {
    private static final long serialVersionUID = 1L;

    private final double valor;
    private final String categoria;
    private final String color;
    private final String fechaHora;

    /** -------------------- valor: R, categoria: Text, colorApi: Text, fecha: Text --> Medicion -------------------- */
    public Medicion(double valor, String categoria, String colorApi, String fechaHora) {
        this.valor = valor;
        this.categoria = categoria == null ? "sin_datos" : categoria;
        this.color = Paleta.color(this.categoria, colorApi);
        this.fechaHora = fechaHora;
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
