import { logicaProxy } from "./logicaProxy.js";

const modalLista = document.querySelector("#modal-usuarios");
const modalAlta = document.querySelector("#modal-alta-usuario");
const modalEditar = document.querySelector("#modal-editar-usuario");
const modalConfirmar = document.querySelector("#modal-confirmar-usuario");
const modalPasswordTemp = document.querySelector("#modal-password-temp");
const tbody = document.querySelector("#usuarios-tbody");
const mensajeLista = document.querySelector("#usuarios-mensaje");
const mensajeEditar = document.querySelector("#editar-usuario-mensaje");
const mensajeAlta = document.querySelector("#alta-usuario-mensaje");
const form = document.querySelector("#form-editar-usuario");
const formAlta = document.querySelector("#form-alta-usuario");
const btnAbrir = document.querySelector("#btn-gestionar-usuarios");
const btnAlta = document.querySelector("#btn-alta-usuario");
const editId = document.querySelector("#edit-id");
const editNombre = document.querySelector("#edit-nombre");
const editApellidos = document.querySelector("#edit-apellidos");
const editEmail = document.querySelector("#edit-email");
const editRol = document.querySelector("#edit-rol");
const editActivo = document.querySelector("#edit-activo");
const altaNombre = document.querySelector("#alta-nombre");
const altaApellidos = document.querySelector("#alta-apellidos");
const altaEmail = document.querySelector("#alta-email");
const altaRol = document.querySelector("#alta-rol");
const confirmarTitulo = document.querySelector("#confirmar-usuario-titulo");
const confirmarTexto = document.querySelector("#confirmar-usuario-texto");
const passwordTempValor = document.querySelector("#password-temp-valor");

let pendiente = null;
let cacheUsuarios = [];

function mostrar(nodo, texto, ok = false) {
  if (!nodo) return;
  nodo.hidden = !texto;
  if (!texto) return;
  nodo.className = `form-message ${ok ? "ok" : "error"}`;
  nodo.textContent = texto;
}

function abrir(modal) {
  if (modal) modal.hidden = false;
}

function cerrar(modal) {
  if (modal) modal.hidden = true;
}

