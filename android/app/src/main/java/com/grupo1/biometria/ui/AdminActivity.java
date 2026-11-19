package com.grupo1.biometria.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.grupo1.biometria.R;
import com.grupo1.biometria.data.AuthRepository;
import com.grupo1.biometria.data.AuthRepositoryProvider;
import com.grupo1.biometria.util.Rol;

/**
 * Pantalla exclusiva para usuarios con rol `admin`.
 *
 * Es el equivalente a `admin.html` de la web: se entra aquí cuando el backend
 * devuelve `usuario.rol = "admin"` en el login (o al validar la sesión guardada).
 * Un usuario normal es enviado a {@link MainActivity}.
 */
public class AdminActivity extends AppCompatActivity {

    private AuthRepository repositorio;

    /**
     * --------------------
     * savedInstanceState: Text? --> onCreate() -->
     * --------------------
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        repositorio = AuthRepositoryProvider.obtener(this);

        // Doble comprobación: hace falta sesión Y rol de administrador.
        if (!repositorio.haySesion() || !Rol.esAdmin(repositorio.usuario())) {
            irALogin();
            return;
        }

        setContentView(R.layout.activity_admin);

        TextView textoUsuario = findViewById(R.id.text_usuario);
        TextView textoEmail = findViewById(R.id.text_email);
        textoUsuario.setText(repositorio.usuario().nombreCompleto());
        textoEmail.setText(repositorio.usuario().email);

        MaterialButton botonLogout = findViewById(R.id.btn_logout);
        botonLogout.setOnClickListener(v -> {
            botonLogout.setEnabled(false);
            repositorio.cerrarSesion(respuesta -> irALogin());
        });
    }

    private void irALogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
