package com.grupo1.biometria.mapa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.grupo1.biometria.mapa.model.Medicion;

import java.util.ArrayList;
import java.util.List;

/**
 * --------------------
 * CD: GraficaEvolucionView = ( [ Medicion ] --> grafica: View )
 * CC: Equivale al SVG de mapa.js:124-155 (polígono #EAF4F8, trazo #198DB1).
 * GR: Copia defensiva del historial; onDraw solo se ejecuta en el hilo de la UI.
 * --------------------
 */
public final class GraficaEvolucionView extends View {
    private final Paint area = new Paint();
    private final Paint linea = new Paint();
    private final Paint etiquetas = new Paint();
    private final float margen;

    private List<Medicion> historial = new ArrayList<>();

    /** -------------------- contexto, atributos --> GraficaEvolucionView -------------------- */
    public GraficaEvolucionView(Context contexto, AttributeSet atributos) {
        super(contexto, atributos);
        margen = 8f * contexto.getResources().getDisplayMetrics().density;
        area.setAntiAlias(true);
        area.setStyle(Paint.Style.FILL);
        area.setColor(Color.parseColor("#EAF4F8"));
        linea.setAntiAlias(true);
        linea.setStyle(Paint.Style.STROKE);
        float densidad = contexto.getResources().getDisplayMetrics().density;
        linea.setStrokeWidth(3f * densidad);
        linea.setColor(Color.parseColor("#198DB1"));
        etiquetas.setAntiAlias(true);
        etiquetas.setColor(Color.GRAY);
        etiquetas.setTextSize(11f * densidad);
    }

    /** -------------------- historial: [ Medicion ] --> () -------------------- */
    public void setHistorial(List<Medicion> historial) {
        this.historial = historial == null ? new ArrayList<>() : new ArrayList<>(historial);
        invalidate();
    }

    /** -------------------- canvas --> () -------------------- */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (historial.size() < 2) {
            return;
        }

        float ancho = getWidth();
        float alto = getHeight();
        float izquierda = margen;
        float derecha = ancho - margen;
        float arriba = margen;
        float abajo = alto - margen * 3f;

        double minimo = Double.MAX_VALUE;
        double maximo = -Double.MAX_VALUE;
        for (Medicion medicion : historial) {
            minimo = Math.min(minimo, medicion.getValor());
            maximo = Math.max(maximo, medicion.getValor());
        }
        double rango = maximo - minimo;
        if (rango < 0.000001) {
            rango = 1;
        }

        Path relleno = new Path();
        Path trazo = new Path();
        for (int i = 0; i < historial.size(); i++) {
            float x = izquierda + (derecha - izquierda) * i / (float) (historial.size() - 1);
            float y = arriba + (float) ((maximo - historial.get(i).getValor()) / rango) * (abajo - arriba);
            if (i == 0) {
                trazo.moveTo(x, y);
                relleno.moveTo(x, abajo);
                relleno.lineTo(x, y);
            } else {
                trazo.lineTo(x, y);
                relleno.lineTo(x, y);
            }
        }
        relleno.lineTo(derecha, abajo);
        relleno.close();

        canvas.drawPath(relleno, area);
        canvas.drawPath(trazo, linea);
        canvas.drawText(Formato.fechaCorta(historial.get(0).getFechaHora()), izquierda, alto - margen, etiquetas);
        String ultima = Formato.fechaCorta(historial.get(historial.size() - 1).getFechaHora());
        canvas.drawText(ultima, derecha - etiquetas.measureText(ultima), alto - margen, etiquetas);
    }
}
