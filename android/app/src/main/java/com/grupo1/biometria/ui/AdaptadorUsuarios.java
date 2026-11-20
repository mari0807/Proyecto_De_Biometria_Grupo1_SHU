package com.grupo1.biometria.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.grupo1.biometria.R;
import com.grupo1.biometria.data.UsuarioAdmin;

import java.util.ArrayList;
import java.util.List;

/**
 * Filas del listado de usuarios del panel de administración.
 *
 * Cada fila muestra nombre, «correo · rol» y el estado Activo/Baja; al pulsarla
 * se abre el editor (equivalente al botón «Editar» de la tabla de la web).
 */
public final class AdaptadorUsuarios extends RecyclerView.Adapter<AdaptadorUsuarios.Fila> {

    /** Fila pulsada. */
    public interface OnUsuarioListener {
        void onUsuario(UsuarioAdmin usuario);
    }

    private final List<UsuarioAdmin> usuarios = new ArrayList<>();
    private final OnUsuarioListener listener;

    /** -------------------- listener --> AdaptadorUsuarios -------------------- */
    public AdaptadorUsuarios(OnUsuarioListener listener) {
        this.listener = listener;
    }

    /** -------------------- usuarios: [ UsuarioAdmin ] --> () -------------------- */
    public void setUsuarios(List<UsuarioAdmin> lista) {
        usuarios.clear();
        if (lista != null) {
            usuarios.addAll(lista);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        View vista = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.fila_usuario, padre, false);
        return new Fila(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        UsuarioAdmin usuario = usuarios.get(posicion);
        fila.nombre.setText(usuario.getNombre() + " " + usuario.getApellidos());
        fila.detalle.setText(usuario.getEmail() + " · " + usuario.getRol());
        boolean activo = usuario.isActivo();
        fila.estado.setText(activo ? "● Activo" : "● Baja");
        fila.estado.setTextColor(ContextCompat.getColor(fila.itemView.getContext(),
                activo ? R.color.success : R.color.error));
        fila.itemView.setOnClickListener(v -> listener.onUsuario(usuario));
    }

    @Override
    public int getItemCount() {
        return usuarios.size();
    }

    /** Fila del listado. */
    static final class Fila extends RecyclerView.ViewHolder {
        final TextView nombre;
        final TextView detalle;
        final TextView estado;

        Fila(View vista) {
            super(vista);
            nombre = vista.findViewById(R.id.fila_nombre);
            detalle = vista.findViewById(R.id.fila_detalle);
            estado = vista.findViewById(R.id.fila_estado);
        }
    }
}
