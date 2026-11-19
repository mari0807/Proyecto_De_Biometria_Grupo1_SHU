package com.queaire.app;

import com.queaire.app.model.Medicion;
import com.queaire.app.model.PuntoMapa;
import com.queaire.app.model.ZonaDetalle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * --------------------
 * CD: MapaJson = ( json: Text --> [ PuntoMapa ], json: Text --> ZonaDetalle )
 * CC: Espejo del contrato de ApiMapa.php:44-80; ok=false se traduce a mensaje.
 * GR: Sin estado; se ejecuta en el hilo de red.
 * --------------------
 */
public final class MapaJson {
    private MapaJson() {
    }

    /** -------------------- json: Text --> ok: B -------------------- */
    public static boolean ok(String json) {
        try {
            return new JSONObject(json).optBoolean("ok", false);
        } catch (JSONException e) {
            return false;
        }
    }

    /** -------------------- json: Text --> mensaje: Text -------------------- */
    public static String mensaje(String json, String porDefecto) {
        try {
            return new JSONObject(json).optString("mensaje", porDefecto);
        } catch (JSONException e) {
            return porDefecto;
        }
    }

    /** -------------------- json: Text --> puntos: [ PuntoMapa ] -------------------- */
    public static List<PuntoMapa> puntos(String json) throws JSONException {
        JSONArray array = new JSONObject(json).optJSONArray("puntos");
        List<PuntoMapa> puntos = new ArrayList<>();
        if (array == null) {
            return puntos;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            puntos.add(new PuntoMapa(
                    item.optInt("id_nodo"),
                    item.optString("nombre", ""),
                    item.optDouble("latitud", 0),
                    item.optDouble("longitud", 0),
                    item.optString("contaminante", ""),
                    item.optString("unidad", ""),
                    item.optDouble("valor", 0),
                    item.optString("categoria", "sin_datos"),
                    item.optString("color", ""),
                    item.optString("fecha_hora", "")
            ));
        }
        return puntos;
    }

    /** -------------------- json: Text --> zona: ZonaDetalle -------------------- */
    public static ZonaDetalle zona(String json) throws JSONException {
        JSONObject obj = new JSONObject(json).optJSONObject("zona");
        if (obj == null) {
            throw new JSONException("La respuesta no contiene la zona.");
        }
        JSONObject actualObj = obj.optJSONObject("actual");
        Medicion actual = actualObj == null ? null : medicion(actualObj);

        List<Medicion> historial = new ArrayList<>();
        JSONArray array = obj.optJSONArray("historial");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                historial.add(medicion(array.getJSONObject(i)));
            }
        }

        return new ZonaDetalle(
                obj.optInt("id_nodo"),
                obj.optString("nombre", ""),
                obj.optString("estado", ""),
                obj.optString("contaminante", ""),
                obj.optString("unidad", ""),
                obj.optDouble("latitud", 0),
                obj.optDouble("longitud", 0),
                actual,
                historial
        );
    }

    /** -------------------- json: Medicion --> medicion: Medicion -------------------- */
    private static Medicion medicion(JSONObject obj) {
        return new Medicion(
                obj.optDouble("valor", 0),
                obj.optString("categoria", "sin_datos"),
                obj.optString("color", ""),
                obj.optString("fecha_hora", "")
        );
    }
}
