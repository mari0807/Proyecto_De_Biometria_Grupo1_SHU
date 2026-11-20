package com.grupo1.biometria.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.grupo1.biometria.R;
import com.grupo1.biometria.data.AuthRepository;
import com.grupo1.biometria.data.AuthRepositoryProvider;
import com.grupo1.biometria.data.ClienteAdmin;
import com.grupo1.biometria.data.SesionManager;
import com.grupo1.biometria.data.UrlServidor;
import com.grupo1.biometria.data.UsuarioAdmin;
import com.grupo1.biometria.util.Rol;

import java.util.List;

/**
 * Panel del administrador (rol «admin»): la misma gestión de usuarios que
 * `adminUsuarios.js` de la web, sobre los endpoints de `ApiAuth.php`.
 *
 * Listar (`GET /api/usuarios`), crear con contraseña temporal (`POST`), editar
 * datos/rol/baja lógica (`PUT`), borrar (`DELETE`) y regenerar contraseña
 * (`POST /api/usuarios/{id}/password`). Las reglas del propio backend se
 * replican aquí: el admin no puede darse de baja, quitarse el rol ni borrar
 * su propia cuenta; si el token caduca (401) se vuelve al login.
 */
public class AdminActivity extends AppCompatActivity {

    /** Roles que ofrece la web en sus desplegables (el backend valida igual). */
    private static final String[] ROLES = { "usuario", "admin" };

    private AuthRepository repositorio;
    private ClienteAdmin cliente;
    private AdaptadorUsuarios adaptador;

    private ProgressBar cargando;
    private TextView estado;
    private TextView vacio;

    /**
     * --------------------
     * savedInstanceState: Text? --> onCreate() -->
     * --------------------
     */
    @Override
    protected void onCreate(Bundle savedInstanceStateGuardado) {
        super.onCreate(savedInstanceStateGuardado);

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

        cargando = findViewById(R.id.admin_cargando);
        estado = findViewById(R.id.admin_estado);
        vacio = findViewById(R.id.admin_vacio);

        RecyclerView lista = findViewById(R.id.lista_usuarios);
        adaptador = new AdaptadorUsuarios(this::abrirEditor);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adaptador);

        findViewById(R.id.btn_nuevo_usuario).setOnClickListener(v -> abrirAlta());
        findViewById(R.id.btn_admin_reintentar).setOnClickListener(v -> cargarUsuarios());

        // Mismo token y servidor que el login (campo «Servidor»).
        cliente = new ClienteAdmin(UrlServidor.url(this), () -> {
            SesionManager sesion = AuthRepositoryProvider.sesion();
            return sesion == null ? null : sesion.token();
        });

