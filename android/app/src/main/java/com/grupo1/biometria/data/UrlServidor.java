package com.grupo1.biometria.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.grupo1.biometria.BuildConfig;

/**
 * URL base de la API, editable desde la pantalla de inicio de sesión.
 *
 * Cada miembro del equipo ejecuta el backend en su propio PC, así que la URL
 * se guarda en SharedPreferences en lugar de fijarse solo al compilar: con el
 * campo «Servidor» del login se apunta la app al backend de turno sin tener
 * que recompilar la APK.
 */
public final class UrlServidor {

    private static final String NOMBRE_FICHERO = "biometria_ajustes";
    private static final String CLAVE_URL = "url_servidor";

    private UrlServidor() {
    }

    /**
     * --------------------
     * --> url() --> base: Text
     * --------------------
     *
     * Devuelve la URL guardada; si está vacía (nunca se ha escrito o se borró)
     * cae en el valor por defecto del build (`BuildConfig.API_BASE`).
     */
    public static String url(Context contexto) {
        SharedPreferences preferencias = contexto.getApplicationContext()
                .getSharedPreferences(NOMBRE_FICHERO, Context.MODE_PRIVATE);
        String guardada = preferencias.getString(CLAVE_URL, null);
        if (guardada == null || guardada.trim().isEmpty()) {
            return BuildConfig.API_BASE;
        }
        return guardada.trim();
    }

    /**
     * --------------------
     * base: Text --> guardar() -->
     * --------------------
     *
     * Una cadena vacía hace volver al valor por defecto del build.
     */
    public static void guardar(Context contexto, String base) {
        String limpia = base == null ? "" : base.trim();
        contexto.getApplicationContext()
                .getSharedPreferences(NOMBRE_FICHERO, Context.MODE_PRIVATE)
                .edit()
                .putString(CLAVE_URL, limpia)
                .apply();
    }
}
