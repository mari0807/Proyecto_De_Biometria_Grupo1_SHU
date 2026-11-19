package com.grupo1.biometria.mapa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;

import com.grupo1.biometria.mapa.model.PuntoMapa;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;

/**
 * --------------------
 * CD: PuntosOverlay = ( puntos: [ PuntoMapa ], tap: PuntoMapa --> () )
 * CC: Marcador circular de radio 8px y borde blanco de 3px (mapa.js:109-120).
 * GR: Copia defensiva de la lista; el listener se invierte solo en la UI.
 * --------------------
 */
public final class PuntosOverlay extends Overlay {
    public interface OnPuntoListener {
        void onPunto(PuntoMapa punto);
    }

    private final float radio;
    private final float areaToque;
    private final Paint relleno = new Paint();
    private final Paint borde = new Paint();
    private final OnPuntoListener listener;

    private List<PuntoMapa> puntos = new ArrayList<>();

    /** -------------------- contexto, listener --> PuntosOverlay -------------------- */
    public PuntosOverlay(Context contexto, OnPuntoListener listener) {
        float densidad = contexto.getResources().getDisplayMetrics().density;
        radio = 8f * densidad;
        areaToque = (8f + 6f) * densidad;
        this.listener = listener;

        relleno.setAntiAlias(true);
        relleno.setStyle(Paint.Style.FILL);
        borde.setAntiAlias(true);
        borde.setStyle(Paint.Style.STROKE);
        borde.setStrokeWidth(3f * densidad);
        borde.setColor(Color.WHITE);
    }

    /** -------------------- puntos: [ PuntoMapa ] --> () -------------------- */
    public void setPuntos(List<PuntoMapa> puntos) {
        this.puntos = puntos == null ? new ArrayList<>() : new ArrayList<>(puntos);
    }

    /** -------------------- canvas, mapa, sombra --> () -------------------- */
    @Override
    public void draw(Canvas canvas, MapView mapa, boolean shadow) {
        if (shadow || mapa.getProjection() == null) {
            return;
        }
        for (PuntoMapa punto : puntos) {
            android.graphics.Point pixel = mapa.getProjection()
                    .toPixels(new GeoPoint(punto.getLatitud(), punto.getLongitud()), null);
            relleno.setColor(colorSeguro(punto.getColor()));
            canvas.drawCircle(pixel.x, pixel.y, radio, relleno);
            canvas.drawCircle(pixel.x, pixel.y, radio, borde);
        }
    }

    /** -------------------- evento, mapa --> consumido:B -------------------- */
    @Override
    public boolean onSingleTapUp(MotionEvent evento, MapView mapa) {
        if (mapa.getProjection() == null) {
            return false;
        }
        for (int i = puntos.size() - 1; i >= 0; i--) {
            PuntoMapa punto = puntos.get(i);
            android.graphics.Point pixel = mapa.getProjection()
                    .toPixels(new GeoPoint(punto.getLatitud(), punto.getLongitud()), null);
            float distancia = (float) Math.hypot(evento.getX() - pixel.x, evento.getY() - pixel.y);
            if (distancia <= areaToque) {
                listener.onPunto(punto);
                return true;
            }
        }
        return false;
    }

    /** -------------------- color: Text --> colorSeguro: N -------------------- */
    private static int colorSeguro(String color) {
        try {
            return Color.parseColor(color);
        } catch (IllegalArgumentException e) {
            return Color.GRAY;
        }
    }
}
