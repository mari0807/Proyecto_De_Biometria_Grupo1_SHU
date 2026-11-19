package com.queaire.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * --------------------
 * CD: LoginActivity = ( email:Text, password:Text, base:Text --> token:Text )
 * CC: POST /api/login (Comunicacion.md:47-53); el token se guarda en SesionManager.
 * GR: Toda la UI se maneja en el hilo principal; la red va en ClienteREST.
 * --------------------
 */
public final class LoginActivity extends AppCompatActivity {
    private EditText correo;
    private EditText contrasena;
    private EditText servidor;
    private Button entrar;
    private ProgressBar cargando;
    private TextView mensaje;

    private SesionManager sesion;
    private ClienteREST cliente;

    /** -------------------- estado --> () -------------------- */
    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        setContentView(R.layout.activity_login);

        sesion = new SesionManager(this);
        cliente = new ClienteREST();

        correo = findViewById(R.id.login_email);
        contrasena = findViewById(R.id.login_password);
        servidor = findViewById(R.id.login_servidor);
        entrar = findViewById(R.id.login_entrar);
        cargando = findViewById(R.id.login_cargando);
        mensaje = findViewById(R.id.login_mensaje);

        String ultimoCorreo = sesion.email();
        correo.setText(ultimoCorreo.isEmpty() ? "cliente@email.com" : ultimoCorreo);
        servidor.setText(sesion.baseUrl());
        entrar.setOnClickListener(v -> intentarEntrar());

        if (sesion.haySesion()) {
            abrirMapa();
        }
    }

    /** -------------------- () --> () -------------------- */
    @Override
    protected void onDestroy() {
        cliente.cerrar();
        super.onDestroy();
    }

    /** -------------------- () --> POST /api/login -------------------- */
    private void intentarEntrar() {
        final String base = servidor.getText().toString().trim();
        final String email = correo.getText().toString().trim();
        String password = contrasena.getText().toString();
        if (email.isEmpty() || password.isEmpty()) {
            mensaje.setText("Introduce correo y contraseña.");
            return;
        }

        entrar.setEnabled(false);
        cargando.setVisibility(View.VISIBLE);
        mensaje.setText("");
        cliente.login(base, email, password, new ClienteREST.LoginCallback() {
            /** -------------------- token:Text, usuario:Text --> () -------------------- */
            @Override
            public void onExito(String token, String usuario) {
                sesion.guardar(base, token, email, usuario);
                abrirMapa();
            }

            /** -------------------- mensaje:Text --> () -------------------- */
            @Override
            public void onError(String textoError) {
                entrar.setEnabled(true);
                cargando.setVisibility(View.GONE);
                mensaje.setText(textoError);
            }
        });
    }

    /** -------------------- () --> () -------------------- */
    private void abrirMapa() {
        startActivity(new Intent(this, MapaActivity.class));
        finish();
    }
}
