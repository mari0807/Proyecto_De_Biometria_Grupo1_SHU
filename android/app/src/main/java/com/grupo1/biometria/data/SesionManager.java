package com.grupo1.biometria.data;

import com.grupo1.biometria.util.JwtUtil;

/**
 * Sesión de la aplicación: token JWT + usuario público.
 *
 * Reglas (las mismas que `logicaProxy.js` de la web):
 * - La sesión caduca con el token (TTL del backend) y se limpia al detectarlo.
 * - "Mantener sesión" decide si se persiste en {@link AlmacenSesion}; sin él,
 *   la sesión solo vive mientras la app está abierta.
 *
 * Thread-safety: todo el estado está en campos con acceso `synchronized`.
 */
public class SesionManager {

    private final AlmacenSesion almacen;
    private final Reloj reloj;

    private String token;
    private Usuario usuario;

    public SesionManager(AlmacenSesion almacen, Reloj reloj) {
        this.almacen = almacen;
        this.reloj = reloj;
        restaurar();
    }

    /**
     * --------------------
     * --> restaurar() -->
     * --------------------
     *
     * Carga la sesión persistida si existe y no ha caducado; en caso
     * contrario borra lo que hubiera (token caducado o datos incoherentes).
     */
    private synchronized void restaurar() {
        String tokenGuardado = almacen.token();
        String usuarioGuardado = almacen.usuarioJson();
        if (tokenGuardado == null || usuarioGuardado == null) {
            almacen.limpiar();
            return;
        }
        if (JwtUtil.expirado(tokenGuardado, reloj.ahoraMs())) {
            almacen.limpiar();
            return;
        }
        Usuario parseado = Usuario.desdeJson(usuarioGuardado);
        if (parseado == null) {
            almacen.limpiar();
            return;
        }
        this.token = tokenGuardado;
        this.usuario = parseado;
    }

    /**
     * --------------------
     * token: Text, usuario: Usuario, mantener: B --> abrir() -->
     * --------------------
     */
    public synchronized void abrir(String token, Usuario usuario, boolean mantener) {
        this.token = token;
        this.usuario = usuario;
        if (mantener) {
            almacen.guardar(token, usuario.aJson());
        } else {
            // Sesión solo en memoria: no sobrevive al cierre de la app.
            almacen.limpiar();
        }
    }

    /**
     * --------------------
     * usuario: Usuario --> actualizar_usuario() -->
     * --------------------
     *
     * Refresca los datos públicos sin tocar el token (p. ej. al comprobar sesión).
     */
    public synchronized void actualizarUsuario(Usuario usuario) {
        if (this.usuario == null) {
            return;
        }
        this.usuario = usuario;
        if (almacen.token() != null) {
            almacen.guardar(this.token, usuario.aJson());
        }
    }

    /**
     * --------------------
     * --> cerrar() -->
     * --------------------
     */
    public synchronized void cerrar() {
        token = null;
        usuario = null;
        almacen.limpiar();
    }

    /**
     * --------------------
     * --> hay_sesion() --> B
     * --------------------
     *
     * @return {@code true} si hay token y no ha caducado según el reloj local.
     *         El servidor es quien valida la firma con `GET /api/sesion`.
     */
    public synchronized boolean haySesion() {
        return token != null && usuario != null && !JwtUtil.expirado(token, reloj.ahoraMs());
    }

    /**
     * --------------------
     * --> token() --> token: Text
     * --------------------
     *
     * @return el token o {@code null} si no hay sesión activa.
     */
    public synchronized String token() {
        return haySesion() ? token : null;
    }

    /**
     * --------------------
     * --> usuario() --> usuario: Usuario
     * --------------------
     *
     * @return el usuario en sesión o {@code null} si no hay.
     */
    public synchronized Usuario usuario() {
        return haySesion() ? usuario : null;
    }
}
