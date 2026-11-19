package com.grupo1.biometria.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Implementación de {@link ClienteRest} con `HttpURLConnection`.
 * Sin dependencias externas: la app solo necesita la librería de Android.
 */
public class HttpClienteRest implements ClienteRest {

    private static final int TIMEOUT_MS = 8000;

    private final String urlBase;

    public HttpClienteRest(String urlBase) {
        this.urlBase = urlBase;
    }

    /**
     * --------------------
     * ruta: Text, cuerpo: Text?, token: Text? --> post_json() --> respuesta: RespuestaHttp
     * --------------------
     */
    @Override
    public RespuestaHttp postJson(String ruta, String cuerpo, String token) throws IOException {
        return enviar("POST", ruta, cuerpo, token);
    }

    /**
     * --------------------
     * ruta: Text, token: Text? --> get_json() --> respuesta: RespuestaHttp
     * --------------------
     */
    @Override
    public RespuestaHttp getJson(String ruta, String token) throws IOException {
        return enviar("GET", ruta, null, token);
    }

    private RespuestaHttp enviar(String metodo, String ruta, String cuerpo, String token) throws IOException {
        HttpURLConnection conexion = (HttpURLConnection) new URL(urlBase + ruta).openConnection();
        try {
            conexion.setRequestMethod(metodo);
            conexion.setConnectTimeout(TIMEOUT_MS);
            conexion.setReadTimeout(TIMEOUT_MS);
            conexion.setRequestProperty("Accept", "application/json");

            if (token != null && !token.isEmpty()) {
                conexion.setRequestProperty("Authorization", "Bearer " + token);
            }
            if (cuerpo != null) {
                conexion.setDoOutput(true);
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream salida = conexion.getOutputStream()) {
                    salida.write(cuerpo.getBytes(StandardCharsets.UTF_8));
                }
            }

            int estado = conexion.getResponseCode();
            // Los errores (4xx/5xx) llegan en el error stream.
            InputStream flujo = estado >= 400 ? conexion.getErrorStream() : conexion.getInputStream();
            return new RespuestaHttp(estado, leer(flujo));
        } finally {
            conexion.disconnect();
        }
    }

    private String leer(InputStream flujo) throws IOException {
        if (flujo == null) {
            return "";
        }
        StringBuilder texto = new StringBuilder();
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(flujo, StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                texto.append(linea);
            }
        }
        return texto.toString();
    }
}
