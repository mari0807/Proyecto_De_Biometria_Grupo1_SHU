package com.queaire.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.queaire.app.model.PuntoMapa;
import com.queaire.app.model.ZonaDetalle;
import com.queaire.app.overlay.CalorOverlay;
import com.queaire.app.overlay.PuntosOverlay;
import com.queaire.app.ui.DetalleZonaBottomSheet;
import com.queaire.app.ui.LeyendaView;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.File;
import java.util.List;

/**
 * --------------------
 * CD: MapaActivity = ( token:Text --> mapa:View, zona:N --> detalle:View )
 * CC: H-16 (inputs/Pagina Mapa App.md:1-5); réplica de user.html + mapa.js.
 * GR: Red en ClienteREST, vistas solo en el hilo principal; overlays se actualizan al cargar.
 * --------------------
 */
public final class MapaActivity extends AppCompatActivity {
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

    private SesionManager sesion;
    private ClienteREST cliente;

    /** -------------------- estado --> () -------------------- */
    @Override
    protected void onCreate(Bundle estadoGuardado) {
        super.onCreate(estadoGuardado);
        configurarOsmdroid();
        setContentView(R.layout.activity_mapa);

        mapa = findViewById(R.id.mapa);
        leyenda = findViewById(R.id.mapa_leyenda);
        estado = findViewById(R.id.mapa_estado);
        actualizacion = findViewById(R.id.mapa_actualizacion);
        cargando = findViewById(R.id.mapa_cargando);
        reintentar = findViewById(R.id.mapa_reintentar);

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

        sesion = new SesionManager(this);
        cliente = new ClienteREST();
        reintentar.setOnClickListener(v -> cargarPuntos());

        if (!sesion.haySesion()) {
            irAlLogin();
            return;
        }
        cargarPuntos();
    }

    /** -------------------- () --> () -------------------- */
    @Override
    protected void onResume() {
        super.onResume();
        mapa.onResume();
    }

    /** -------------------- () --> () -------------------- */
    @Override
    protected void onPause() {
        super.onPause();
        mapa.onPause();
    }

    /** -------------------- () --> () -------------------- */
    @Override
    protected void onDestroy() {
        cliente.cerrar();
        super.onDestroy();
    }

    /** -------------------- () --> GET /api/mapa -------------------- */
    private void cargarPuntos() {
        estado.setVisibility(View.GONE);
        reintentar.setVisibility(View.GONE);
        cargando.setVisibility(View.VISIBLE);

        cliente.puntos(sesion.baseUrl(), sesion.token(), new ClienteREST.PuntosCallback() {
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
                    sesion.limpiar();
                    irAlLogin();
                    return;
                }
                mostrarEstado(mensaje);
            }
        });
    }

    /** -------------------- idNodo:N --> GET /api/mapa/zona/{id} -------------------- */
    private void abrirDetalle(int idNodo) {
        cargando.setVisibility(View.VISIBLE);
        cliente.zona(idNodo, sesion.baseUrl(), sesion.token(), new ClienteREST.ZonaCallback() {
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
                    sesion.limpiar();
                    irAlLogin();
                    return;
                }
                Toast.makeText(MapaActivity.this, mensaje, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** -------------------- mensaje:Text --> () -------------------- */
    private void mostrarEstado(String mensaje) {
        estado.setText(mensaje);
        estado.setVisibility(View.VISIBLE);
        reintentar.setVisibility(View.VISIBLE);
    }

    /** -------------------- () --> () -------------------- */
    private void irAlLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    /** -------------------- () --> () -------------------- */
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
}
