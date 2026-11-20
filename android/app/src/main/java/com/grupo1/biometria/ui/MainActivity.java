package com.grupo1.biometria.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.grupo1.biometria.R;
import com.grupo1.biometria.data.AuthRepository;
import com.grupo1.biometria.data.AuthRepositoryProvider;
import com.grupo1.biometria.data.SesionManager;
import com.grupo1.biometria.data.UrlServidor;
import com.grupo1.biometria.mapa.CalorOverlay;
import com.grupo1.biometria.mapa.ClienteMapa;
import com.grupo1.biometria.mapa.DetalleZonaBottomSheet;
import com.grupo1.biometria.mapa.Formato;
import com.grupo1.biometria.mapa.LeyendaView;
import com.grupo1.biometria.mapa.PuntosOverlay;
import com.grupo1.biometria.mapa.model.PuntoMapa;
import com.grupo1.biometria.mapa.model.ZonaDetalle;
import com.grupo1.biometria.util.Rol;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.File;
import java.util.List;

/**
 * Pantalla principal tras autenticarse con rol «usuario»: el mapa de calidad
 * del aire (H16, portado de la primera app del grupo) más el cierre de sesión.
 *
 * Flujo: `GET /api/mapa` con el token de la sesión; al tocar un nodo se abre
 * el detalle de zona (`GET /api/mapa/zona/{id}`) en un panel inferior. Si el
 * token ya no es válido (401) se limpia la sesión local y se vuelve al login.
 */
public class MainActivity extends AppCompatActivity {
    private static final double CENTRO_LATITUD = 38.9748;
    private static final double CENTRO_LONGITUD = -0.1745;
    private static final int ZOOM_INICIAL = 13;

    private MapView mapa;
    private CalorOverlay capaCalor;
    private PuntosOverlay capaPuntos;
    private LeyendaView leyenda;
    private TextView estado;
    private TextView actualizacion;
    private ProgressBar cargando;
    private Button reintentar;

    private AuthRepository repositorio;
    private ClienteMapa cliente;

