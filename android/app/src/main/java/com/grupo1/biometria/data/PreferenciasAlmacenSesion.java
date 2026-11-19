package com.grupo1.biometria.data;

import android.content.Context;
import android.content.SharedPreferences;

/** Guarda la sesión en SharedPreferences de la app (privadas). */
public class PreferenciasAlmacenSesion implements AlmacenSesion {

    private static final String NOMBRE_FICHERO = "biometria_sesion";
    private static final String CLAVE_TOKEN = "token";
    private static final String CLAVE_USUARIO = "usuario";

    private final SharedPreferences preferencias;

    public PreferenciasAlmacenSesion(Context contexto) {
        this.preferencias = contexto.getApplicationContext()
                .getSharedPreferences(NOMBRE_FICHERO, Context.MODE_PRIVATE);
    }

    /**
     * --------------------
     * token: Text, usuario_json: Text --> guardar() -->
     * --------------------
     */
    @Override
    public void guardar(String token, String usuarioJson) {
        preferencias.edit()
                .putString(CLAVE_TOKEN, token)
                .putString(CLAVE_USUARIO, usuarioJson)
                .apply();
    }

    /**
     * --------------------
     * --> token() --> token: Text
     * --------------------
     */
    @Override
    public String token() {
        return preferencias.getString(CLAVE_TOKEN, null);
    }

    /**
     * --------------------
     * --> usuario_json() --> json: Text
     * --------------------
     */
    @Override
    public String usuarioJson() {
        return preferencias.getString(CLAVE_USUARIO, null);
    }

    /**
     * --------------------
     * --> limpiar() -->
     * --------------------
     */
    @Override
    public void limpiar() {
        preferencias.edit()
                .remove(CLAVE_TOKEN)
                .remove(CLAVE_USUARIO)
                .apply();
    }
}
