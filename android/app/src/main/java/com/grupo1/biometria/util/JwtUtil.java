package com.grupo1.biometria.util;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Lectura del token JWT emitido por el backend.
 * Mismo criterio que `expiracionMs()` de la web (`logicaProxy.js`):
 * se decodifica el payload (parte central) y se lee `exp` (segundos UNIX).
 */
public final class JwtUtil {

    private JwtUtil() {
    }

    /**
     * --------------------
     * token: Text --> expiracion_ms() --> expiracion: N
     * --------------------
     *
     * @return el instante de caducidad en milisegundos, o {@code null} si el
     *         token no tiene el formato esperado o no lleva `exp`.
     */
    public static Long expiracionMs(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String[] partes = token.split("\\.");
        if (partes.length != 3 || partes[1].isEmpty()) {
            return null;
        }
        try {
            byte[] decodificado = Base64.getUrlDecoder().decode(partes[1]);
            JSONObject cuerpo = new JSONObject(new String(decodificado, StandardCharsets.UTF_8));
            if (!cuerpo.has("exp")) {
                return null;
            }
            return cuerpo.getLong("exp") * 1000L;
        } catch (Exception e) {
            // Base64 o JSON inválido: el token no es utilizable.
            return null;
        }
    }

    /**
     * --------------------
     * token: Text, ahora_ms: N --> expirado() --> B
     * --------------------
     *
     * Un token sin `exp` legible se considera no caducado (mismo criterio que
     * la web): quien decide en último término es el servidor con `/api/sesion`.
     */
    public static boolean expirado(String token, long ahoraMs) {
        Long expiracion = expiracionMs(token);
        return expiracion != null && expiracion <= ahoraMs;
    }
}
