package com.grupo1.biometria.data;

/**
 * Estados posibles de una respuesta de autenticación.
 * Traduce el código HTTP del backend a un concepto de la app.
 */
public enum EstadoAuth {
    /** Operación completada correctamente. */
    OK,
    /** 400: el backend ha rechazado los datos enviados. */
    DATOS_INVALIDOS,
    /** 409: ya existe una cuenta con ese correo. */
    CORREO_REGISTRADO,
    /** 401 en login: correo o contraseña no válidos. */
    CREDENCIALES_INVALIDAS,
    /** 401 con token: la sesión no es válida o ha caducado. */
    SESION_INVALIDA,
    /** No se ha podido contactar con el servidor. */
    ERROR_RED,
    /** Respuesta inesperada o error del servidor (500, etc.). */
    ERROR_SERVIDOR
}
