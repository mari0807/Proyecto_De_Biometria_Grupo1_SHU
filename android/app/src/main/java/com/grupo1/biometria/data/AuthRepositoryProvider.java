package com.grupo1.biometria.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Punto de acceso único al repositorio de autenticación.
 * Patrón holder perezoso: todas las Activities comparten la misma instancia
 * y, con ella, la misma sesión.
 */
public final class AuthRepositoryProvider {

    private static volatile AuthRepository instancia;
    private static volatile String instanciaUrl;
    private static volatile SesionManager sesion;

    private AuthRepositoryProvider() {
    }

    /**
     * --------------------
     * contexto: Context --> obtener() --> repositorio: AuthRepository
     * --------------------
     */
    public static AuthRepository obtener(Context contexto) {
        // La URL base la decide el campo «Servidor» del login (UrlServidor):
        // cada miembro del equipo apunta la app al backend de su propio PC
        // sin recompilar. Si la URL cambia, se recrea el repositorio.
        String urlBase = UrlServidor.url(contexto);
        synchronized (AuthRepositoryProvider.class) {
            if (instancia == null || !urlBase.equals(instanciaUrl)) {
                instanciaUrl = urlBase;
                Context app = contexto.getApplicationContext();
                Handler hiloPrincipal = new Handler(Looper.getMainLooper());
                ExecutorService hiloRed = Executors.newSingleThreadExecutor();

                sesion = new SesionManager(new PreferenciasAlmacenSesion(app), System::currentTimeMillis);
                instancia = new AuthRepositoryRemoto(
                        new HttpClienteRest(urlBase),
                        sesion,
                        hiloRed,
                        hiloPrincipal::post);
            }
        }
        return instancia;
    }

    /**
     * --------------------
     * --> sesion() --> sesion: SesionManager | NULL
     * --------------------
     *
     * La sesión compartida que usa el repositorio; el mapa la necesita para
     * adjuntar el token del usuario a `GET /api/mapa`.
     */
    public static SesionManager sesion() {
        return sesion;
    }
}
