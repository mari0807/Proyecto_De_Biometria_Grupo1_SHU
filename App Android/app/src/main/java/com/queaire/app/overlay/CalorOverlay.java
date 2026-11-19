package com.queaire.app.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import com.queaire.app.Paleta;
import com.queaire.app.model.PuntoMapa;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;

/**
 * --------------------
 * CD: CalorOverlay = ( puntos: [ PuntoMapa ] --> capa: Overlay )
 * CC: Equivale a L.heatLayer con radius 38, blur 26 y maxZoom 15 (mapa.js:100-105).
 * GR: La lista se copia al asignarla; el dibujo ocurre solo en el hilo de la UI.
 * --------------------
 */
public final class CalorOverlay extends Overlay {
    private static final int ZOOM_MAXIMO = 15;

    private final float radio;
    private final Paint pintura = new Paint();
    private List<PuntoMapa> puntos = new ArrayList<>();

    /** -------------------- contexto --> CalorOverlay -------------------- */
    public CalorOverlay(Context contexto) {
        radio = 38f * contexto.getResources().getDisplayMetrics().density;
        pintura.setAntiAlias(true);
    }

    /** -------------------- puntos: [ PuntoMapa ] --> () -------------------- */
    public void setPuntos(List<PuntoMapa> puntos) {
        this.puntos = puntos == null ? new ArrayList<>() : new ArrayList<>(puntos);
    }

    /** -------------------- canvas, mapa, sombra --> () -------------------- */
    @Override
    public void draw(Canvas canvas, MapView mapa, boolean shadow) {
        if (shadow || puntos.isEmpty() || mapa.getProjection() == null) {
            return;
        }
        if (mapa.getZoomLevelDouble() > ZOOM_MAXIMO) {
            return;
        }
        for (PuntoMapa punto : puntos) {
            android.graphics.Point pixel = mapa.getProjection()
                    .toPixels(new GeoPoint(punto.getLatitud(), punto.getLongitud()), null);
            int color = seguro(punto.getColor());
            int rojo = Color.red(color);
            int verde = Color.green(color);
            int azul = Color.blue(color);
            int alfa = (int) Math.round(255 * Paleta.intensidad(punto.getCategoria()));

            RadialGradient degradado = new RadialGradient(
                    pixel.x, pixel.y, radio,
                    new int[]{
                            Color.argb(alfa, rojo, verde, azul),
                            Color.argb(alfa / 2, rojo, verde, azul),
                            Color.argb(0, rojo, verde, azul)
                    },
                    new float[]{0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP
            );
            pintura.setShader(degradado);
            canvas.drawCircle(pixel.x, pixel.y, radio, pintura);
        }
        pintura.setShader(null);
    }

    /** -------------------- color: Text --> colorSeguro: N -------------------- */
    private static int seguro(String color) {
        try {
            return Color.parseColor(color);
        } catch (IllegalArgumentException e) {
            return Color.GRAY;
        }
    }
}
