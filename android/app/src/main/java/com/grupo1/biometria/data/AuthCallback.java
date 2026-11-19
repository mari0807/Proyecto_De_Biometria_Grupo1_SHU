package com.grupo1.biometria.data;

/**
 * Callback de las operaciones de autenticación.
 * Siempre se entrega en el hilo principal: la UI puede tocar vistas.
 */
public interface AuthCallback {

    /**
     * --------------------
     * respuesta: RespuestaAuth --> onResultado() -->
     * --------------------
     */
    void onResultado(RespuestaAuth respuesta);
}
