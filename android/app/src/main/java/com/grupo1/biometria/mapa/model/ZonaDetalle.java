package com.grupo1.biometria.mapa.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * --------------------
 * CD: ZonaDetalle = ( id_nodo: N, nombre: Text, estado: Text, contaminante: Text,
 *                     unidad: Text, latitud: R, longitud: R, actual: Medicion | NULL,
 *                     historial: [ Medicion ] )
 * CC: actual puede ser NULL cuando el nodo no tiene mediciones (ApiMapa.php:78-86).
 * GR: Copia defensiva del historial; inmutable tras construirse.
 * --------------------
 */
public final class ZonaDetalle implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int idNodo;
    private final String nombre;
    private final String estado;
    private final String contaminante;
    private final String unidad;
    private final double latitud;
    private final double longitud;
    private final Medicion actual;
    private final List<Medicion> historial;

    /** -------------------- datos API --> ZonaDetalle -------------------- */
    public ZonaDetalle(int idNodo, String nombre, String estado, String contaminante, String unidad,
                       double latitud, double longitud, Medicion actual, List<Medicion> historial) {
        this.idNodo = idNodo;
        this.nombre = nombre;
        this.estado = estado;
        this.contaminante = contaminante;
        this.unidad = unidad;
        this.latitud = latitud;
        this.longitud = longitud;
        this.actual = actual;
        this.historial = historial == null ? new ArrayList<Medicion>() : new ArrayList<>(historial);
    }

    public int getIdNodo() {
        return idNodo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEstado() {
        return estado;
    }

    public String getContaminante() {
        return contaminante;
    }

    public String getUnidad() {
        return unidad;
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public Medicion getActual() {
        return actual;
    }

    public List<Medicion> getHistorial() {
        return new ArrayList<>(historial);
    }
}
