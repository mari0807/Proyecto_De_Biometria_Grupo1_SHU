package com.grupo1.biometria.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.grupo1.biometria.R;
import com.grupo1.biometria.data.AuthRepository;
import com.grupo1.biometria.data.AuthRepositoryProvider;
import com.grupo1.biometria.data.EstadoAuth;
import com.grupo1.biometria.data.RespuestaAuth;
import com.grupo1.biometria.data.UrlServidor;
import com.grupo1.biometria.util.Rol;
import com.grupo1.biometria.util.Validador;

/**
 * Pantalla de inicio de sesión (tarjeta H11).
 *
 * Flujo: si ya hay sesión local, comprueba contra el servidor y entra; si no,
 * valida los campos, llama a `POST /api/login` y, según la respuesta, entra en
 * la pantalla principal o muestra el error.
 */
public class LoginActivity extends AppCompatActivity {

    /** Clave del extra con el correo recién registrado (viene de RegisterActivity). */
    static final String EXTRA_EMAIL_REGISTRADO = "email_registrado";

    private AuthRepository repositorio;

    private TextInputLayout inputEmail;
    private TextInputLayout inputPassword;
    private TextInputLayout inputServidor;
    private TextInputEditText campoEmail;
    private TextInputEditText campoPassword;
    private TextInputEditText campoServidor;
    private CheckBox checkMantenerSesion;
    private TextView textoError;
    private MaterialButton botonLogin;

