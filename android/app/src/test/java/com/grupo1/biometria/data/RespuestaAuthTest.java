package com.grupo1.biometria.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Tests de traducción de las respuestas HTTP de la API a estados de la app.
 * Los cuerpos JSON son los que devuelve realmente `ApiAuth.php`.
 */
public class RespuestaAuthTest {

    private static final String LOGIN_OK = "{\"ok\":true,\"mensaje\":\"Sesión iniciada.\","
            + "\"usuario\":{\"id_usuario\":1,\"nombre\":\"Ana\",\"apellidos\":\"García\","
            + "\"email\":\"ana@ejemplo.com\",\"rol\":\"usuario\",\"foto\":\"\"},"
            + "\"token\":\"cabeza.cuerpo.firma\"}";

    private static final String CREDENCIALES = "{\"ok\":false,"
            + "\"mensaje\":\"Correo o contraseña incorrectos.\","
            + "\"usuario\":{\"id_usuario\":0,\"nombre\":\"\",\"apellidos\":\"\","
            + "\"email\":\"\",\"rol\":\"\",\"foto\":\"\"},\"token\":\"\"}";

    private static final String CORREO_REGISTRADO = "{\"ok\":false,"
            + "\"mensaje\":\"Ese correo ya está registrado.\"}";

    private static final String REGISTRO_OK = "{\"ok\":true,"
            + "\"mensaje\":\"Cuenta creada correctamente.\","
            + "\"usuario\":{\"id_usuario\":2,\"nombre\":\"Luis\",\"apellidos\":\"Pérez\","
            + "\"email\":\"luis@ejemplo.com\",\"rol\":\"usuario\",\"foto\":\"\"}}";

    private static final String SESION_OK = "{\"ok\":true,\"mensaje\":\"Sesión válida.\","
            + "\"usuario\":{\"id_usuario\":1,\"nombre\":\"Ana\",\"apellidos\":\"García\","
            + "\"email\":\"ana@ejemplo.com\",\"rol\":\"usuario\",\"foto\":\"\"}}";

    // ---------- Login ----------

    @Test
    public void login_con_200_devuelve_ok_con_token_y_usuario() {
        RespuestaAuth r = RespuestaAuth.deLogin(200, LOGIN_OK);

        assertEquals(EstadoAuth.OK, r.estado);
        assertEquals("cabeza.cuerpo.firma", r.token);
        assertNotNull(r.usuario);
        assertEquals("ana@ejemplo.com", r.usuario.email);
        assertEquals("Ana García", r.usuario.nombreCompleto());
    }

    @Test
    public void login_con_401_son_credenciales_incorrectas() {
        RespuestaAuth r = RespuestaAuth.deLogin(401, CREDENCIALES);

        assertEquals(EstadoAuth.CREDENCIALES_INVALIDAS, r.estado);
        assertEquals("Correo o contraseña incorrectos.", r.mensaje);
        assertNull(r.usuario);
    }

    @Test
    public void login_con_400_son_datos_invalidos() {
        RespuestaAuth r = RespuestaAuth.deLogin(400, "{\"ok\":false,\"mensaje\":\"Datos inválidos.\"}");

        assertEquals(EstadoAuth.DATOS_INVALIDOS, r.estado);
        assertEquals("Datos inválidos.", r.mensaje);
    }

    @Test
    public void login_sin_respuesta_del_servidor_es_error_de_servidor() {
        RespuestaAuth r = RespuestaAuth.deLogin(500, "<html>error</html>");

        assertEquals(EstadoAuth.ERROR_SERVIDOR, r.estado);
        assertNotNull(r.mensaje);
    }

    // ---------- Registro ----------

    @Test
    public void registro_con_200_devuelve_ok() {
        RespuestaAuth r = RespuestaAuth.deRegistro(200, REGISTRO_OK);

        assertEquals(EstadoAuth.OK, r.estado);
        assertNotNull(r.usuario);
        assertEquals("Luis Pérez", r.usuario.nombreCompleto());
    }

    @Test
    public void registro_con_409_es_correo_registrado() {
        RespuestaAuth r = RespuestaAuth.deRegistro(409, CORREO_REGISTRADO);

        assertEquals(EstadoAuth.CORREO_REGISTRADO, r.estado);
        assertEquals("Ese correo ya está registrado.", r.mensaje);
    }

    @Test
    public void registro_con_400_es_datos_invalidos() {
        RespuestaAuth r = RespuestaAuth.deRegistro(400,
                "{\"ok\":false,\"mensaje\":\"La contraseña debe tener al menos 8 caracteres.\"}");

        assertEquals(EstadoAuth.DATOS_INVALIDOS, r.estado);
        assertEquals("La contraseña debe tener al menos 8 caracteres.", r.mensaje);
    }

    // ---------- Sesión ----------

    @Test
    public void sesion_con_200_devuelve_ok_con_usuario() {
        RespuestaAuth r = RespuestaAuth.deSesion(200, SESION_OK);

        assertEquals(EstadoAuth.OK, r.estado);
        assertNotNull(r.usuario);
        assertEquals("ana@ejemplo.com", r.usuario.email);
    }

    @Test
    public void sesion_con_401_es_sesion_invalida() {
        RespuestaAuth r = RespuestaAuth.deSesion(401,
                "{\"ok\":false,\"mensaje\":\"La sesión no es válida.\"}");

        assertEquals(EstadoAuth.SESION_INVALIDA, r.estado);
        assertEquals("La sesión no es válida.", r.mensaje);
    }

    // ---------- Red ----------

    @Test
    public void fallo_de_red_produce_estado_de_red() {
        RespuestaAuth r = RespuestaAuth.deRed("");

        assertEquals(EstadoAuth.ERROR_RED, r.estado);
    }
}
