package com.grupo1.biometria.data;

import org.json.JSONObject;

/**
 * Resultado de una operación de autenticación: estado, mensaje del servidor,
 * usuario público (si lo hay) y token (solo en login).
 *
 * La UI traduce los estados a recursos de `strings.xml`; los mensajes que
 * vienen del servidor se muestran tal cual (ya están en español).
 */
public final class RespuestaAuth {

    public final EstadoAuth estado;
    public final String mensaje;
    public final Usuario usuario;
    public final String token;

    private RespuestaAuth(EstadoAuth estado, String mensaje, Usuario usuario, String token) {
        this.estado = estado;
        this.mensaje = mensaje == null ? "" : mensaje;
        this.usuario = usuario;
        this.token = token == null ? "" : token;
    }

    /**
     * --------------------
     * http: N, cuerpo: Text --> de_login() --> respuesta: RespuestaAuth
     * --------------------
     *
     * `POST /api/login`: 200 con token, 400 datos, 401 credenciales.
     */
    public static RespuestaAuth deLogin(int http, String cuerpo) {
        JSONObject json = parsear(cuerpo);
        String mensaje = mensajeDe(json, "");
        if (http == 200) {
            String token = tokenDe(json);
            Usuario usuario = usuarioDe(json);
            if (esOk(json) && !token.isEmpty() && usuario != null) {
                return new RespuestaAuth(EstadoAuth.OK, mensaje, usuario, token);
            }
            return servidor(mensaje);
        }
        if (http == 400) {
            return new RespuestaAuth(EstadoAuth.DATOS_INVALIDOS, mensaje, null, "");
        }
        if (http == 401) {
            return new RespuestaAuth(EstadoAuth.CREDENCIALES_INVALIDAS, mensaje, null, "");
        }
        return servidor(mensaje);
    }

    /**
     * --------------------
     * http: N, cuerpo: Text --> de_registro() --> respuesta: RespuestaAuth
     * --------------------
     *
     * `POST /api/registro`: 200 creado, 409 correo repetido, 400 datos.
     * El backend no devuelve token en el registro.
     */
    public static RespuestaAuth deRegistro(int http, String cuerpo) {
        JSONObject json = parsear(cuerpo);
        String mensaje = mensajeDe(json, "");
        if (http == 200 && esOk(json)) {
            return new RespuestaAuth(EstadoAuth.OK, mensaje, usuarioDe(json), "");
        }
        if (http == 409) {
            return new RespuestaAuth(EstadoAuth.CORREO_REGISTRADO, mensaje, null, "");
        }
        if (http == 400) {
            return new RespuestaAuth(EstadoAuth.DATOS_INVALIDOS, mensaje, null, "");
        }
        return servidor(mensaje);
    }

    /**
     * --------------------
     * http: N, cuerpo: Text --> de_sesion() --> respuesta: RespuestaAuth
     * --------------------
     *
     * `GET /api/sesion` y `POST /api/logout`: 200 correcto, 401 sesión inválida.
     */
    public static RespuestaAuth deSesion(int http, String cuerpo) {
        JSONObject json = parsear(cuerpo);
        String mensaje = mensajeDe(json, "");
        if (http == 200 && esOk(json)) {
            return new RespuestaAuth(EstadoAuth.OK, mensaje, usuarioDe(json), "");
        }
        if (http == 401) {
            return new RespuestaAuth(EstadoAuth.SESION_INVALIDA, mensaje, null, "");
        }
        if (http == 400) {
            return new RespuestaAuth(EstadoAuth.DATOS_INVALIDOS, mensaje, null, "");
        }
        return servidor(mensaje);
    }

    /**
     * --------------------
     * mensaje: Text --> de_red() --> respuesta: RespuestaAuth
     * --------------------
     *
     * Fallo de conexión (el servidor no responde o no está en marcha).
     */
    public static RespuestaAuth deRed(String mensaje) {
        return new RespuestaAuth(EstadoAuth.ERROR_RED, mensaje, null, "");
    }

    private static RespuestaAuth servidor(String mensaje) {
        String texto = mensaje.isEmpty() ? "Error del servidor. Inténtalo más tarde." : mensaje;
        return new RespuestaAuth(EstadoAuth.ERROR_SERVIDOR, texto, null, "");
    }

    private static JSONObject parsear(String cuerpo) {
        if (cuerpo == null || cuerpo.isEmpty()) {
            return null;
        }
        try {
            return new JSONObject(cuerpo);
        } catch (Exception e) {
            // Respuesta sin JSON (p. ej. una página de error del servidor PHP).
            return null;
        }
    }

    private static boolean esOk(JSONObject json) {
        return json != null && json.optBoolean("ok", false);
    }

    private static String mensajeDe(JSONObject json, String porDefecto) {
        if (json == null) {
            return porDefecto;
        }
        return json.optString("mensaje", porDefecto);
    }

    private static String tokenDe(JSONObject json) {
        return json == null ? "" : json.optString("token", "");
    }

    private static Usuario usuarioDe(JSONObject json) {
        if (json == null || !json.has("usuario") || json.isNull("usuario")) {
            return null;
        }
        return Usuario.desdeJson(json.optJSONObject("usuario").toString());
    }
}
