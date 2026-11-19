package com.grupo1.biometria.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Tests de la sesión: persistencia ("mantener sesión") y caducidad del JWT.
 */
public class SesionManagerTest {

    private static final long AHORA = 1_700_000_000_000L;

    private static final Usuario USUARIO =
            new Usuario(1, "Ana", "García", "ana@ejemplo.com", "usuario", "");

    /** Almacenamiento en memoria (doble de prueba). */
    private static class AlmacenMemoria implements AlmacenSesion {
        private String token;
        private String usuarioJson;

        @Override
        public void guardar(String token, String usuarioJson) {
            this.token = token;
            this.usuarioJson = usuarioJson;
        }

        @Override
        public String token() {
            return token;
        }

        @Override
        public String usuarioJson() {
            return usuarioJson;
        }

        @Override
        public void limpiar() {
            token = null;
            usuarioJson = null;
        }
    }

    /** Reloj fijo de prueba. */
    private static class RelojFijo implements Reloj {
        long momento = AHORA;

        @Override
        public long ahoraMs() {
            return momento;
        }
    }

    private static String tokenConExp(long expSegundos) {
        String cabecera = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String cuerpo = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"exp\":" + expSegundos + "}").getBytes(StandardCharsets.UTF_8));
        return cabecera + "." + cuerpo + ".firma";
    }

    private final AlmacenMemoria almacen = new AlmacenMemoria();
    private final RelojFijo reloj = new RelojFijo();

    // ---------- Apertura de sesión ----------

    @Test
    public void abrir_con_mantener_persiste_en_almacen() {
        SesionManager sesion = new SesionManager(almacen, reloj);

        sesion.abrir(tokenConExp(AHORA / 1000 + 600), USUARIO, true);

        assertTrue(sesion.haySesion());
        assertNotNull(almacen.token());
        assertEquals("ana@ejemplo.com", sesion.usuario().email);
    }

    @Test
    public void abrir_sin_mantener_no_persiste_pero_sigue_activa() {
        SesionManager sesion = new SesionManager(almacen, reloj);

        sesion.abrir(tokenConExp(AHORA / 1000 + 600), USUARIO, false);

        assertTrue("La sesión sigue viva mientras la app abierta", sesion.haySesion());
        assertNull("No debe guardarse en preferencias", almacen.token());
    }

    // ---------- Restauración al arrancar la app ----------

    @Test
    public void la_sesion_persistida_se_restaura_al_arrancar() {
        SesionManager original = new SesionManager(almacen, reloj);
        original.abrir(tokenConExp(AHORA / 1000 + 600), USUARIO, true);

        // Nueva instancia = app relanzada.
        SesionManager relanzada = new SesionManager(almacen, reloj);

        assertTrue(relanzada.haySesion());
        assertEquals("ana@ejemplo.com", relanzada.usuario().email);
    }

    @Test
    public void sin_mantener_no_hay_sesion_tras_relanzar() {
        SesionManager original = new SesionManager(almacen, reloj);
        original.abrir(tokenConExp(AHORA / 1000 + 600), USUARIO, false);

        SesionManager relanzada = new SesionManager(almacen, reloj);

        assertFalse(relanzada.haySesion());
        assertNull(relanzada.usuario());
    }

    // ---------- Caducidad ----------

    @Test
    public void un_token_caducado_no_se_restaura() {
        SesionManager original = new SesionManager(almacen, reloj);
        original.abrir(tokenConExp(AHORA / 1000 - 1), USUARIO, true);

        SesionManager relanzada = new SesionManager(almacen, reloj);

        assertFalse(relanzada.haySesion());
        assertNull("El token caducado se limpia del almacen", almacen.token());
    }

    @Test
    public void la_sesion_caduca_con_el_paso_del_tiempo() {
        SesionManager sesion = new SesionManager(almacen, reloj);
        sesion.abrir(tokenConExp(AHORA / 1000 + 600), USUARIO, true);
        assertTrue(sesion.haySesion());

        // Pasan 10 minutos (el TTL del backend).
        reloj.momento = AHORA + 600_000L;

        assertFalse(sesion.haySesion());
        assertNull(sesion.usuario());
    }

    @Test
    public void datos_corruptos_en_almacen_no_provocan_excepcion() {
        almacen.guardar(tokenConExp(AHORA / 1000 + 600), "no-es-json");

        SesionManager sesion = new SesionManager(almacen, reloj);

        assertFalse(sesion.haySesion());
        assertNull(almacen.token());
    }

    // ---------- Cierre ----------

    @Test
    public void cerrar_borra_sesion_memoria_y_almacen() {
        SesionManager sesion = new SesionManager(almacen, reloj);
        sesion.abrir(tokenConExp(AHORA / 1000 + 600), USUARIO, true);

        sesion.cerrar();

        assertFalse(sesion.haySesion());
        assertNull(sesion.usuario());
        assertNull(sesion.token());
        assertNull(almacen.token());
    }

    @Test
    public void actualizar_usuario_con_sesion_activa_persiste_el_cambio() {
        SesionManager sesion = new SesionManager(almacen, reloj);
        sesion.abrir(tokenConExp(AHORA / 1000 + 600), USUARIO, true);

        sesion.actualizarUsuario(new Usuario(1, "Ana", "García", "nuevo@ejemplo.com", "usuario", ""));

        assertEquals("nuevo@ejemplo.com", sesion.usuario().email);
        // El usuario renovado también se guarda en preferencias.
        SesionManager relanzada = new SesionManager(almacen, reloj);
        assertEquals("nuevo@ejemplo.com", relanzada.usuario().email);
    }
}
