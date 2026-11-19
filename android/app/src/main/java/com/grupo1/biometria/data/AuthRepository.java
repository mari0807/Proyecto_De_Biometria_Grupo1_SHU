package com.grupo1.biometria.data;

/**
 * Contrato de autenticación de la app contra el backend del proyecto.
 *
 * Las operaciones de red son **asíncronas**: recibe un {@link AuthCallback}
 * que siempre se ejecuta en el hilo principal, de forma que la UI puede
 * actualizar vistas sin riesgo. La comprobación local de la sesión
 * ({@link #haySesion()}, {@link #usuario()}) es síncrona.
 */
public interface AuthRepository {

    /**
     * --------------------
     * email: Text, password: Text, mantener: B --> iniciar_sesion() --> respuesta: RespuestaAuth
     * --------------------
     *
     * `POST /api/login`. Si es correcta, guarda la sesión (token + usuario).
     */
    void iniciarSesion(String email, String password, boolean mantener, AuthCallback callback);

    /**
     * --------------------
     * nombre: Text, apellidos: Text, email: Text, password: Text --> registrar() --> respuesta: RespuestaAuth
     * --------------------
     *
     * `POST /api/registro`. No inicia sesión: el backend no devuelve token aquí.
     */
    void registrar(String nombre, String apellidos, String email, String password, AuthCallback callback);

    /**
     * --------------------
     * --> cerrar_sesion() --> respuesta: RespuestaAuth
     * --------------------
     *
     * `POST /api/logout`. Borra la sesión local **antes** de avisar al
     * servidor (mismo criterio que la web) y termina siempre con estado OK:
     * la credencial local ya no existe aunque el servidor no responda.
     */
    void cerrarSesion(AuthCallback callback);

    /**
     * --------------------
     * --> comprobar_sesion() --> respuesta: RespuestaAuth
     * --------------------
     *
     * `GET /api/sesion` con el token almacenado. Si el servidor la rechaza,
     * borra la sesión local.
     */
    void comprobarSesion(AuthCallback callback);

    /**
     * --------------------
     * --> hay_sesion() --> B
     * --------------------
     *
     * Comprobación local (token presente y no caducado por el reloj del móvil).
     */
    boolean haySesion();

    /**
     * --------------------
     * --> usuario() --> usuario: Usuario
     * --------------------
     *
     * @return el usuario en sesión o {@code null} si no lo hay.
     */
    Usuario usuario();
}
