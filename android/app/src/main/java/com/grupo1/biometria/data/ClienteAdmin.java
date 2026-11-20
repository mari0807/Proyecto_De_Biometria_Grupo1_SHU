package com.grupo1.biometria.data;

import android.os.Handler;
import android.os.Looper;

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
 * CD: ClienteAdmin = ( listar() --> [ UsuarioAdmin ],
 *                     crear(datos) --> password: Text,
 *                     actualizar(id, datos) --> mensaje: Text,
 *                     borrar(id) --> mensaje: Text,
 *                     nuevaPassword(id) --> password: Text )
 * CC: Contrato de ApiAuth.php: las mismas cinco operaciones que hace
 *     `adminUsuarios.js` de la web, con la cabecera Bearer del admin.
 * GR: Un único hilo de red; los callbacks se entregan en el hilo principal.
 * --------------------
 */
public final class ClienteAdmin {
    public interface ListadoCallback {
        void onExito(List<UsuarioAdmin> usuarios);

        void onError(String mensaje, boolean noAutorizado);
    }

    public interface UsuarioCallback {
        void onExito(String mensaje);

        void onError(String mensaje, boolean noAutorizado);
    }

    public interface PasswordCallback {
        void onExito(String passwordTemporal);

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
    public ClienteAdmin(String urlBase, Supplier<String> token) {
        this.urlBase = urlBase == null ? "" : urlBase.trim().replaceAll("/+$", "");
        this.token = token;
    }

    /** -------------------- callback --> GET /api/usuarios -------------------- */
    public void listar(ListadoCallback callback) {
        ejecutar(() -> {
            String texto = peticion("GET", urlBase + "/api/usuarios", null);
            validar(texto, "No se ha podido cargar la lista de usuarios.");
            List<UsuarioAdmin> usuarios = AdminJson.usuarios(texto);
            principal.post(() -> callback.onExito(usuarios));
        }, callback::onError);
    }

    /** -------------------- datos, callback --> POST /api/usuarios -------------------- */
    public void crear(String nombre, String apellidos, String email, String rol, PasswordCallback callback) {
        ejecutar(() -> {
            String cuerpo = new JSONObject()
                    .put("nombre", nombre)
                    .put("apellidos", apellidos)
                    .put("email", email)
                    .put("rol", rol)
                    .toString();
            String texto = peticion("POST", urlBase + "/api/usuarios", cuerpo);
            validar(texto, "No se ha podido crear el usuario.");
            String temporal = AdminJson.passwordTemporal(texto);
            principal.post(() -> callback.onExito(temporal));
        }, callback::onError);
    }

    /** -------------------- id:N, datos, callback --> PUT /api/usuarios/{id} -------------------- */
    public void actualizar(int id, String nombre, String apellidos, String email, boolean activo, String rol,
                           UsuarioCallback callback) {
        ejecutar(() -> {
            String cuerpo = new JSONObject()
                    .put("nombre", nombre)
                    .put("apellidos", apellidos)
                    .put("email", email)
                    .put("activo", activo)
                    .put("rol", rol)
                    .toString();
            String texto = peticion("PUT", urlBase + "/api/usuarios/" + id, cuerpo);
            validar(texto, "No se ha podido actualizar el usuario.");
            principal.post(() -> callback.onExito(
                    AdminJson.mensaje(texto, "Usuario actualizado correctamente.")));
        }, callback::onError);
    }

    /** -------------------- id:N, callback --> DELETE /api/usuarios/{id} -------------------- */
    public void borrar(int id, UsuarioCallback callback) {
        ejecutar(() -> {
            String texto = peticion("DELETE", urlBase + "/api/usuarios/" + id, null);
            validar(texto, "No se ha podido borrar el usuario.");
            principal.post(() -> callback.onExito(AdminJson.mensaje(texto, "Cuenta eliminada.")));
        }, callback::onError);
    }

    /** -------------------- id:N, callback --> POST /api/usuarios/{id}/password -------------------- */
    public void nuevaPassword(int id, PasswordCallback callback) {
        ejecutar(() -> {
            String texto = peticion("POST", urlBase + "/api/usuarios/" + id + "/password", null);
            validar(texto, "No se ha podido generar la contraseña.");
            String temporal = AdminJson.passwordTemporal(texto);
            principal.post(() -> callback.onExito(temporal));
        }, callback::onError);
    }

    /** -------------------- () --> () -------------------- */
    public void cerrar() {
        executor.shutdownNow();
    }

    /** -------------------- texto: Text, porDefecto: Text --> () -------------------- */
    private static void validar(String texto, String porDefecto) throws HttpException {
        if (!AdminJson.ok(texto)) {
            throw new HttpException(400, AdminJson.mensaje(texto, porDefecto));
        }
    }

    /** -------------------- metodo:Text, url:Text, cuerpo:Text|NULL --> json: Text -------------------- */
    private String peticion(String metodo, String url, String cuerpo) throws Exception {
        HttpURLConnection conexion = null;
        try {
            conexion = (HttpURLConnection) new URL(url).openConnection();
            conexion.setRequestMethod(metodo);
            conexion.setConnectTimeout(10000);
            conexion.setReadTimeout(10000);
            conexion.setRequestProperty("Accept", "application/json");
            // TODAS las cabeceras antes de abrir el flujo de salida: tras
            // getOutputStream() la conexion ya esta abierta y poner otra
            // cabecera lanza IllegalStateException.
            String autorizacion = token.get();
            if (autorizacion != null && !autorizacion.isEmpty()) {
                conexion.setRequestProperty("Authorization", "Bearer " + autorizacion);
            }
            if (cuerpo != null) {
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conexion.setDoOutput(true);
                OutputStream salida = conexion.getOutputStream();
                salida.write(cuerpo.getBytes(StandardCharsets.UTF_8));
                salida.flush();
                salida.close();
            }

            int codigo = conexion.getResponseCode();
            InputStream flujo = codigo >= 400 ? conexion.getErrorStream() : conexion.getInputStream();
            String respuesta = leer(flujo);
            if (codigo != 200) {
                throw new HttpException(codigo, AdminJson.mensaje(respuesta, "Error " + codigo + " del servidor."));
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
                android.util.Log.e("ClienteAdmin", "Fallo inesperado en la peticion", e);
                notificar(fallo, "No se ha podido conectar con el servidor ("
                        + e.getClass().getSimpleName() + ").", false);
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
