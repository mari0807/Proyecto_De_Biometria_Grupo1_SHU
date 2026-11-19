package com.grupo1.biometria.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.grupo1.biometria.BuildConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Punto de acceso único al repositorio de autenticación.
 * Patrón holder perezoso: todas las Activities comparten la misma instancia
 * y, con ella, la misma sesión.
 */
public final class AuthRepositoryProvider {

    private static volatile AuthRepository instancia;

    private AuthRepositoryProvider() {
    }

    /**
     * --------------------
     * contexto: Context --> obtener() --> repositorio: AuthRepository
     * --------------------
     */
    public static AuthRepository obtener(Context contexto) {
        if (instancia == null) {
            synchronized (AuthRepositoryProvider.class) {
                if (instancia == null) {
                    Context app = contexto.getApplicationContext();
                    Handler hiloPrincipal = new Handler(Looper.getMainLooper());
                    ExecutorService hiloRed = Executors.newSingleThreadExecutor();

                    instancia = new AuthRepositoryRemoto(
                            new HttpClienteRest(BuildConfig.API_BASE),
                            new SesionManager(new PreferenciasAlmacenSesion(app), System::currentTimeMillis),
                            hiloRed,
                            hiloPrincipal::post);
                }
            }
        }
        return instancia;
    }
}
