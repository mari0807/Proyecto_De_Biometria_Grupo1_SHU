package com.grupo1.biometria.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Executor;

/**
 * Implementación de {@link AuthRepository} contra la API REST del proyecto.
 *
 * Thread-safety:
 * - La red se ejecuta en `hiloRed` (nunca en el hilo principal).
 * - El callback se entrega en `hiloUi` (el hilo principal de Android).
 * - El estado de sesión vive en {@link SesionManager}, que sincroniza sus
 *   métodos; esta clase no muta estado compartido por sí misma.
 */
public class AuthRepositoryRemoto implements AuthRepository {

    private static final String MENSAJE_RED = "";

    private final ClienteRest cliente;
    private final SesionManager sesion;
    private final Executor hiloRed;
    private final Executor hiloUi;

    public AuthRepositoryRemoto(ClienteRest cliente, SesionManager sesion, Executor hiloRed, Executor hiloUi) {
        this.cliente = cliente;
        this.sesion = sesion;
        this.hiloRed = hiloRed;
        this.hiloUi = hiloUi;
    }

    /**
     * --------------------
     * email: Text, password: Text, mantener: B --> iniciar_sesion() --> respuesta: RespuestaAuth
     * --------------------
     */
    @Override
    public void iniciarSesion(String email, String password, boolean mantener, AuthCallback callback) {
        String cuerpo = json("email", email, "password", password);
        enRed(() -> {
            RespuestaHttp respuesta;
            try {
                respuesta = cliente.postJson("/api/login", cuerpo, null);
            } catch (IOException e) {
                return RespuestaAuth.deRed(MENSAJE_RED);
            }
            RespuestaAuth auth = RespuestaAuth.deLogin(respuesta.estado, respuesta.cuerpo);
            if (auth.estado == EstadoAuth.OK) {
                sesion.abrir(auth.token, auth.usuario, mantener);
            }
            return auth;
        }, callback);
    }

    /**
     * --------------------
     * nombre: Text, apellidos: Text, email: Text, password: Text --> registrar() --> respuesta: RespuestaAuth
     * --------------------
     */
    @Override
    public void registrar(String nombre, String apellidos, String email, String password, AuthCallback callback) {
        String cuerpo = json("nombre", nombre, "apellidos", apellidos, "email", email, "password", password);
        enRed(() -> {
            try {
                RespuestaHttp respuesta = cliente.postJson("/api/registro", cuerpo, null);
                return RespuestaAuth.deRegistro(respuesta.estado, respuesta.cuerpo);
            } catch (IOException e) {
                return RespuestaAuth.deRed(MENSAJE_RED);
            }
        }, callback);
    }

    /**
     * --------------------
     * --> cerrar_sesion() --> respuesta: RespuestaAuth
     * --------------------
     */
    @Override
    public void cerrarSesion(AuthCallback callback) {
        // La credencial local se borra primero: aunque el servidor falle,
        // la app queda sin sesión (mismo criterio que logicaProxy.cerrarSesion).
        String token = sesion.token();
        sesion.cerrar();
        enRed(() -> {
            if (token == null || token.isEmpty()) {
                return RespuestaAuth.deSesion(200, "{\"ok\":true}");
            }
            try {
                RespuestaHttp respuesta = cliente.postJson("/api/logout", null, token);
                return RespuestaAuth.deSesion(respuesta.estado, respuesta.cuerpo);
            } catch (IOException e) {
                // La sesión local ya está cerrada: para la UI el cierre es un éxito.
                return RespuestaAuth.deSesion(200, "{\"ok\":true}");
            }
        }, callback);
    }

    /**
     * --------------------
     * --> comprobar_sesion() --> respuesta: RespuestaAuth
     * --------------------
     */
    @Override
    public void comprobarSesion(AuthCallback callback) {
        String token = sesion.token();
        if (token == null || token.isEmpty()) {
            entregar(callback, RespuestaAuth.deSesion(401, "{\"ok\":false}"));
            return;
        }
        enRed(() -> {
            RespuestaHttp respuesta;
            try {
                respuesta = cliente.getJson("/api/sesion", token);
            } catch (IOException e) {
                // Sin red no se puede validar: se conserva la sesión local
                // (criterio de la web: en errores de red no se borra).
                return RespuestaAuth.deSesion(200, "{\"ok\":true}");
            }
            RespuestaAuth auth = RespuestaAuth.deSesion(respuesta.estado, respuesta.cuerpo);
            if (auth.estado == EstadoAuth.OK && auth.usuario != null) {
                sesion.actualizarUsuario(auth.usuario);
            } else if (auth.estado == EstadoAuth.SESION_INVALIDA) {
                sesion.cerrar();
            }
            return auth;
        }, callback);
    }

    /**
     * --------------------
     * --> hay_sesion() --> B
     * --------------------
     */
    @Override
    public boolean haySesion() {
        return sesion.haySesion();
    }

    /**
     * --------------------
     * --> usuario() --> usuario: Usuario
     * --------------------
     */
    @Override
    public Usuario usuario() {
        return sesion.usuario();
    }

    /** Ejecuta `tarea` en el hilo de red y entrega el resultado en el hilo UI. */
    private void enRed(Tarea tarea, AuthCallback callback) {
        hiloRed.execute(() -> {
            RespuestaAuth respuesta;
            try {
                respuesta = tarea.ejecutar();
            } catch (IOException e) {
                // Sin conexión con el servidor.
                respuesta = RespuestaAuth.deRed(MENSAJE_RED);
            } catch (RuntimeException e) {
                // Fallo inesperado de parseo: se informa como error de red/servidor.
                respuesta = RespuestaAuth.deRed(MENSAJE_RED);
            }
            entregar(callback, respuesta);
        });
    }

    private void entregar(AuthCallback callback, RespuestaAuth respuesta) {
        hiloUi.execute(() -> callback.onResultado(respuesta));
    }

    private interface Tarea {
        RespuestaAuth ejecutar() throws IOException;
    }

    /** Construye el cuerpo JSON de la petición con pares clave/valor. */
    private static String json(String... pares) {
        try {
            JSONObject objeto = new JSONObject();
            for (int i = 0; i < pares.length; i += 2) {
                objeto.put(pares[i], pares[i + 1]);
            }
            return objeto.toString();
        } catch (JSONException e) {
            // Claves literales de la app: no pueden fallar.
            throw new IllegalStateException(e);
        }
    }
}
