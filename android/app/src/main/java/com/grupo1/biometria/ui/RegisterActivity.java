package com.grupo1.biometria.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.grupo1.biometria.R;
import com.grupo1.biometria.data.AuthRepository;
import com.grupo1.biometria.data.AuthRepositoryProvider;
import com.grupo1.biometria.data.EstadoAuth;
import com.grupo1.biometria.data.RespuestaAuth;
import com.grupo1.biometria.util.Rol;
import com.grupo1.biometria.util.Validador;

/**
 * Pantalla de registro (tarjeta H29).
 *
 * Es accesible desde el enlace de {@link LoginActivity}. Valida los campos
 * obligatorios y el formato del correo con las mismas reglas que el backend,
 * exige la aceptación de los términos de uso y, si todo es correcto, confirma
 * el éxito y vuelve al inicio de sesión con el correo precargado.
 */
public class RegisterActivity extends AppCompatActivity {

    private AuthRepository repositorio;

    private TextInputLayout inputNombre;
    private TextInputLayout inputApellidos;
    private TextInputLayout inputEmail;
    private TextInputLayout inputPassword;
    private TextInputLayout inputConfirmPassword;
    private TextInputEditText campoNombre;
    private TextInputEditText campoApellidos;
    private TextInputEditText campoEmail;
    private TextInputEditText campoPassword;
    private TextInputEditText campoConfirmPassword;
    private CheckBox checkTerminos;
    private TextView textoError;
    private TextView textoErrorTerminos;
    private MaterialButton botonRegistro;

    /** Vistas y etiquetas de los cinco requisitos de la contraseña. */
    private TextView[] requisitos;
    private String[] etiquetasRequisitos;

