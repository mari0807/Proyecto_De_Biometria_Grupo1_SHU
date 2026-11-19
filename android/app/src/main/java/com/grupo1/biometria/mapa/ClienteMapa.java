package com.grupo1.biometria.mapa;

import android.os.Handler;
import android.os.Looper;

import com.grupo1.biometria.mapa.model.PuntoMapa;
import com.grupo1.biometria.mapa.model.ZonaDetalle;

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
import java.util.function.Supplier;

/**
 * --------------------
 * CD: ClienteMapa = ( puntos() --> [ PuntoMapa ], zona(N) --> ZonaDetalle )
 * CC: Contrato de ApiMapa.php (`GET /api/mapa` y `GET /api/mapa/zona/{id}` con
 *     cabecera Bearer). Portado del ClienteREST de la primera app y adaptado a
 *     la capa de datos de esta: la URL base viene de UrlServidor y el token, de
 *     la sesión compartida (AuthRepositoryProvider.sesion()).
 * GR: Un único hilo de red; los callbacks se entregan siempre en el hilo principal.
 * --------------------
 */
public final class ClienteMapa {
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
    private final String urlBase;
    private final Supplier<String> token;

    /** -------------------- urlBase: Text, token: () --> token:Text|NULL -------------------- */
    public ClienteMapa(String urlBase, Supplier<String> token) {
        this.urlBase = urlBase == null ? "" : urlBase.trim().replaceAll("/+$", "");
        this.token = token;
    }

    /** -------------------- callback --> GET /api/mapa -------------------- */
    public void puntos(PuntosCallback callback) {
        ejecutar(() -> {
            String texto = peticion("GET", urlBase + "/api/mapa", null);
            validarRespuesta(texto, "No hay datos disponibles en este momento.");
            List<PuntoMapa> puntos = MapaJson.puntos(texto);
            principal.post(() -> callback.onExito(puntos));
        }, callback::onError);
    }

    /** -------------------- idNodo:N, callback --> GET /api/mapa/zona/{id} -------------------- */
    public void zona(int idNodo, ZonaCallback callback) {
        ejecutar(() -> {
            String texto = peticion("GET", urlBase + "/api/mapa/zona/" + idNodo, null);
            validarRespuesta(texto, "No se ha podido cargar la zona.");
            ZonaDetalle zona = MapaJson.zona(texto);
            principal.post(() -> callback.onExito(zona));
        }, callback::onError);
    }

    /** -------------------- () --> () -------------------- */
    public void cerrar() {
        executor.shutdownNow();
    }

    /** -------------------- texto: Text, porDefecto: Text --> () -------------------- */
    private static void validarRespuesta(String texto, String porDefecto) throws HttpException {
        if (!MapaJson.ok(texto)) {
            throw new HttpException(400, MapaJson.mensaje(texto, porDefecto));
        }
    }

    /** -------------------- metodo:Text, url:Text, cuerpo:Text|NULL --> json:Text -------------------- */
    private String peticion(String metodo, String url, String cuerpo) throws Exception {
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
            String autorizacion = token.get();
            if (autorizacion != null && !autorizacion.isEmpty()) {
                conexion.setRequestProperty("Authorization", "Bearer " + autorizacion);
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

    /** -------------------- flujo:InputStream|NULL --> texto: Text -------------------- */
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

    /** -------------------- fallo, mensaje: Text, noAutorizado:B --> () -------------------- */
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
