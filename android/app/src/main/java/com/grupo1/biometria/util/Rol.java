package com.grupo1.biometria.util;

import com.grupo1.biometria.data.Usuario;

/**
 * Comprobación del rol que devuelve el backend en `usuario.rol`.
 *
 * El servidor solo trabaja con dos valores (`admin` y `usuario`, ver
 * `Autenticacion.php` y `acceso.js` de la web). Se compara sin distinguir
 * mayúsculas y se ignoran espacios para no depender del formato del JSON.
 */
public final class Rol {

    public static final String ADMIN = "admin";

    private Rol() {
        // Solo métodos estáticos.
    }

    /**
     * --------------------
     * usuario: Usuario? --> es_admin() --> B
     * --------------------
     */
    public static boolean esAdmin(Usuario usuario) {
        return usuario != null && esAdmin(usuario.rol);
    }

    /**
     * --------------------
     * rol: Text? --> es_admin() --> B
     * --------------------
     */
    public static boolean esAdmin(String rol) {
        return rol != null && ADMIN.equalsIgnoreCase(rol.trim());
    }
}
