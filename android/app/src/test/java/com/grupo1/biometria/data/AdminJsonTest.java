package com.grupo1.biometria.data;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdminJsonTest {
    @Test
    public void parseaElListadoDeUsuarios() throws Exception {
        String json = "{\"ok\":true,\"mensaje\":\"Listado de usuarios.\",\"usuarios\":["
                + "{\"id_usuario\":1,\"nombre\":\"Admin\",\"apellidos\":\"QueAire\","
                + "\"email\":\"admin@email.com\",\"rol\":\"admin\",\"activo\":true},"
                + "{\"id_usuario\":10,\"nombre\":\"Prueba\",\"apellidos\":\"App\","
                + "\"email\":\"prueba@email.com\",\"rol\":\"usuario\",\"activo\":false}]}";

        List<UsuarioAdmin> usuarios = AdminJson.usuarios(json);
        assertEquals(2, usuarios.size());
        assertEquals(1, usuarios.get(0).getId());
        assertEquals("admin@email.com", usuarios.get(0).getEmail());
        assertEquals("admin", usuarios.get(0).getRol());
        assertTrue(usuarios.get(0).isActivo());
        assertFalse(usuarios.get(1).isActivo());
        assertTrue(AdminJson.ok(json));
    }

    @Test
    public void activoPorDefectoCuandoFalta() throws Exception {
        String json = "{\"ok\":true,\"usuarios\":[{\"id_usuario\":3,\"nombre\":\"Cliente\","
                + "\"apellidos\":\"QueAire\",\"email\":\"cliente@email.com\",\"rol\":\"usuario\"}]}";

        assertTrue(AdminJson.usuarios(json).get(0).isActivo());
    }

    @Test
    public void extraeLaPasswordTemporal() {
        String json = "{\"ok\":true,\"mensaje\":\"Usuario creado.\",\"password_temporal\":\"Temp0!aB\"}";

        assertEquals("Temp0!aB", AdminJson.passwordTemporal(json));
        assertEquals("", AdminJson.passwordTemporal("{\"ok\":true}"));
    }

    @Test
    public void respuestasDeErrorSeTraducenAMensaje() {
        assertEquals("No puedes borrar tu propia cuenta desde aquí.",
                AdminJson.mensaje("{\"ok\":false,\"mensaje\":\"No puedes borrar tu propia cuenta desde aquí.\"}",
                        "error"));
        assertFalse(AdminJson.ok("{\"ok\":false}"));
    }
}
