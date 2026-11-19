package com.queaire.app;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * --------------------
 * CD: SesionManager = ( token:Text, base:Text, email:Text, usuario:Text )
 * CC: Equivale a sessionStorage["queaire_sesion"] de la web (logicaProxy.js:1,42-44).
 * GR: SharedPreferences es seguro entre hilos; la UI solo lee y escribe en main.
 * --------------------
 */
public final class SesionManager {
    private static final String CLAVE = "queaire_sesion";
    private static final String TOKEN = "token";
    private static final String BASE = "base_url";
    private static final String EMAIL = "email";
    private static final String USUARIO = "usuario";
    private static final String BASE_POR_DEFECTO = "http://10.0.2.2/api";

    private final SharedPreferences preferencias;

    /** -------------------- contexto --> SesionManager -------------------- */
    public SesionManager(Context contexto) {
        preferencias = contexto.getSharedPreferences(CLAVE, Context.MODE_PRIVATE);
    }

    /** -------------------- () --> token:Text -------------------- */
    public String token() {
        return preferencias.getString(TOKEN, "");
    }

    /** -------------------- () --> base:Text -------------------- */
    public String baseUrl() {
        String base = preferencias.getString(BASE, "");
        return base == null || base.trim().isEmpty() ? BASE_POR_DEFECTO : base;
    }

    /** -------------------- () --> email:Text -------------------- */
    public String email() {
        return preferencias.getString(EMAIL, "");
    }

    /** -------------------- () --> usuario:Text -------------------- */
    public String usuario() {
        return preferencias.getString(USUARIO, "");
    }

    /** -------------------- () --> haySesion:B -------------------- */
    public boolean haySesion() {
        return !token().isEmpty();
    }

    /** -------------------- base:Text, token:Text, email:Text, usuario:Text --> () -------------------- */
    public void guardar(String base, String token, String email, String usuario) {
        preferencias.edit()
                .putString(BASE, base)
                .putString(TOKEN, token)
                .putString(EMAIL, email)
                .putString(USUARIO, usuario)
                .apply();
    }

    /** -------------------- () --> () -------------------- */
    public void limpiar() {
        preferencias.edit().clear().apply();
    }
}
