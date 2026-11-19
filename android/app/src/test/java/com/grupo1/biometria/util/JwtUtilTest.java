package com.grupo1.biometria.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Tests de lectura del JWT (misma lógica que `expiracionMs()` de la web).
 */
public class JwtUtilTest {

    private static final long AHORA = 1_700_000_000_000L;

    /** Construye un JWT de prueba con la `exp` indicada (segundos UNIX). */
    private static String tokenConExp(Long expSegundos) {
        String cabecera = codificar("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String cuerpo = codificar(expSegundos == null
                ? "{\"id_usuario\":1,\"rol\":\"usuario\"}"
                : "{\"id_usuario\":1,\"rol\":\"usuario\",\"exp\":" + expSegundos + "}");
        return cabecera + "." + cuerpo + ".firma-de-prueba";
    }

    private static String codificar(String texto) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void devuelve_la_expiracion_en_milisegundos() {
        long expSegundos = AHORA / 1000 + 600;
        assertEquals(expSegundos * 1000L, (long) JwtUtil.expiracionMs(tokenConExp(expSegundos)));
    }

    @Test
    public void token_sin_exp_devuelve_null() {
        assertNull(JwtUtil.expiracionMs(tokenConExp(null)));
    }

    @Test
    public void token_basura_devuelve_null() {
        assertNull(JwtUtil.expiracionMs("no-es-un-token"));
        assertNull(JwtUtil.expiracionMs(""));
        assertNull(JwtUtil.expiracionMs(null));
        assertNull(JwtUtil.expiracionMs("a.b.c.d"));
    }

    @Test
    public void token_caducado_se_detecta() {
        String token = tokenConExp((AHORA / 1000) - 1);
        assertTrue(JwtUtil.expirado(token, AHORA));
    }

    @Test
    public void token_vigente_no_se_detecta_caducado() {
        String token = tokenConExp((AHORA / 1000) + 600);
        assertFalse(JwtUtil.expirado(token, AHORA));
    }

    @Test
    public void token_en_el_limite_de_expiracion_se_caduca() {
        String token = tokenConExp(AHORA / 1000);
        assertTrue(JwtUtil.expirado(token, AHORA));
    }
}
