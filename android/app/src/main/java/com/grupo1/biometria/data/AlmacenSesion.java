package com.grupo1.biometria.data;

/**
 * Persistencia de la sesión ("mantener sesión iniciada").
 * Separa el almacenamiento de la lógica para poder probarlo en JVM.
 */
public interface AlmacenSesion {

    /**
     * --------------------
     * token: Text, usuario_json: Text --> guardar() -->
     * --------------------
     */
    void guardar(String token, String usuarioJson);

    /**
     * --------------------
     * --> token() --> token: Text
     * --------------------
     *
     * @return el token guardado o {@code/null} si no hay.
     */
    String token();

    /**
     * --------------------
     * --> usuario_json() --> json: Text
     * --------------------
     *
     * @return el usuario guardado en JSON o {@code null} si no hay.
     */
    String usuarioJson();

    /**
     * --------------------
     * --> limpiar() -->
     * --------------------
     */
    void limpiar();
}
