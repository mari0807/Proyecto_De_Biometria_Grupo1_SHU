package com.grupo1.biometria.data;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Usuario público devuelto por la API (nunca incluye `password_hash`).
 * Coincide con `publico()` de `Autenticacion.php`.
 */
public final class Usuario {

    public final int idUsuario;
    public final String nombre;
    public final String apellidos;
    public final String email;
    public final String rol;
    public final String foto;

    public Usuario(int idUsuario, String nombre, String apellidos, String email, String rol, String foto) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.email = email;
        this.rol = rol;
        this.foto = foto;
    }

    /**
     * --------------------
     * json: Text --> desde_json() --> usuario: Usuario
     * --------------------
     *
     * @return el usuario parseado o {@code null} si el JSON no es válido.
     */
    public static Usuario desdeJson(String json) {
        try {
            JSONObject objeto = new JSONObject(json);
            return new Usuario(
                    objeto.optInt("id_usuario", 0),
                    objeto.optString("nombre", ""),
                    objeto.optString("apellidos", ""),
                    objeto.optString("email", ""),
                    objeto.optString("rol", ""),
                    objeto.optString("foto", ""));
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * --------------------
     * --> a_json() --> json: Text
     * --------------------
     */
    public String aJson() {
        try {
            return new JSONObject()
                    .put("id_usuario", idUsuario)
                    .put("nombre", nombre)
                    .put("apellidos", apellidos)
                    .put("email", email)
                    .put("rol", rol)
                    .put("foto", foto)
                    .toString();
        } catch (JSONException e) {
            // Campos de tipo primitivo: no puede fallar.
            throw new IllegalStateException(e);
        }
    }

    /**
     * --------------------
     * --> nombre_completo() --> texto: Text
     * --------------------
     */
    public String nombreCompleto() {
        String completo = (nombre + " " + apellidos).trim();
        return completo.isEmpty() ? email : completo;
    }
}
