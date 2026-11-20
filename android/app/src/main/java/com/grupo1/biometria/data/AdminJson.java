package com.grupo1.biometria.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * --------------------
 * CD: AdminJson = ( json: Text --> [ UsuarioAdmin ], json: Text --> password: Text )
 * CC: Espejo del contrato de ApiAuth.php (listar/crear/actualizar/borrar/password);
 *     `ok=false` se traduce a mensaje.
 * GR: Sin estado; se ejecuta en el hilo de red.
 * --------------------
 */
public final class AdminJson {
    private AdminJson() {
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

    /** -------------------- json: Text --> usuarios: [ UsuarioAdmin ] -------------------- */
    public static List<UsuarioAdmin> usuarios(String json) throws JSONException {
        JSONArray array = new JSONObject(json).optJSONArray("usuarios");
        List<UsuarioAdmin> usuarios = new ArrayList<>();
        if (array == null) {
            return usuarios;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            usuarios.add(new UsuarioAdmin(
                    item.optInt("id_usuario"),
                    item.optString("nombre", ""),
                    item.optString("apellidos", ""),
                    item.optString("email", ""),
                    item.optString("rol", "usuario"),
                    item.optBoolean("activo", true)
            ));
        }
        return usuarios;
    }

    /** -------------------- json: Text --> password_temporal: Text -------------------- */
    public static String passwordTemporal(String json) {
        try {
            return new JSONObject(json).optString("password_temporal", "");
        } catch (JSONException e) {
            return "";
        }
    }
}
