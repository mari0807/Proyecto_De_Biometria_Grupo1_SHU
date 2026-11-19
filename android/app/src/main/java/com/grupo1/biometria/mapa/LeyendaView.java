package com.grupo1.biometria.mapa;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.grupo1.biometria.R;
import com.grupo1.biometria.mapa.model.PuntoMapa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * --------------------
 * CD: LeyendaView = ( [ PuntoMapa ] --> leyenda: View )
 * CC: Cinco estados de mapa.js:14-20,77-90; colores tomados de la API.
 * GR: Los swatches se guardan por categoría; las actualizaciones llegan en la UI.
 * --------------------
 */
public final class LeyendaView extends LinearLayout {
    private final Map<String, GradientDrawable> muestras = new HashMap<>();

    /** -------------------- contexto, atributos --> LeyendaView -------------------- */
    public LeyendaView(Context contexto, AttributeSet atributos) {
        super(contexto, atributos);
        construir();
    }

    /** -------------------- puntos: [ PuntoMapa ] --> () -------------------- */
    public void actualizarColores(List<PuntoMapa> puntos) {
        Map<String, String> colores = new HashMap<>();
        if (puntos != null) {
            for (PuntoMapa punto : puntos) {
                colores.put(punto.getCategoria(), punto.getColor());
            }
        }
        for (Map.Entry<String, GradientDrawable> entrada : muestras.entrySet()) {
            String color = colores.get(entrada.getKey());
            if (color != null) {
                entrada.getValue().setColor(colorSeguro(color));
            }
        }
    }

    /** -------------------- () --> () -------------------- */
    private void construir() {
        setOrientation(LinearLayout.VERTICAL);
        int size = dp(10);
        for (String categoria : Paleta.CATEGORIAS) {
            LinearLayout fila = new LinearLayout(getContext());
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(android.view.Gravity.CENTER_VERTICAL);

            GradientDrawable circulo = new GradientDrawable();
            circulo.setShape(GradientDrawable.OVAL);
            circulo.setColor(colorSeguro(Paleta.fallback(categoria)));
            View muestra = new View(getContext());
            muestra.setBackground(circulo);
            muestra.setLayoutParams(new LinearLayout.LayoutParams(size, size));

            TextView etiqueta = new TextView(getContext());
            etiqueta.setText(Paleta.etiqueta(categoria));
            etiqueta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            etiqueta.setTextColor(getResources().getColor(R.color.ink));
            LinearLayout.LayoutParams parametros = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            parametros.setMargins(dp(6), 0, 0, 0);
            etiqueta.setLayoutParams(parametros);

            fila.addView(muestra);
            fila.addView(etiqueta);
            addView(fila);
            muestras.put(categoria, circulo);
        }
    }

    /** -------------------- dp: N --> pixeles: N -------------------- */
    private int dp(int valor) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, valor, getResources().getDisplayMetrics()));
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
