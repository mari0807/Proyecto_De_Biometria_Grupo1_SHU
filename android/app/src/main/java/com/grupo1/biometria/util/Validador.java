package com.grupo1.biometria.util;

/**
 * Validación de campos de los formularios.
 *
 * Reproduce las reglas del backend (`Autenticacion.php` y `password.js`) para
 * que el usuario vea el error antes de enviar la petición. Lógica pura
 * (sin Android) para que sea testeable con JUnit.
 */
public final class Validador {

    /** Regla del backend: mínimo 8 caracteres. */
    public static final int LONGITUD_MINIMA_PASSWORD = 8;
    /** Regla del backend: bcrypt admite como máximo 72 bytes. */
    public static final int LONGITUD_MAXIMA_PASSWORD = 72;

    private Validador() {
    }

    /**
     * --------------------
     * valor: Text --> esta_vacio() --> B
     * --------------------
     */
    public static boolean estaVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    /**
     * --------------------
     * email: Text --> email_valido() --> B
     * --------------------
     *
     * Mismo criterio que `emailValido()` del backend y que el regex de la web:
     * sin espacios, algo antes y después de la arroba y un punto en el dominio.
     */
    public static boolean emailValido(String email) {
        if (estaVacio(email)) {
            return false;
        }
        String limpio = email.trim();
        if (limpio.length() > 254 || limpio.matches(".*\\s.*")) {
            return false;
        }
        return limpio.matches("^[^@]+@[^@]+\\.[^@]+$");
    }

    /**
     * --------------------
     * password: Text --> error_password() --> mensaje: Text
     * --------------------
     *
     * @return {@code null} si la contraseña cumple la política del backend;
     *         en caso contrario, el mensaje que debe mostrar la UI.
     *         Política: 8–72 caracteres, mayúscula, minúscula, número y signo.
     */
    public static String errorPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "La contraseña es obligatoria.";
        }
        if (password.length() < LONGITUD_MINIMA_PASSWORD) {
            return "La contraseña no cumple: al menos 8 caracteres.";
        }
        if (password.length() > LONGITUD_MAXIMA_PASSWORD) {
            return "La contraseña no puede superar 72 caracteres.";
        }
        if (!password.matches(".*[A-ZÁÉÍÓÚÜÑ].*")) {
            return "La contraseña no cumple: al menos una mayúscula (a-z).";
        }
        if (!password.matches(".*[a-záéíóúüñ].*")) {
            return "La contraseña no cumple: al menos una minúscula (a-z).";
        }
        if (!password.matches(".*[0-9].*")) {
            return "La contraseña no cumple: al menos un número (0-9).";
        }
        // Signo especial: cualquier carácter que no sea letra ni número.
        boolean tieneSigno = false;
        for (int i = 0; i < password.length(); i++) {
            if (!Character.isLetterOrDigit(password.charAt(i))) {
                tieneSigno = true;
                break;
            }
        }
        if (!tieneSigno) {
            return "La contraseña no cumple: al menos un signo especial (!@#$…).";
        }
        return null;
    }

    /**
     * --------------------
     * password: Text --> estados_requisitos() --> B[5]
     * --------------------
     *
     * Devuelve, en orden, si la contraseña cumple: longitud (8–72),
     * mayúscula, minúscula, número y signo. Lo usa la pantalla de registro
     * para marcar cada requisito en vivo, igual que `password.js` en la web.
     */
    public static boolean[] requisitosPassword(String password) {
        String p = password == null ? "" : password;
        boolean tieneSigno = false;
        for (int i = 0; i < p.length(); i++) {
            if (!Character.isLetterOrDigit(p.charAt(i))) {
                tieneSigno = true;
                break;
            }
        }
        return new boolean[]{
                p.length() >= LONGITUD_MINIMA_PASSWORD && p.length() <= LONGITUD_MAXIMA_PASSWORD,
                p.matches(".*[A-ZÁÉÍÓÚÜÑ].*"),
                p.matches(".*[a-záéíóúüñ].*"),
                p.matches(".*[0-9].*"),
                tieneSigno
        };
    }

    /**
     * --------------------
     * password: Text --> password_valida() --> B
     * --------------------
     */
    public static boolean passwordValida(String password) {
        return errorPassword(password) == null;
    }
}
