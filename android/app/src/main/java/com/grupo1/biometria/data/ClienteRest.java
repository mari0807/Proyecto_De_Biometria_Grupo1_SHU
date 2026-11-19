package com.grupo1.biometria.data;

import java.io.IOException;

/**
 * Transporte HTTP de la API. Interfaz para poder sustituirlo por un doble de
 * pruebas en los tests unitarios sin tocar la red.
 *
 * Todas las operaciones son bloqueantes: deben llamarse desde un hilo de
 * fondo, nunca desde el hilo principal (Android lanza `NetworkOnMainThreadException`).
 */
public interface ClienteRest {

    /**
     * --------------------
     * ruta: Text, cuerpo: Text?, token: Text? --> post_json() --> respuesta: RespuestaHttp
     * --------------------
     *
     * @param cuerpo JSON a enviar o {@code null} sin cuerpo (p. ej. logout).
     * @param token  Bearer a añadir o {@code null}/vacío si no hay sesión.
     */
    RespuestaHttp postJson(String ruta, String cuerpo, String token) throws IOException;

    /**
     * --------------------
     * ruta: Text, token: Text? --> get_json() --> respuesta: RespuestaHttp
     * --------------------
     */
    RespuestaHttp getJson(String ruta, String token) throws IOException;
}
