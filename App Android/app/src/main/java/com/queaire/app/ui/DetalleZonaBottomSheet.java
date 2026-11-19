package com.queaire.app.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import com.queaire.app.Formato;
import com.queaire.app.Paleta;
import com.queaire.app.R;
import com.queaire.app.model.Medicion;
import com.queaire.app.model.ZonaDetalle;

import java.util.Locale;

/**
 * --------------------
 * CD: DetalleZonaBottomSheet = ( ZonaDetalle --> panel: View )
 * CC: Panel inferior con los campos de H18 que la web muestra en user.html:101-111.
 * GR: La zona viaja por Bundle como Serializable; la gráfica se dibuja en la UI.
 * --------------------
 */
public final class DetalleZonaBottomSheet extends BottomSheetDialogFragment {
    private static final String CLAVE_ZONA = "zona";

    /** -------------------- zona: ZonaDetalle --> DetalleZonaBottomSheet -------------------- */
    public static DetalleZonaBottomSheet nuevaInstancia(ZonaDetalle zona) {
        DetalleZonaBottomSheet fragmento = new DetalleZonaBottomSheet();
        Bundle argumentos = new Bundle();
        argumentos.putSerializable(CLAVE_ZONA, zona);
        fragmento.setArguments(argumentos);
        return fragmento;
    }

    /** -------------------- inflater, contenedor, estado --> View -------------------- */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup contenedor, Bundle estado) {
        View vista = inflater.inflate(R.layout.fragment_detalle_zona, contenedor, false);
        ZonaDetalle zona = (ZonaDetalle) requireArguments().getSerializable(CLAVE_ZONA);
        if (zona == null) {
            dismiss();
            return vista;
        }

        TextView nombre = vista.findViewById(R.id.detalle_nombre);
        TextView categoria = vista.findViewById(R.id.detalle_categoria);
        TextView contaminante = vista.findViewById(R.id.detalle_contaminante);
        TextView valor = vista.findViewById(R.id.detalle_valor);
        TextView ubicacion = vista.findViewById(R.id.detalle_ubicacion);
        TextView fecha = vista.findViewById(R.id.detalle_fecha);
        TextView graficaVacia = vista.findViewById(R.id.detalle_grafica_vacio);
        GraficaEvolucionView grafica = vista.findViewById(R.id.detalle_grafica);
        Button cerrar = vista.findViewById(R.id.detalle_cerrar);

        nombre.setText(zona.getNombre());
        contaminante.setText(zona.getContaminante() + " · "
                + String.format(Locale.US, "%.5f, %.5f", zona.getLatitud(), zona.getLongitud()));
        ubicacion.setText("Ubicación: "
                + String.format(Locale.US, "%.5f, %.5f", zona.getLatitud(), zona.getLongitud()));

        Medicion actual = zona.getActual();
        String categoriaTexto = actual == null ? "sin_datos" : actual.getCategoria();
        categoria.setText(Paleta.etiqueta(categoriaTexto));
        GradientDrawable chip = new GradientDrawable();
        chip.setShape(GradientDrawable.RECTANGLE);
        chip.setCornerRadius(12f * getResources().getDisplayMetrics().density);
        chip.setColor(colorSeguro(actual == null ? Paleta.SIN_DATOS : actual.getColor()));
        categoria.setBackground(chip);

        valor.setText(actual == null
                ? "—"
                : Formato.valor(actual.getValor()) + " " + zona.getUnidad());
        fecha.setText(actual == null
                ? "—"
                : "Fecha: " + Formato.fecha(actual.getFechaHora()));

        grafica.setHistorial(zona.getHistorial());
        boolean hayGrafica = zona.getHistorial().size() >= 2;
        grafica.setVisibility(hayGrafica ? View.VISIBLE : View.GONE);
        vista.findViewById(R.id.detalle_grafica_titulo).setVisibility(hayGrafica ? View.VISIBLE : View.GONE);
        graficaVacia.setVisibility(hayGrafica ? View.GONE : View.VISIBLE);
        cerrar.setOnClickListener(v -> dismiss());

        return vista;
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