    /** Devuelve el resultado del registro para precargar el correo. */
    private final ActivityResultLauncher<Intent> lanzadorRegistro =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null) {
                    String email = resultado.getData().getStringExtra(EXTRA_EMAIL_REGISTRADO);
                    if (email != null) {
                        campoEmail.setText(email);
                    }
                }
            });

    /**
     * --------------------
     * savedInstanceState: Text? --> onCreate() -->
     * --------------------
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        repositorio = AuthRepositoryProvider.obtener(this);
        vincularVistas();
        campoServidor.setText(UrlServidor.url(this));

        botonLogin.setOnClickListener(v -> intentarInicioSesion());
        findViewById(R.id.link_registro).setOnClickListener(v -> {
            // El registro usa la misma URL de servidor: se guarda antes de navegar.
            UrlServidor.guardar(this, texto(campoServidor));
            lanzadorRegistro.launch(new Intent(this, RegisterActivity.class));
        });
        findViewById(R.id.link_olvidar).setOnClickListener(v -> mostrarRecuperacion());

        // Sesión ya iniciada (y no caducada): se valida con el servidor y se entra.
        if (repositorio.haySesion()) {
            comprobarSesionGuardada();
        }
    }

    /**
     * --------------------
     * olvido_contraseña() --> mostrar_diálogo() --> escribir_soporte() -->
     * --------------------
     *
     * Igual que la sección `#recuperar` de `user.html`: la web no llama a la
     * API, muestra el correo de soporte y abre el cliente de correo con el
     * asunto ya preparado.
     */
    private void mostrarRecuperacion() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.titulo_recuperar)
                .setMessage(getString(R.string.recuperar_subtitulo)
                        + "\n\n" + getString(R.string.msg_recuperar))
                .setPositiveButton(R.string.btn_soporte, (dialog, which) -> {
                    Intent correo = new Intent(Intent.ACTION_SENDTO,
                            Uri.parse("mailto:shu@gmail.com?subject=Recuperar%20contrase%C3%B1a%20Qu%C3%A9Aire"));
                    startActivity(Intent.createChooser(correo, getString(R.string.btn_soporte)));
                })
                .setNegativeButton(R.string.link_volver_login, null)
                .show();
    }

    private void vincularVistas() {
        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        campoEmail = findViewById(R.id.campo_email);
        campoPassword = findViewById(R.id.campo_password);
        inputServidor = findViewById(R.id.input_servidor);
        campoServidor = findViewById(R.id.campo_servidor);
        checkMantenerSesion = findViewById(R.id.check_mantener_sesion);
        textoError = findViewById(R.id.text_error_login);
        botonLogin = findViewById(R.id.btn_login);
    }

    /** Valida la sesión almacenada contra `GET /api/sesion` antes de entrar. */
    private void comprobarSesionGuardada() {
        botonLogin.setEnabled(false);
        mostrarErrorGeneral(getString(R.string.comprobando_sesion));
        repositorio.comprobarSesion(respuesta -> {
            botonLogin.setEnabled(true);
            textoError.setVisibility(View.GONE);
            if (respuesta.estado == EstadoAuth.OK || respuesta.estado == EstadoAuth.ERROR_RED) {
                // Sin respuesta del servidor se confía en la sesión local
                // (el token ya se ha comprobado contra el reloj del móvil).
                abrirPantallaPrincipal();
            }
            // SESION_INVALIDA: la sesión se ha borrado en el repositorio y
            // el usuario se queda en el formulario para iniciar sesión.
        });
    }

    /**
     * --------------------
     * --> intentar_inicio_sesion() -->
     * --------------------
     */
    private void intentarInicioSesion() {
        limpiarErrores();

        String email = texto(campoEmail);
        String password = texto(campoPassword);

        boolean hayErrorDeCampo = false;
        if (Validador.estaVacio(email)) {
            inputEmail.setError(getString(R.string.error_login_campos));
            hayErrorDeCampo = true;
        } else if (!Validador.emailValido(email)) {
            inputEmail.setError(getString(R.string.error_email_invalido));
            hayErrorDeCampo = true;
        }
        if (Validador.estaVacio(password)) {
            inputPassword.setError(getString(R.string.error_login_campos));
            hayErrorDeCampo = true;
        }
        if (hayErrorDeCampo) {
            return;
        }

        // La URL del servidor escrita se guarda y se usa para esta petición
        // (y para el registro y la sesión); si cambió, el repositorio se recrea.
        UrlServidor.guardar(this, texto(campoServidor));
        repositorio = AuthRepositoryProvider.obtener(this);

        // Mientras el servidor responde, el botón queda deshabilitado.
        botonLogin.setEnabled(false);
        repositorio.iniciarSesion(email, password, checkMantenerSesion.isChecked(), respuesta -> {
            botonLogin.setEnabled(true);
            mostrarRespuestaLogin(respuesta);
        });
    }

    private void mostrarRespuestaLogin(RespuestaAuth respuesta) {
        switch (respuesta.estado) {
            case OK:
                abrirPantallaPrincipal();
                break;
            case CREDENCIALES_INVALIDAS:
            case DATOS_INVALIDOS:
                // El backend devuelve el mensaje en español ("Correo o contraseña incorrectos.").
                mostrarErrorGeneral(respuesta.mensaje.isEmpty()
                        ? getString(R.string.error_campo_obligatorio) : respuesta.mensaje);
                descartarContraseña();
                break;
            case ERROR_RED:
                mostrarErrorGeneral(getString(R.string.error_red));
                break;
            default:
                mostrarErrorGeneral(respuesta.mensaje.isEmpty()
                        ? getString(R.string.error_campo_obligatorio) : respuesta.mensaje);
                break;
        }
    }

    /**
     * --------------------
     * usuario.rol --> elegir_destino() --> abrir_pantalla() -->
     * --------------------
     *
     * Mismo criterio que `destino()` en `acceso.js` de la web:
     * rol `admin` → AdminActivity; cualquier otro → MainActivity.
     * Se usa el usuario guardado en la sesión para cubrir también el caso de
     * sesión restaurada (sin respuesta nueva del servidor).
     */
    private void abrirPantallaPrincipal() {
        Class<?> destino = Rol.esAdmin(repositorio.usuario())
                ? AdminActivity.class
                : MainActivity.class;
        startActivity(new Intent(this, destino));
        finish();
    }

    private void limpiarErrores() {
        inputEmail.setError(null);
        inputPassword.setError(null);
        textoError.setVisibility(View.GONE);
    }

    private void mostrarErrorGeneral(String mensaje) {
        textoError.setText(mensaje);
        textoError.setVisibility(View.VISIBLE);
    }

    /**
     * --------------------
     * credenciales_rechazadas() --> borrar_contraseña() -->
     * --------------------
     *
     * Tras un error de credenciales se limpia la contraseña y se cierra el
     * teclado. Así el aviso (situado bajo el campo) queda siempre a la vista
     * y no queda una contraseña fallida escrita en el formulario.
     */
    private void descartarContraseña() {
        campoPassword.setText("");
        inputPassword.setError(null);
        campoPassword.clearFocus();
        ocultarTeclado();
    }

    private void ocultarTeclado() {
        InputMethodManager gestor =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (gestor == null) {
            return;
        }
        View vista = getCurrentFocus();
        if (vista == null) {
            vista = campoPassword;
        }
        gestor.hideSoftInputFromWindow(vista.getWindowToken(), 0);
    }

    private String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }
}