    /**
     * --------------------
     * savedInstanceState: Text? --> onCreate() -->
     * --------------------
     */
    @Override
    protected void onCreate(Bundle savedInstanceStateGuardado) {
        super.onCreate(savedInstanceStateGuardado);
        configurarOsmdroid();

        repositorio = AuthRepositoryProvider.obtener(this);

        // Sin sesión no se puede entrar aquí: se vuelve al inicio de sesión.
        if (!repositorio.haySesion()) {
            volverALogin();
            return;
        }
        // Un administrador nunca debe caer en la pantalla de usuario normal.
        if (Rol.esAdmin(repositorio.usuario())) {
            startActivity(new Intent(this, AdminActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        mapa = findViewById(R.id.mapa);
        leyenda = findViewById(R.id.mapa_leyenda);
        estado = findViewById(R.id.mapa_estado);
        actualizacion = findViewById(R.id.mapa_actualizacion);
        cargando = findViewById(R.id.mapa_cargando);
        reintentar = findViewById(R.id.mapa_reintentar);

        // La cabecera queda DEBAJO de la barra de estado (iconos de
        // notificaciones y cámara del móvil): se aplica como padding el alto
        // real que informe el sistema en cada dispositivo.
        View cabecera = findViewById(R.id.mapa_cabecera);
        ViewCompat.setOnApplyWindowInsetsListener(cabecera, (vista, insets) -> {
            int barra = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            vista.setPadding(
                    vista.getPaddingLeft(),
                    barra + dp(8),
                    vista.getPaddingRight(),
                    vista.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(cabecera);

        mapa.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        mapa.setMultiTouchControls(true);
        mapa.setMinZoomLevel(3.0);
        mapa.setMaxZoomLevel(19.0);
        mapa.getController().setZoom(ZOOM_INICIAL);
        mapa.getController().setCenter(new GeoPoint(CENTRO_LATITUD, CENTRO_LONGITUD));

        capaCalor = new CalorOverlay(this);
        capaPuntos = new PuntosOverlay(this, punto -> abrirDetalle(punto.getIdNodo()));
        mapa.getOverlays().add(capaCalor);
        mapa.getOverlays().add(capaPuntos);

        // La URL base es la misma que usa el login (campo «Servidor»).
        cliente = new ClienteMapa(UrlServidor.url(this), () -> {
            SesionManager sesion = AuthRepositoryProvider.sesion();
            return sesion == null ? null : sesion.token();
        });

        reintentar.setOnClickListener(v -> cargarPuntos());

        MaterialButton botonLogout = findViewById(R.id.btn_logout);
        botonLogout.setOnClickListener(v -> {
            botonLogout.setEnabled(false);
            // La sesión local ya se borra en el repositorio; el callback solo
            // sirve para volver cuando termine la petición (o falle la red).
            repositorio.cerrarSesion(respuesta -> volverALogin());
        });

        cargarPuntos();
    }

    /**
     * --------------------
     * () --> () -->
     * --------------------
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mapa != null) {
            mapa.onResume();
        }
    }

    /**
     * --------------------
     * () --> () -->
     * --------------------
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mapa != null) {
            mapa.onPause();
        }
    }

    /**
     * --------------------
     * () --> () -->
     * --------------------
     */
    @Override
    protected void onDestroy() {
        if (cliente != null) {
            cliente.cerrar();
        }
        super.onDestroy();
    }

    /** -------------------- () --> GET /api/mapa -------------------- */
    private void cargarPuntos() {
        estado.setVisibility(View.GONE);
        reintentar.setVisibility(View.GONE);
        cargando.setVisibility(View.VISIBLE);

        cliente.puntos(new ClienteMapa.PuntosCallback() {
            /** -------------------- puntos: [ PuntoMapa ] --> () -------------------- */
            @Override
            public void onExito(List<PuntoMapa> puntos) {
                cargando.setVisibility(View.GONE);
                capaCalor.setPuntos(puntos);
                capaPuntos.setPuntos(puntos);
                leyenda.actualizarColores(puntos);
                mapa.invalidate();

                if (puntos.isEmpty()) {
                    mostrarEstado("No hay datos disponibles en este momento.");
                    actualizacion.setText("● Sin datos");
                    return;
                }
                String ultima = Formato.maximaFecha(puntos);
                actualizacion.setText(ultima == null
                        ? "● Sin datos"
                        : "● Última actualización: " + Formato.fecha(ultima));
            }

            /** -------------------- mensaje:Text, noAutorizado:B --> () -------------------- */
            @Override
            public void onError(String mensaje, boolean noAutorizado) {
                cargando.setVisibility(View.GONE);
                if (noAutorizado) {
                    cerrarSesionLocal();
                    volverALogin();
                    return;
                }
                mostrarEstado(mensaje);
            }
        });
    }

    /** -------------------- idNodo:N --> GET /api/mapa/zona/{id} -------------------- */
    private void abrirDetalle(int idNodo) {
        cargando.setVisibility(View.VISIBLE);
        cliente.zona(idNodo, new ClienteMapa.ZonaCallback() {
            /** -------------------- zona: ZonaDetalle --> () -------------------- */
            @Override
            public void onExito(ZonaDetalle zona) {
                cargando.setVisibility(View.GONE);
                DetalleZonaBottomSheet.nuevaInstancia(zona)
                        .show(getSupportFragmentManager(), "detalle_zona");
            }

            /** -------------------- mensaje:Text, noAutorizado:B --> () -------------------- */
            @Override
            public void onError(String mensaje, boolean noAutorizado) {
                cargando.setVisibility(View.GONE);
                if (noAutorizado) {
                    cerrarSesionLocal();
                    volverALogin();
                    return;
                }
                Toast.makeText(MainActivity.this, mensaje, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** -------------------- mensaje:Text --> () -------------------- */
    private void mostrarEstado(String mensaje) {
        estado.setText(mensaje);
        estado.setVisibility(View.VISIBLE);
        reintentar.setVisibility(View.VISIBLE);
    }

    /**
     * --------------------
     * token_invalido() --> borrar_sesion_local() -->
     * --------------------
     *
     * Al recibir 401 del mapa el token ya no es válido (caducó o se revocó):
     * no tiene sentido avisar al servidor con él; se limpia la sesión local.
     */
    private void cerrarSesionLocal() {
        SesionManager sesion = AuthRepositoryProvider.sesion();
        if (sesion != null) {
            sesion.cerrar();
        }
    }

    /** -------------------- () --> () -------------------- */
    private void volverALogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    /**
     * --------------------
     * () --> configurar_cache_osmdroid() -->
     * --------------------
     *
     * Teselas en la caché privada de la app y User-Agent propio, igual que
     * hacía la primera app del mapa.
     */
    private void configurarOsmdroid() {
        File base = new File(getCacheDir(), "osmdroid");
        File teselas = new File(base, "tiles");
        if (!teselas.exists()) {
            teselas.mkdirs();
        }
        Configuration.getInstance().setOsmdroidBasePath(base);
        Configuration.getInstance().setOsmdroidTileCache(teselas);
        Configuration.getInstance().setUserAgentValue("QueAireMapa/1.0 (Android)");
    }

    /** -------------------- dp: N --> pixeles: N -------------------- */
    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}
