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
 * Pantalla principal a la que se accede tras autenticarse (H11).
 * Muestra el usuario en sesión y permite cerrarla (`POST /api/logout`).
 */
public class MainActivity extends AppCompatActivity {

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
        findViewById(R.id.btn_atras).setOnClickListener(v -> volverALogin());

        TextView textoUsuario = findViewById(R.id.text_usuario);
        TextView textoEmail = findViewById(R.id.text_email);
        textoUsuario.setText(repositorio.usuario().nombreCompleto());
        textoEmail.setText(repositorio.usuario().email);

        MaterialButton botonLogout = findViewById(R.id.btn_logout);
        botonLogout.setOnClickListener(v -> {
            botonLogout.setEnabled(false);
            // La sesión local ya se borra en el repositorio; el callback solo
            // sirve para volver cuando termine la petición (o falle la red).
            repositorio.cerrarSesion(respuesta -> volverALogin());
        });
    }

    private void volverALogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