        cargarUsuarios();
    }

    /**
     * --------------------
     * () --> () -->
     * --------------------
     */
    @Override
    protected void onDestroy() {
        if (cliente != null) {
            cliente.cerrar();
        }
        super.onDestroy();
    }

    /** -------------------- () --> GET /api/usuarios -------------------- */
    private void cargarUsuarios() {
        cargando.setVisibility(View.VISIBLE);
        estado.setVisibility(View.GONE);
        findViewById(R.id.btn_admin_reintentar).setVisibility(View.GONE);
        vacio.setVisibility(View.GONE);

        cliente.listar(new ClienteAdmin.ListadoCallback() {
            /** -------------------- usuarios: [ UsuarioAdmin ] --> () -------------------- */
            @Override
            public void onExito(List<UsuarioAdmin> usuarios) {
                cargando.setVisibility(View.GONE);
                adaptador.setUsuarios(usuarios);
                vacio.setVisibility(usuarios.isEmpty() ? View.VISIBLE : View.GONE);
            }

            /** -------------------- mensaje:Text, noAutorizado:B --> () -------------------- */
            @Override
            public void onError(String mensaje, boolean noAutorizado) {
                cargando.setVisibility(View.GONE);
                if (noAutorizado) {
                    cerrarSesionLocal();
                    irALogin();
                    return;
                }
                mostrarEstado(mensaje);
            }
        });
    }

    /** -------------------- () --> dialogo de alta -------------------- */
    private void abrirAlta() {
        View formulario = LayoutInflater.from(this).inflate(R.layout.dialog_usuario, null);
        TextView subtitulo = formulario.findViewById(R.id.dialog_subtitulo);
        TextView mensaje = formulario.findViewById(R.id.dialog_mensaje);
        TextInputEditText campoNombre = formulario.findViewById(R.id.campo_dialog_nombre);
        TextInputEditText campoApellidos = formulario.findViewById(R.id.campo_dialog_apellidos);
        TextInputEditText campoEmail = formulario.findViewById(R.id.campo_dialog_email);
        Spinner campoRol = prepararSpinner(formulario, 0);
        CheckBox campoActivo = formulario.findViewById(R.id.campo_dialog_activo);
        MaterialButton guardar = formulario.findViewById(R.id.btn_dialog_guardar);
        MaterialButton password = formulario.findViewById(R.id.btn_dialog_password);
        MaterialButton borrar = formulario.findViewById(R.id.btn_dialog_borrar);

        // El alta solo pide datos y rol (el backend genera la contraseña).
        subtitulo.setText(R.string.admin_alta_subtitulo);
        campoActivo.setVisibility(View.GONE);
        password.setVisibility(View.GONE);
        borrar.setVisibility(View.GONE);
        guardar.setText(R.string.btn_crear_usuario);

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle(R.string.btn_nuevo_usuario)
                .setView(formulario)
                .setNegativeButton(R.string.dialog_cancelar, null)
                .show();

        guardar.setOnClickListener(v -> {
            String nombre = texto(campoNombre);
            String apellidos = texto(campoApellidos);
            String email = texto(campoEmail);
            String rol = ROLES[campoRol.getSelectedItemPosition()];
            if (nombre.isEmpty() || apellidos.isEmpty() || email.isEmpty()) {
                mensajeFormulario(mensaje, getString(R.string.error_campo_obligatorio));
                return;
            }
            confirmar(getString(R.string.admin_confirmar_alta_titulo),
                    getString(R.string.admin_confirmar_alta_texto),
                    () -> cliente.crear(nombre, apellidos, email, rol, new ClienteAdmin.PasswordCallback() {
                        /** -------------------- temporal: Text --> () -------------------- */
                        @Override
                        public void onExito(String temporal) {
                            dialogo.dismiss();
                            mostrarPasswordTemporal(temporal);
                            cargarUsuarios();
                        }

                        /** -------------------- mensaje:Text, noAutorizado:B --> () -------------------- */
                        @Override
                        public void onError(String mensajeError, boolean noAutorizado) {
                            resultadoAdmin(mensajeError, noAutorizado);
                            if (!noAutorizado) {
                                mensajeFormulario(mensaje, mensajeError);
                            }
                        }
                    }));
        });
    }

    /** -------------------- usuario: UsuarioAdmin --> dialogo de edicion -------------------- */
    private void abrirEditor(UsuarioAdmin usuario) {
        boolean esMiCuenta = repositorio.usuario() != null
                && usuario.getId() == repositorio.usuario().idUsuario;

        View formulario = LayoutInflater.from(this).inflate(R.layout.dialog_usuario, null);
        TextView subtitulo = formulario.findViewById(R.id.dialog_subtitulo);
        TextView mensaje = formulario.findViewById(R.id.dialog_mensaje);
        TextInputEditText campoNombre = formulario.findViewById(R.id.campo_dialog_nombre);
        TextInputEditText campoApellidos = formulario.findViewById(R.id.campo_dialog_apellidos);
        TextInputEditText campoEmail = formulario.findViewById(R.id.campo_dialog_email);
        Spinner campoRol = prepararSpinner(formulario, "admin".equals(usuario.getRol()) ? 1 : 0);
        CheckBox campoActivo = formulario.findViewById(R.id.campo_dialog_activo);
        MaterialButton guardar = formulario.findViewById(R.id.btn_dialog_guardar);
        MaterialButton password = formulario.findViewById(R.id.btn_dialog_password);
        MaterialButton borrar = formulario.findViewById(R.id.btn_dialog_borrar);

        subtitulo.setText(getString(R.string.admin_editando_subtitulo, usuario.getId(), usuario.getEmail()));
        campoNombre.setText(usuario.getNombre());
        campoApellidos.setText(usuario.getApellidos());
        campoEmail.setText(usuario.getEmail());
        campoActivo.setChecked(usuario.isActivo());

        // Reglas del propio backend (Autenticacion.php): el admin no puede
        // darse de baja, quitarse el rol ni borrar su propia cuenta.
        if (esMiCuenta) {
            campoRol.setEnabled(false);
            campoActivo.setEnabled(false);
            borrar.setVisibility(View.GONE);
        }

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle(R.string.admin_gestion)
                .setView(formulario)
                .setNegativeButton(R.string.dialog_cancelar, null)
                .show();

        guardar.setOnClickListener(v -> {
            String nombre = texto(campoNombre);
            String apellidos = texto(campoApellidos);
            String email = texto(campoEmail);
            String rol = campoRol.isEnabled()
                    ? ROLES[campoRol.getSelectedItemPosition()]
                    : usuario.getRol();
            boolean activo = campoActivo.isEnabled() && campoActivo.isChecked();
            if (nombre.isEmpty() || apellidos.isEmpty() || email.isEmpty()) {
                mensajeFormulario(mensaje, getString(R.string.error_campo_obligatorio));
                return;
            }
            confirmar(getString(R.string.admin_confirmar_guardar_titulo),
                    getString(R.string.admin_confirmar_guardar_texto),
                    () -> cliente.actualizar(usuario.getId(), nombre, apellidos, email, activo, rol,
                            new ClienteAdmin.UsuarioCallback() {
                                /** -------------------- mensaje: Text --> () -------------------- */
                                @Override
                                public void onExito(String mensajeExito) {
                                    dialogo.dismiss();
                                    Toast.makeText(AdminActivity.this, mensajeExito, Toast.LENGTH_SHORT).show();
                                    cargarUsuarios();
                                }

                                /** -------------------- mensaje:Text, noAutorizado:B --> () -------------------- */
                                @Override
                                public void onError(String mensajeError, boolean noAutorizado) {
                                    resultadoAdmin(mensajeError, noAutorizado);
                                    if (!noAutorizado) {
                                        mensajeFormulario(mensaje, mensajeError);
                                    }
                                }
                            }));
        });

        password.setOnClickListener(v -> confirmar(
                getString(R.string.admin_confirmar_password_titulo),
                getString(R.string.admin_confirmar_password_texto),
                () -> cliente.nuevaPassword(usuario.getId(), new ClienteAdmin.PasswordCallback() {
                    /** -------------------- temporal: Text --> () -------------------- */
                    @Override
                    public void onExito(String temporal) {
                        dialogo.dismiss();
                        mostrarPasswordTemporal(temporal);
                    }

                    /** -------------------- mensaje:Text, noAutorizado:B --> () -------------------- */
                    @Override
                    public void onError(String mensajeError, boolean noAutorizado) {
                        resultadoAdmin(mensajeError, noAutorizado);
                        if (!noAutorizado) {
                            mensajeFormulario(mensaje, mensajeError);
                        }
                    }
                })));

        if (!esMiCuenta) {
            borrar.setOnClickListener(v -> confirmar(
                    getString(R.string.admin_confirmar_borrar_titulo),
                    getString(R.string.admin_confirmar_borrar_texto),
                    () -> cliente.borrar(usuario.getId(), new ClienteAdmin.UsuarioCallback() {
                        /** -------------------- mensaje: Text --> () -------------------- */
                        @Override
                        public void onExito(String mensajeExito) {
                            dialogo.dismiss();
                            Toast.makeText(AdminActivity.this, mensajeExito, Toast.LENGTH_SHORT).show();
                            cargarUsuarios();
                        }

                        /** -------------------- mensaje:Text, noAutorizado:B --> () -------------------- */
                        @Override
                        public void onError(String mensajeError, boolean noAutorizado) {
                            resultadoAdmin(mensajeError, noAutorizado);
                            if (!noAutorizado) {
                                mensajeFormulario(mensaje, mensajeError);
                            }
                        }
                    })));
        }
    }

    /** -------------------- temporal: Text --> dialogo -------------------- */
    private void mostrarPasswordTemporal(String temporal) {
        String valor = temporal == null ? "" : temporal.trim();
        if (valor.isEmpty()) {
            mostrarEstado(getString(R.string.admin_password_vacio));
            return;
        }
        View vista = LayoutInflater.from(this).inflate(R.layout.dialog_password, null);
        TextView texto = vista.findViewById(R.id.password_valor);
        texto.setText(valor);
        MaterialButton copiar = vista.findViewById(R.id.btn_copiar_password);

        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_password_titulo)
                .setView(vista)
                .setPositiveButton(R.string.detalle_cerrar, null)
                .show();

        copiar.setOnClickListener(v -> {
            ClipboardManager portapapeles =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (portapapeles != null) {
                portapapeles.setPrimaryClip(ClipData.newPlainText("QueAire", valor));
                Toast.makeText(this, R.string.admin_password_copiada, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** -------------------- formulario, seleccion --> spinner listo -------------------- */
    private Spinner prepararSpinner(View formulario, int seleccion) {
        Spinner campoRol = formulario.findViewById(R.id.campo_dialog_rol);
        ArrayAdapter<String> adaptadorRol =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ROLES);
        adaptadorRol.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        campoRol.setAdapter(adaptadorRol);
        campoRol.setSelection(seleccion);
        return campoRol;
    }

    /** -------------------- titulo:Text, texto:Text, accion --> () -------------------- */
    private void confirmar(String titulo, String texto, Runnable accion) {
        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(texto)
                .setPositiveButton(R.string.dialog_aceptar, (dialogo, cual) -> accion.run())
                .setNegativeButton(R.string.dialog_cancelar, null)
                .show();
    }

    /** -------------------- mensaje:Text, noAutorizado:B --> () -------------------- */
    private void resultadoAdmin(String mensaje, boolean noAutorizado) {
        if (noAutorizado) {
            cerrarSesionLocal();
            irALogin();
        }
    }

    /** -------------------- mensaje: Text --> () -------------------- */
    private void mostrarEstado(String mensaje) {
        estado.setText(mensaje);
        estado.setVisibility(View.VISIBLE);
        findViewById(R.id.btn_admin_reintentar).setVisibility(View.VISIBLE);
    }

    /** -------------------- destino, mensaje: Text --> () -------------------- */
    private void mensajeFormulario(TextView destino, String mensaje) {
        destino.setText(mensaje);
        destino.setVisibility(View.VISIBLE);
    }

    /**
     * --------------------
     * token_invalido() --> borrar_sesion_local() -->
     * --------------------
     */
    private void cerrarSesionLocal() {
        SesionManager sesion = AuthRepositoryProvider.sesion();
        if (sesion != null) {
            sesion.cerrar();
        }
    }

    /** -------------------- () --> () -------------------- */
    private void irALogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    /** -------------------- campo --> texto: Text -------------------- */
    private String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }
}
