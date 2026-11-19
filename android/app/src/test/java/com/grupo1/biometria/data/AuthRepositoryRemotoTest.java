package com.grupo1.biometria.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests del repositorio de autenticación con un {@link ClienteRest} falso:
 * se comprueba el contrato con la API (rutas, cuerpos y sesión) sin red.
 *
 * Los executors son directos (ejecutan en el mismo hilo), por lo que las
 * respuestas están disponibles al terminar cada llamada.
 */
public class AuthRepositoryRemotoTest {

    private static final long AHORA = 1_700_000_000_000L;

    private static final String LOGIN_OK = "{\"ok\":true,\"mensaje\":\"Sesión iniciada.\","
            + "\"usuario\":{\"id_usuario\":1,\"nombre\":\"Ana\",\"apellidos\":\"García\","
            + "\"email\":\"ana@ejemplo.com\",\"rol\":\"usuario\",\"foto\":\"\"},"
            + "\"token\":\"" + tokenVigente() + "\"}";

    private static String tokenVigente() {
        long exp = (AHORA / 1000) + 600;
        String cabecera = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String cuerpo = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"exp\":" + exp + "}").getBytes(StandardCharsets.UTF_8));
        return cabecera + "." + cuerpo + ".firma";
    }

    /** Cliente falso que registra la última petición y devuelve la prevista. */
    private static class ClienteFalso implements ClienteRest {
        RespuestaHttp proxima = new RespuestaHttp(200, "{}");
        /** Cuando es {@code true} todas las llamadas fallan como sin cobertura. */
        boolean sinRed;
        String ultimaRuta;
        String ultimoCuerpo;
        String ultimoToken;
        String ultimoMetodo;

        @Override
        public RespuestaHttp postJson(String ruta, String cuerpo, String token) throws IOException {
            if (sinRed) {
                throw new IOException("sin red");
            }
            ultimoMetodo = "POST";
            ultimaRuta = ruta;
            ultimoCuerpo = cuerpo;
            ultimoToken = token;
            return proxima;
        }

        @Override
        public RespuestaHttp getJson(String ruta, String token) throws IOException {
            if (sinRed) {
                throw new IOException("sin red");
            }
            ultimoMetodo = "GET";
            ultimaRuta = ruta;
            ultimoToken = token;
            return proxima;
        }
    }

    /** Cliente que siempre falla la conexión. */
    private static class ClienteSinRed implements ClienteRest {
        @Override
        public RespuestaHttp postJson(String ruta, String cuerpo, String token) throws IOException {
            throw new IOException("sin red");
        }

        @Override
        public RespuestaHttp getJson(String ruta, String token) throws IOException {
            throw new IOException("sin red");
        }
    }

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

    private final ClienteFalso cliente = new ClienteFalso();
    private final AlmacenMemoria almacen = new AlmacenMemoria();

    private AuthRepositoryRemoto crearRepositorio() {
        return new AuthRepositoryRemoto(
                cliente,
                new SesionManager(almacen, () -> AHORA),
                Runnable::run,   // hilo "de red" directo
                Runnable::run);  // hilo "UI" directo
    }

    private static RespuestaAuth pedir(java.util.function.Consumer<AuthCallback> operacion) {
        AtomicReference<RespuestaAuth> recibida = new AtomicReference<>();
        operacion.accept(recibida::set);
        RespuestaAuth respuesta = recibida.get();
        assertNotNull("El callback debe entregarse siempre", respuesta);
        return respuesta;
    }

    // ---------- Login ----------

    @Test
    public void login_correcto_llama_a_api_login_y_guarda_sesion() {
        cliente.proxima = new RespuestaHttp(200, LOGIN_OK);
        AuthRepositoryRemoto repositorio = crearRepositorio();

        RespuestaAuth r = pedir(cb -> repositorio.iniciarSesion("ana@ejemplo.com", "Abcdef1!", true, cb));

        assertEquals(EstadoAuth.OK, r.estado);
        assertEquals("POST", cliente.ultimoMetodo);
        assertEquals("/api/login", cliente.ultimaRuta);
        assertTrue(cliente.ultimoCuerpo.contains("ana@ejemplo.com"));
        assertTrue(repositorio.haySesion());
        assertEquals("ana@ejemplo.com", repositorio.usuario().email);
        assertNotNull(almacen.token());
    }

    @Test
    public void login_sin_mantener_no_persiste_la_sesion() {
        cliente.proxima = new RespuestaHttp(200, LOGIN_OK);
        AuthRepositoryRemoto repositorio = crearRepositorio();

        pedir(cb -> repositorio.iniciarSesion("ana@ejemplo.com", "Abcdef1!", false, cb));

        assertTrue(repositorio.haySesion());
        assertNull("No debe guardarse en preferencias", almacen.token());
    }

    @Test
    public void login_con_credenciales_invalidas_no_abre_sesion() {
        cliente.proxima = new RespuestaHttp(401, "{\"ok\":false,"
                + "\"mensaje\":\"Correo o contraseña incorrectos.\"}");
        AuthRepositoryRemoto repositorio = crearRepositorio();

        RespuestaAuth r = pedir(cb -> repositorio.iniciarSesion("ana@ejemplo.com", "mala", true, cb));

        assertEquals(EstadoAuth.CREDENCIALES_INVALIDAS, r.estado);
        assertFalse(repositorio.haySesion());
        assertNull(almacen.token());
    }

    @Test
    public void login_sin_red_da_error_de_red_y_no_crash() {
        AuthRepositoryRemoto repositorio = new AuthRepositoryRemoto(
                new ClienteSinRed(),
                new SesionManager(almacen, () -> AHORA),
                Runnable::run,
                Runnable::run);

        RespuestaAuth r = pedir(cb -> repositorio.iniciarSesion("a@b.com", "Abcdef1!", true, cb));

        assertEquals(EstadoAuth.ERROR_RED, r.estado);
        assertFalse(repositorio.haySesion());
    }

    // ---------- Registro ----------

    @Test
    public void registro_llama_a_api_registro_con_nombre_y_apellidos() {
        cliente.proxima = new RespuestaHttp(200, "{\"ok\":true,"
                + "\"mensaje\":\"Cuenta creada correctamente.\","
                + "\"usuario\":{\"id_usuario\":2,\"nombre\":\"Luis\",\"apellidos\":\"Pérez\","
                + "\"email\":\"luis@ejemplo.com\",\"rol\":\"usuario\",\"foto\":\"\"}}");
        AuthRepositoryRemoto repositorio = crearRepositorio();

        RespuestaAuth r = pedir(cb ->
                repositorio.registrar("Luis", "Pérez", "luis@ejemplo.com", "Abcdef1!", cb));

        assertEquals(EstadoAuth.OK, r.estado);
        assertEquals("/api/registro", cliente.ultimaRuta);
        assertTrue(cliente.ultimoCuerpo.contains("\"nombre\""));
        assertTrue(cliente.ultimoCuerpo.contains("Pérez"));
        // El registro no inicia sesión: el backend no devuelve token.
        assertFalse(repositorio.haySesion());
    }

    @Test
    public void registro_con_correo_repetido_devuelve_correo_registrado() {
        cliente.proxima = new RespuestaHttp(409, "{\"ok\":false,"
                + "\"mensaje\":\"Ese correo ya está registrado.\"}");
        AuthRepositoryRemoto repositorio = crearRepositorio();

        RespuestaAuth r = pedir(cb ->
                repositorio.registrar("Luis", "Pérez", "luis@ejemplo.com", "Abcdef1!", cb));

        assertEquals(EstadoAuth.CORREO_REGISTRADO, r.estado);
        assertEquals("Ese correo ya está registrado.", r.mensaje);
    }

    // ---------- Sesión ----------

    @Test
    public void comprobar_sesion_sin_token_devuelve_sesion_invalida() {
        AuthRepositoryRemoto repositorio = crearRepositorio();

        RespuestaAuth r = pedir(repositorio::comprobarSesion);

        assertEquals(EstadoAuth.SESION_INVALIDA, r.estado);
        assertNull(cliente.ultimaRuta);
    }

    @Test
    public void comprobar_sesion_valida_actualiza_el_usuario() {
        cliente.proxima = new RespuestaHttp(200, LOGIN_OK);
        AuthRepositoryRemoto repositorio = crearRepositorio();
        pedir(cb -> repositorio.iniciarSesion("ana@ejemplo.com", "Abcdef1!", true, cb));
        assertTrue(repositorio.haySesion());

        // A partir de aquí el servidor responde al GET /api/sesion con el usuario renovado.
        cliente.proxima = new RespuestaHttp(200, "{\"ok\":true,"
                + "\"mensaje\":\"Sesión válida.\","
                + "\"usuario\":{\"id_usuario\":1,\"nombre\":\"Ana\",\"apellidos\":\"García\","
                + "\"email\":\"actualizado@ejemplo.com\",\"rol\":\"usuario\",\"foto\":\"\"}}");

        RespuestaAuth r = pedir(repositorio::comprobarSesion);

        assertEquals(EstadoAuth.OK, r.estado);
        assertEquals("GET", cliente.ultimoMetodo);
        assertEquals("/api/sesion", cliente.ultimaRuta);
        assertNotNull("Debe llevar el Bearer", cliente.ultimoToken);
        assertEquals("actualizado@ejemplo.com", repositorio.usuario().email);
    }

    @Test
    public void sesion_rechazada_por_el_servidor_se_borra_localmente() {
        cliente.proxima = new RespuestaHttp(200, LOGIN_OK);
        AuthRepositoryRemoto repositorio = crearRepositorio();
        pedir(cb -> repositorio.iniciarSesion("ana@ejemplo.com", "Abcdef1!", true, cb));
        assertTrue(repositorio.haySesion());

        // El servidor rechaza el token en GET /api/sesion.
        cliente.proxima = new RespuestaHttp(401, "{\"ok\":false,"
                + "\"mensaje\":\"La sesión no es válida.\"}");

        RespuestaAuth r = pedir(repositorio::comprobarSesion);

        assertEquals(EstadoAuth.SESION_INVALIDA, r.estado);
        assertFalse(repositorio.haySesion());
        assertNull(almacen.token());
    }

    // ---------- Cierre de sesión ----------

    @Test
    public void cerrar_sesion_borra_lo_local_y_avisa_al_servidor() {
        cliente.proxima = new RespuestaHttp(200, LOGIN_OK);
        AuthRepositoryRemoto repositorio = crearRepositorio();
        pedir(cb -> repositorio.iniciarSesion("ana@ejemplo.com", "Abcdef1!", true, cb));
        assertTrue(repositorio.haySesion());

        // El servidor responde al POST /api/logout.
        cliente.proxima = new RespuestaHttp(200, "{\"ok\":true,"
                + "\"mensaje\":\"Sesión cerrada.\"}");

        RespuestaAuth r = pedir(repositorio::cerrarSesion);

        assertEquals(EstadoAuth.OK, r.estado);
        assertFalse(repositorio.haySesion());
        assertNull(almacen.token());
        assertEquals("/api/logout", cliente.ultimaRuta);
        assertNotNull("Debe llevar el Bearer", cliente.ultimoToken);
    }

    @Test
    public void cerrar_sesion_sin_red_igualmente_cierra_lo_local() {
        cliente.proxima = new RespuestaHttp(200, LOGIN_OK);
        AuthRepositoryRemoto repositorio = crearRepositorio();
        pedir(cb -> repositorio.iniciarSesion("ana@ejemplo.com", "Abcdef1!", true, cb));
        assertTrue(repositorio.haySesion());

        // El servidor deja de responder: la credencial local se borra antes.
        cliente.sinRed = true;
        RespuestaAuth r = pedir(repositorio::cerrarSesion);

        assertEquals(EstadoAuth.OK, r.estado);
        assertFalse(repositorio.haySesion());
        assertNull(almacen.token());
    }
}
