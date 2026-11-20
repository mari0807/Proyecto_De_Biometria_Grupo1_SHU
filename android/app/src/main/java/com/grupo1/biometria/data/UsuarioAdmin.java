package com.grupo1.biometria.data;

/**
 * Usuario del listado de administración (`GET /api/usuarios`).
 *
 * Coincide con lo que devuelve `listarUsuarios` de `Autenticacion.php`:
 * nunca incluye `password_hash`; `activo` es la baja lógica de la cuenta.
 * GR: Inmutable; la UI solo lo lee.
 */
public final class UsuarioAdmin {

    private final int id;
    private final String nombre;
    private final String apellidos;
    private final String email;
    private final String rol;
    private final boolean activo;

    /** -------------------- datos API --> UsuarioAdmin -------------------- */
    public UsuarioAdmin(int id, String nombre, String apellidos, String email, String rol, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.email = email;
        this.rol = rol;
        this.activo = activo;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getEmail() {
        return email;
    }

    public String getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }
}