function escapar(texto) {
  return String(texto ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function filaHtml(u) {
  const estado = u.activo
    ? '<span class="pill active"><i></i> Activo</span>'
    : '<span class="pill offline"><i></i> Baja</span>';
  return `
    <tr data-id="${u.id_usuario}">
      <td>${u.id_usuario}</td>
      <td><b>${escapar(u.nombre)}</b></td>
      <td>${escapar(u.apellidos)}</td>
      <td>${escapar(u.email)}</td>
      <td>${escapar(u.rol)}</td>
      <td>${estado}</td>
      <td><button type="button" class="row-action" data-editar="${u.id_usuario}">Editar</button></td>
    </tr>
  `;
}

async function cargarUsuarios() {
  tbody.innerHTML = '<tr><td colspan="7">Cargando…</td></tr>';
  const resultado = await logicaProxy.listarUsuarios();
  if (!resultado.ok) {
    cacheUsuarios = [];
    tbody.innerHTML = `<tr><td colspan="7">${escapar(resultado.mensaje || "No se pudo cargar.")}</td></tr>`;
    mostrar(mensajeLista, resultado.mensaje, false);
    return;
  }
  cacheUsuarios = resultado.usuarios;
  mostrar(mensajeLista, "", false);
  if (!cacheUsuarios.length) {
    tbody.innerHTML = '<tr><td colspan="7">No hay usuarios.</td></tr>';
    return;
  }
  tbody.innerHTML = cacheUsuarios.map(filaHtml).join("");
}

function abrirEdicion(usuario) {
  editId.value = String(usuario.id_usuario);
  editNombre.value = usuario.nombre;
  editApellidos.value = usuario.apellidos;
  editEmail.value = usuario.email;
  editRol.value = usuario.rol === "admin" ? "admin" : "usuario";
  editActivo.checked = Boolean(usuario.activo);
  mostrar(mensajeEditar, "", false);
  document.querySelector("#editar-usuario-sub").textContent =
    `Editando #${usuario.id_usuario} · ${usuario.email}`;
  abrir(modalEditar);
}

function pedirConfirmacion(accion, titulo, texto, datos) {
  pendiente = { accion, ...datos };
  confirmarTitulo.textContent = titulo;
  confirmarTexto.textContent = texto;
  abrir(modalConfirmar);
}

export async function abrirGestionUsuarios() {
  if (!modalLista) return;
  abrir(modalLista);
  await cargarUsuarios();
}

btnAbrir?.addEventListener("click", abrirGestionUsuarios);

btnAlta?.addEventListener("click", () => {
  formAlta?.reset();
  mostrar(mensajeAlta, "", false);
  abrir(modalAlta);
});

modalLista?.querySelectorAll("[data-cerrar-usuarios]").forEach((el) => {
  el.addEventListener("click", () => cerrar(modalLista));
});

modalAlta?.querySelectorAll("[data-cerrar-alta]").forEach((el) => {
  el.addEventListener("click", () => {
    cerrar(modalAlta);
    pendiente = null;
  });
});

modalEditar?.querySelectorAll("[data-cerrar-editar]").forEach((el) => {
  el.addEventListener("click", () => {
    cerrar(modalEditar);
    pendiente = null;
  });
});

formAlta?.addEventListener("submit", (evento) => {
  evento.preventDefault();
  const datos = {
    nombre: altaNombre.value.trim(),
    apellidos: altaApellidos.value.trim(),
    email: altaEmail.value.trim(),
    rol: altaRol.value,
  };
  if (!datos.nombre || !datos.apellidos || !datos.email) {
    mostrar(mensajeAlta, "Completa todos los campos.", false);
    return;
  }
  pedirConfirmacion(
    "alta",
    "¿Crear este usuario?",
    "Se dará de alta la cuenta y se generará una contraseña temporal. ¿Continuar?",
    datos,
  );
});

modalConfirmar?.querySelectorAll("[data-cerrar-confirmar]").forEach((el) => {
  el.addEventListener("click", () => {
    cerrar(modalConfirmar);
    pendiente = null;
  });
});

modalPasswordTemp?.querySelectorAll("[data-cerrar-password-temp]").forEach((el) => {
  el.addEventListener("click", () => {
    cerrar(modalPasswordTemp);
    passwordTempValor.textContent = "";
  });
});

tbody?.addEventListener("click", (evento) => {
  const boton = evento.target.closest("[data-editar]");
  if (!boton) return;
  const id = Number(boton.dataset.editar);
  const usuario = cacheUsuarios.find((u) => u.id_usuario === id);
  if (!usuario) {
    mostrar(mensajeLista, "No se ha encontrado ese usuario.", false);
    return;
  }
  abrirEdicion(usuario);
});

form?.addEventListener("submit", (evento) => {
  evento.preventDefault();
  const datos = {
    id_usuario: Number(editId.value),
    nombre: editNombre.value.trim(),
    apellidos: editApellidos.value.trim(),
    email: editEmail.value.trim(),
    rol: editRol.value,
    activo: editActivo.checked,
  };
  if (!datos.nombre || !datos.apellidos || !datos.email) {
    mostrar(mensajeEditar, "Completa todos los campos.", false);
    return;
  }
  pedirConfirmacion(
    "guardar",
    "¿Estás seguro?",
    "¿Estás seguro de que quieres aplicar estos cambios?",
    datos,
  );
});

document.querySelector("#btn-gen-password")?.addEventListener("click", () => {
  pedirConfirmacion(
    "password",
    "¿Generar contraseña nueva?",
    "Se creará una contraseña temporal. La anterior no se puede ver (solo hay un hash). ¿Continuar?",
    { id_usuario: Number(editId.value) },
  );
});

document.querySelector("#btn-borrar-usuario")?.addEventListener("click", () => {
  pedirConfirmacion(
    "borrar",
    "¿Borrar esta cuenta?",
    "Se eliminará el usuario de la base de datos. Esta acción no se puede deshacer. ¿Estás seguro?",
    { id_usuario: Number(editId.value) },
  );
});

document.querySelector("#btn-copiar-password")?.addEventListener("click", async () => {
  const texto = passwordTempValor.textContent || "";
  if (!texto) return;
  try {
    await navigator.clipboard.writeText(texto);
    mostrar(mensajeLista, "Contraseña copiada al portapapeles.", true);
  } catch {
    mostrar(mensajeLista, "No se pudo copiar. Selecciónala a mano.", false);
  }
});

document.querySelector("#btn-confirmar-usuario")?.addEventListener("click", async () => {
  if (!pendiente) return;
  const btn = document.querySelector("#btn-confirmar-usuario");
  btn.disabled = true;
  const accion = pendiente.accion;
  const id = pendiente.id_usuario;

  if (accion === "alta") {
    const resultado = await logicaProxy.crearUsuarioAdmin(pendiente);
    btn.disabled = false;
    cerrar(modalConfirmar);
    pendiente = null;
    if (!resultado.ok) {
      mostrar(mensajeAlta, resultado.mensaje, false);
      return;
    }
    cerrar(modalAlta);
    passwordTempValor.textContent = resultado.password_temporal;
    abrir(modalPasswordTemp);
    mostrar(mensajeLista, resultado.mensaje || "Usuario creado.", true);
    await cargarUsuarios();
    return;
  }

  if (accion === "guardar") {
    const resultado = await logicaProxy.actualizarUsuarioAdmin(id, pendiente);
    btn.disabled = false;
    cerrar(modalConfirmar);
    pendiente = null;
    if (!resultado.ok) {
      mostrar(mensajeEditar, resultado.mensaje, false);
      return;
    }
    cerrar(modalEditar);
    mostrar(mensajeLista, resultado.mensaje || "Usuario actualizado.", true);
    await cargarUsuarios();
    return;
  }

  if (accion === "borrar") {
    const resultado = await logicaProxy.borrarUsuarioAdmin(id);
    btn.disabled = false;
    cerrar(modalConfirmar);
    pendiente = null;
    if (!resultado.ok) {
      mostrar(mensajeEditar, resultado.mensaje, false);
      return;
    }
    cerrar(modalEditar);
    mostrar(mensajeLista, resultado.mensaje || "Cuenta eliminada.", true);
    await cargarUsuarios();
    return;
  }

  if (accion === "password") {
    const resultado = await logicaProxy.generarPasswordAdmin(id);
    btn.disabled = false;
    cerrar(modalConfirmar);
    pendiente = null;
    if (!resultado.ok) {
      mostrar(mensajeEditar, resultado.mensaje, false);
      return;
    }
    passwordTempValor.textContent = resultado.password_temporal;
    abrir(modalPasswordTemp);
    mostrar(mensajeLista, resultado.mensaje, true);
    return;
  }

  btn.disabled = false;
  pendiente = null;
});

document.addEventListener("keydown", (evento) => {
  if (evento.key !== "Escape") return;
  if (!modalPasswordTemp?.hidden) {
    cerrar(modalPasswordTemp);
    passwordTempValor.textContent = "";
  } else if (!modalConfirmar?.hidden) {
    cerrar(modalConfirmar);
    pendiente = null;
  } else if (!modalAlta?.hidden) {
    cerrar(modalAlta);
  } else if (!modalEditar?.hidden) {
    cerrar(modalEditar);
  } else if (!modalLista?.hidden) {
    cerrar(modalLista);
  }
});

export function montarGestionUsuarios() {}
