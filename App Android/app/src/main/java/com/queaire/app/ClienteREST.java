package com.queaire.app;

import android.os.Handler;
import android.os.Looper;

import com.queaire.app.model.PuntoMapa;
import com.queaire.app.model.ZonaDetalle;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * --------------------
 * CD: ClienteREST = ( login(Text,Text,Text) --> token:Text,
 *                     puntos(Text) --> [ PuntoMapa ],
 *                     zona(N,Text) --> ZonaDetalle )
 * CC: Contrato de Agentes_IA/Agente Backend/context/Comunicacion.md:39-81 y ApiMapa.php:31-36.
 * GR: Un único hilo de red; los resultados se entregan siempre en el hilo principal.
 * --------------------
 */
public final class ClienteREST {
    public interface LoginCallback {
        void onExito(String token, String usuario);

        void onError(String mensaje);
    }

    public interface PuntosCallback {
        void onExito(List<PuntoMapa> puntos);

        void onError(String mensaje, boolean noAutorizado);
    }

    public interface ZonaCallback {
        void onExito(ZonaDetalle zona);

        void onError(String mensaje, boolean noAutorizado);
    }

    private interface Tarea {
        void ejecutar() throws Exception;
    }

    private interface Fallo {
        void aceptar(String mensaje, boolean noAutorizado);
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler principal = new Handler(Looper.getMainLooper());

    /** -------------------- base:Text, email:Text, password:Text, callback --> POST /api/login -------------------- */
    public void login(String base, String email, String password, LoginCallback callback) {
        ejecutar(() -> {
            String cuerpo = new JSONObject()
                    .put("email", email)
                    .put("password", password)
                    .toString();
            String texto = peticion("POST", url(base, "/login"), cuerpo, null);
            JSONObject respuesta = new JSONObject(texto);
            if (!respuesta.optBoolean("ok", false)) {
                throw new HttpException(400, MapaJson.mensaje(texto, "No se ha podido iniciar sesión."));
            }
            String token = respuesta.optString("token", "");
            if (token.isEmpty()) {
                throw new HttpException(400, "El servidor no devolvió un token.");
            }
            JSONObject usuario = respuesta.optJSONObject("usuario");
            String nombre = usuario == null ? "" : usuario.optString("nombre", "");
            final String tokenFinal = token;
            final String nombreFinal = nombre;
            principal.post(() -> callback.onExito(tokenFinal, nombreFinal));
        }, (mensaje, noAutorizado) -> callback.onError(mensaje));
    }

    /** -------------------- base:Text, token:Text, callback --> GET /api/mapa -------------------- */
    public void puntos(String base, String token, PuntosCallback callback) {
        ejecutar(() -> {
            String texto = peticion("GET", url(base, "/mapa"), null, token);
            validarRespuesta(texto, "No hay datos disponibles en este momento.");
            List<PuntoMapa> puntos = MapaJson.puntos(texto);
            principal.post(() -> callback.onExito(puntos));
        }, callback::onError);
    }

    /** -------------------- idNodo:N, base:Text, token:Text, callback --> GET /api/mapa/zona/{id} -------------------- */
    public void zona(int idNodo, String base, String token, ZonaCallback callback) {
        ejecutar(() -> {
            String texto = peticion("GET", url(base, "/mapa/zona/" + idNodo), null, token);
            validarRespuesta(texto, "No se ha podido cargar la zona.");
            ZonaDetalle zona = MapaJson.zona(texto);
            principal.post(() -> callback.onExito(zona));
        }, callback::onError);
    }

    /** -------------------- () --> () -------------------- */
    public void cerrar() {
        executor.shutdownNow();
    }

    /** -------------------- base:Text --> urlApi:Text -------------------- */
    private static String url(String base, String ruta) {
        String raiz = base == null ? "" : base.trim().replaceAll("/+$", "");
        if (!raiz.endsWith("/api")) {
            raiz = raiz + "/api";
        }
        return raiz + ruta;
    }

    /** -------------------- texto:Text, porDefecto:Text --> () -------------------- */
    private static void validarRespuesta(String texto, String porDefecto) throws HttpException {
        if (!MapaJson.ok(texto)) {
            throw new HttpException(400, MapaJson.mensaje(texto, porDefecto));
        }
    }

    /** -------------------- metodo:Text, url:Text, cuerpo:Text|NULL, token:Text|NULL --> json:Text -------------------- */
    private static String peticion(String metodo, String url, String cuerpo, String token) throws Exception {
        HttpURLConnection conexion = null;
        try {
            conexion = (HttpURLConnection) new URL(url).openConnection();
            conexion.setRequestMethod(metodo);
            conexion.setConnectTimeout(10000);
            conexion.setReadTimeout(10000);
            conexion.setRequestProperty("Accept", "application/json");
            if (cuerpo != null) {
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conexion.setDoOutput(true);
                OutputStream salida = conexion.getOutputStream();
                salida.write(cuerpo.getBytes(StandardCharsets.UTF_8));
                salida.flush();
                salida.close();
            }
            if (token != null && !token.isEmpty()) {
                conexion.setRequestProperty("Authorization", "Bearer " + token);
            }

            int codigo = conexion.getResponseCode();
            InputStream flujo = codigo >= 400 ? conexion.getErrorStream() : conexion.getInputStream();
            String respuesta = leer(flujo);
            if (codigo != 200) {
                throw new HttpException(codigo, MapaJson.mensaje(respuesta, "Error " + codigo + " del servidor."));
            }
            return respuesta;
        } finally {
            if (conexion != null) {
                conexion.disconnect();
            }
        }
    }

    /** -------------------- flujo:InputStream|NULL --> texto:Text -------------------- */
    private static String leer(InputStream flujo) throws IOException {
        if (flujo == null) {
            return "";
        }
        BufferedReader lector = new BufferedReader(new InputStreamReader(flujo, StandardCharsets.UTF_8));
        StringBuilder contenido = new StringBuilder();
        String linea;
        while ((linea = lector.readLine()) != null) {
            contenido.append(linea);
        }
        lector.close();
        return contenido.toString();
    }

    /** -------------------- tarea, fallo --> () -------------------- */
    private void ejecutar(Tarea tarea, Fallo fallo) {
        executor.execute(() -> {
            try {
                tarea.ejecutar();
            } catch (HttpException e) {
                notificar(fallo, e.getMessage(), e.codigo == 401);
            } catch (Exception e) {
                notificar(fallo, "No se ha podido conectar con el servidor.", false);
            }
        });
    }

    /** -------------------- fallo, mensaje:Text, noAutorizado:B --> () -------------------- */
    private void notificar(Fallo fallo, String mensaje, boolean noAutorizado) {
        principal.post(() -> fallo.aceptar(mensaje, noAutorizado));
    }

    private static final class HttpException extends Exception {
        private static final long serialVersionUID = 1L;

        private final int codigo;

        HttpException(int codigo, String mensaje) {
            super(mensaje);
            this.codigo = codigo;
        }
    }
}