    /**
     * --------------------
     * savedInstanceState: Text? --> onCreate() -->
     * --------------------
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        repositorio = AuthRepositoryProvider.obtener(this);
        vincularVistas();

        botonRegistro.setOnClickListener(v -> intentarRegistro());
        findViewById(R.id.btn_atras).setOnClickListener(v -> finish());
        findViewById(R.id.link_login).setOnClickListener(v -> finish());
        findViewById(R.id.link_terminos).setOnClickListener(v -> mostrarTerminos());

        // Los requisitos se marcan en vivo mientras se escribe la contraseña.
        campoPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No es necesario.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No es necesario.
            }

            @Override
            public void afterTextChanged(Editable s) {
                actualizarRequisitos(s == null ? "" : s.toString());
            }
        });
    }

    private void vincularVistas() {
        inputNombre = findViewById(R.id.input_nombre);
        inputApellidos = findViewById(R.id.input_apellidos);
        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        inputConfirmPassword = findViewById(R.id.input_confirm_password);
        campoNombre = findViewById(R.id.campo_nombre);
        campoApellidos = findViewById(R.id.campo_apellidos);
        campoEmail = findViewById(R.id.campo_email);
        campoPassword = findViewById(R.id.campo_password);
        campoConfirmPassword = findViewById(R.id.campo_confirm_password);
        checkTerminos = findViewById(R.id.check_terminos);
        textoError = findViewById(R.id.text_error_register);
        textoErrorTerminos = findViewById(R.id.text_error_terminos);
        botonRegistro = findViewById(R.id.btn_register);

        requisitos = new TextView[]{
                findViewById(R.id.req_longitud),
                findViewById(R.id.req_mayuscula),
                findViewById(R.id.req_minuscula),
                findViewById(R.id.req_numero),
                findViewById(R.id.req_signo)
        };
        etiquetasRequisitos = new String[]{
                getString(R.string.req_longitud),
                getString(R.string.req_mayuscula),
                getString(R.string.req_minuscula),
                getString(R.string.req_numero),
                getString(R.string.req_signo)
        };
        actualizarRequisitos("");
    }

    /**
     * --------------------
     * ver_terminos() --> mostrar_diálogo() --> aceptar_terminos() -->
     * --------------------
     *
     * Muestra los términos y permite aceptarlos desde el propio diálogo.
     */
    private void mostrarTerminos() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.titulo_terminos)
                .setMessage(R.string.terminos_contenido)
                .setPositiveButton(R.string.dialog_aceptar,
                        (dialog, which) -> checkTerminos.setChecked(true))
                .setNegativeButton(R.string.dialog_cancelar, null)
                .show();
    }

    /**
     * --------------------
     * password: Text --> actualizar_requisitos() -->
     * --------------------
     */
    private void actualizarRequisitos(String password) {
        boolean[] estados = Validador.requisitosPassword(password);
        for (int i = 0; i < requisitos.length; i++) {
            TextView vista = requisitos[i];
            boolean cumplido = estados[i];
            vista.setText((cumplido ? "✓ " : "○ ") + etiquetasRequisitos[i]);
            vista.setTextColor(ContextCompat.getColor(this,
                    cumplido ? R.color.success : R.color.muted));
        }
    }

    /**
     * --------------------
     * --> intentar_registro() -->
     * --------------------
     */
    private void intentarRegistro() {
        limpiarErrores();

        String nombre = texto(campoNombre);
        String apellidos = texto(campoApellidos);
        String email = texto(campoEmail);
        String password = texto(campoPassword);
        String confirmacion = texto(campoConfirmPassword);

        boolean hayErrorDeCampo = false;
        if (Validador.estaVacio(nombre)) {
            inputNombre.setError(getString(R.string.error_campo_obligatorio));
            hayErrorDeCampo = true;
        }
        if (Validador.estaVacio(apellidos)) {
            inputApellidos.setError(getString(R.string.error_campo_obligatorio));
            hayErrorDeCampo = true;
        }
        if (Validador.estaVacio(email)) {
            inputEmail.setError(getString(R.string.error_campo_obligatorio));
            hayErrorDeCampo = true;
        } else if (!Validador.emailValido(email)) {
            inputEmail.setError(getString(R.string.error_email_invalido));
            hayErrorDeCampo = true;
        }

        // Política de contraseña del backend: 8–72, mayúscula, minúscula, número y signo.
        String errorPassword = Validador.errorPassword(password);
        if (errorPassword != null) {
            inputPassword.setError(errorPassword);
            hayErrorDeCampo = true;
        }
        if (Validador.estaVacio(confirmacion)) {
            inputConfirmPassword.setError(getString(R.string.error_campo_obligatorio));
            hayErrorDeCampo = true;
        } else if (!password.equals(confirmacion)) {
            inputConfirmPassword.setError(getString(R.string.error_passwords_distintas));
            hayErrorDeCampo = true;
        }

        // Los términos de uso son obligatorios para darse de alta.
        if (!checkTerminos.isChecked()) {
            textoErrorTerminos.setText(R.string.error_terminos);
            textoErrorTerminos.setVisibility(View.VISIBLE);
            hayErrorDeCampo = true;
        }
        if (hayErrorDeCampo) {
            return;
        }

        botonRegistro.setEnabled(false);
        repositorio.registrar(nombre, apellidos, email, password, respuesta -> {
            botonRegistro.setEnabled(true);
            mostrarRespuestaRegistro(respuesta, email.trim(), password);
        });
    }

    private void mostrarRespuestaRegistro(RespuestaAuth respuesta, String email, String password) {
        switch (respuesta.estado) {
            case OK:
                iniciarSesionTrasRegistro(email, password);
                break;
            case CORREO_REGISTRADO:
                // Mensaje del backend: "Ese correo ya está registrado."
                inputEmail.setError(respuesta.mensaje.isEmpty()
                        ? getString(R.string.error_campo_obligatorio) : respuesta.mensaje);
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
     * registro_correcto() --> iniciar_sesion() --> elegir_destino() -->
     * --------------------
     *
     * Igual que `acceso.js`: después de registrar, la web llama a
     * `iniciarSesion()` y navega al panel que corresponda al rol. Así la app
     * y la web dejan al usuario en el mismo sitio tras darse de alta.
     */
    private void iniciarSesionTrasRegistro(String email, String password) {
        botonRegistro.setEnabled(false);
        repositorio.iniciarSesion(email, password, false, respuesta -> {
            botonRegistro.setEnabled(true);
            if (respuesta.estado == EstadoAuth.OK) {
                Class<?> destino = Rol.esAdmin(repositorio.usuario())
                        ? AdminActivity.class
                        : MainActivity.class;
                startActivity(new Intent(this, destino));
                finish();
                return;
            }
            // Cuenta creada pero sin sesión: se vuelve al login con el correo ya puesto.
            Intent datos = new Intent();
            datos.putExtra(LoginActivity.EXTRA_EMAIL_REGISTRADO, email);
            setResult(RESULT_OK, datos);
            finish();
        });
    }

    private void limpiarErrores() {
        inputNombre.setError(null);
        inputApellidos.setError(null);
        inputEmail.setError(null);
        inputPassword.setError(null);
        inputConfirmPassword.setError(null);
        textoError.setVisibility(View.GONE);
        textoErrorTerminos.setVisibility(View.GONE);
    }

    private void mostrarErrorGeneral(String mensaje) {
        textoError.setText(mensaje);
        textoError.setVisibility(View.VISIBLE);
    }

    private String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }
}
