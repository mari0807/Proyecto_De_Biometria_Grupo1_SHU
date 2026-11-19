package com.grupo1.biometria.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.grupo1.biometria.data.Usuario;

import org.junit.Test;

/** Tests del enrutamiento por rol (mismo criterio que `destino()` en la web). */
public class RolTest {

    private static Usuario usuarioConRol(String rol) {
        return new Usuario(1, "Ana", "García", "ana@ejemplo.com", rol, "");
    }

    @Test
    public void rol_admin_es_administrador() {
        assertTrue(Rol.esAdmin("admin"));
        assertTrue(Rol.esAdmin(usuarioConRol("admin")));
    }

    @Test
    public void mayusculas_y_espacios_no_alteran_el_rol() {
        assertTrue(Rol.esAdmin(" ADMIN "));
        assertTrue(Rol.esAdmin(usuarioConRol("Admin")));
    }

    @Test
    public void rol_usuario_no_es_administrador() {
        assertFalse(Rol.esAdmin("usuario"));
        assertFalse(Rol.esAdmin(usuarioConRol("usuario")));
    }

    @Test
    public void rol_ausente_no_es_administrador() {
        assertFalse(Rol.esAdmin((String) null));
        assertFalse(Rol.esAdmin((Usuario) null));
        assertFalse(Rol.esAdmin(""));
    }
}
