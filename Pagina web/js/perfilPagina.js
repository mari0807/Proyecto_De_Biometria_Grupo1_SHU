import { logicaProxy } from "./logicaProxy.js";
import { pintarAvatar, leerArchivo, confirmarBorrarCuenta, actualizarLogoInicio } from "./perfilUi.js";
import {
  validarPassword,
  htmlInfoPassword,
  montarInfoPassword,
  montarChecklistPassword,
  REQUISITOS,
} from "./password.js";

const params = new URLSearchParams(location.search);
const desde = params.get("desde") === "admin" ? "admin" : "user";
const volverUrl = desde === "admin" ? "admin.html" : "user.html";

document.querySelector("#volver-origen").href = volverUrl;
const btnVolverCuenta = document.querySelector("#btn-volver-cuenta");
if (btnVolverCuenta) btnVolverCuenta.href = volverUrl;

const preview = document.querySelector("#perfil-preview");
const mensajePagina = document.querySelector("#perfil-message");
const modal = document.querySelector("#modal-editar");
const formModal = document.querySelector("#form-modal");
const modalTitulo = document.querySelector("#modal-titulo");
const modalSub = document.querySelector("#modal-sub");
const modalCampo = document.querySelector("#modal-campo");
const modalMensaje = document.querySelector("#modal-message");
const modalPassword = document.querySelector("#modal-password");

let usuario = null;
let campoActual = "";
let fotoBase64 = "";
let quitarFoto = false;
let fotoPendiente = "";

const CAMPOS = {
  foto: {
    titulo: "Editar foto",
    sub: "Elige una imagen, confírmala y guarda con tu contraseña. También puedes quitar la actual.",
  },
  nombre: {
    titulo: "Editar nombre",
    sub: "Escribe el nuevo nombre y confirma con tu contraseña.",
  },
  apellidos: {
    titulo: "Editar apellidos",
    sub: "Escribe los nuevos apellidos y confirma con tu contraseña.",
  },
  email: {
    titulo: "Editar correo",
    sub: "Escribe el nuevo correo y confirma con tu contraseña.",
  },
  password: {
    titulo: "Editar contraseña",
    sub: "Elige una contraseña nueva y confirma con la actual.",
  },
};

function mostrar(nodo, texto, ok) {
  nodo.hidden = !texto;
  if (!texto) return;
  nodo.className = `form-message ${ok ? "ok" : "error"}`;
  nodo.textContent = texto;
}

function pintar() {
  document.querySelector("#valor-nombre").textContent = usuario.nombre || "—";
  document.querySelector("#valor-apellidos").textContent = usuario.apellidos || "—";
  document.querySelector("#valor-email").textContent = usuario.email || "—";
  pintarAvatar(preview, usuario);
}

function cerrarModal() {
  modal.hidden = true;
  formModal.reset();
  fotoBase64 = "";
  fotoPendiente = "";
  quitarFoto = false;
  modalCampo.innerHTML = "";
  mostrar(modalMensaje, "", false);
  campoActual = "";
}

function pintarMiniPreview(nodo, src) {
  if (!nodo) return;
  if (src) {
    nodo.style.backgroundImage = `url("${src}")`;
    nodo.classList.add("has-photo");
    nodo.textContent = "";
  } else {
    nodo.style.backgroundImage = "";
    nodo.classList.remove("has-photo");
    const n = (usuario?.nombre || "").charAt(0);
    const a = (usuario?.apellidos || "").charAt(0);
    nodo.textContent = `${n}${a}`.toUpperCase() || "QA";
  }
}

function abrirModal(campo) {
  const meta = CAMPOS[campo];
  if (!meta) return;
  campoActual = campo;
  fotoBase64 = "";
  fotoPendiente = "";
  quitarFoto = false;
  mostrar(modalMensaje, "", false);
  mostrar(mensajePagina, "", false);
  modalTitulo.textContent = meta.titulo;
  modalSub.textContent = meta.sub;
  modalPassword.value = "";

  if (campo === "foto") {
    const tieneFoto = Boolean(usuario?.foto);
    modalCampo.innerHTML = `
      <div class="foto-edit">
        <span class="avatar foto-mini" id="foto-mini-preview">QA</span>
        <div class="foto-edit-acciones">
          <label for="modal-valor" class="btn ghost foto-file-label">Elegir foto</label>
          <input id="modal-valor" name="valor" type="file" accept="image/png,image/jpeg,image/webp" hidden>
          ${tieneFoto ? '<button type="button" class="btn ghost" id="btn-quitar-foto">Quitar foto actual</button>' : ""}
        </div>
      </div>
      <div class="foto-confirm" id="foto-confirm" hidden>
        <p class="hint">¿Quieres usar esta foto?</p>
        <div class="foto-confirm-acciones">
          <button type="button" class="btn ghost" id="foto-rechazar">No, otra</button>
          <button type="button" class="btn primary" id="foto-aceptar">Sí, esta</button>
        </div>
      </div>
      <p class="hint" id="foto-estado">${tieneFoto ? "Puedes cambiarla o quitarla." : "Aún no tienes foto de perfil."}</p>
      <p class="hint">PNG, JPG o WebP · máx. 2 MB</p>
    `;

    const inputFile = modalCampo.querySelector("#modal-valor");
    const mini = modalCampo.querySelector("#foto-mini-preview");
    const bloqueConfirm = modalCampo.querySelector("#foto-confirm");
    const estado = modalCampo.querySelector("#foto-estado");
    const urlActual = usuario?.foto
      ? (usuario.foto.startsWith("http") || usuario.foto.startsWith("data:")
        ? usuario.foto
        : `http://127.0.0.1:8080${usuario.foto}`)
      : "";

    pintarMiniPreview(mini, urlActual);

    inputFile.addEventListener("change", async (evento) => {
      const archivo = evento.target.files?.[0];
      fotoPendiente = "";
      if (!archivo) return;
      if (archivo.size > 2_000_000) {
        mostrar(modalMensaje, "La foto no puede superar 2 MB.", false);
        evento.target.value = "";
        return;
      }
      fotoPendiente = await leerArchivo(archivo);
      pintarMiniPreview(mini, fotoPendiente);
      bloqueConfirm.hidden = false;
      estado.textContent = "Confirma si quieres esta foto.";
      mostrar(modalMensaje, "", false);
    });

    modalCampo.querySelector("#foto-aceptar")?.addEventListener("click", () => {
      if (!fotoPendiente) return;
      fotoBase64 = fotoPendiente;
      quitarFoto = false;
      fotoPendiente = "";
      bloqueConfirm.hidden = true;
      estado.textContent = "Foto lista. Confirma con tu contraseña para guardarla.";
      mostrar(modalMensaje, "", false);
    });

    modalCampo.querySelector("#foto-rechazar")?.addEventListener("click", () => {
      fotoPendiente = "";
      inputFile.value = "";
      bloqueConfirm.hidden = true;
      if (fotoBase64) {
        pintarMiniPreview(mini, fotoBase64);
        estado.textContent = "Foto lista. Confirma con tu contraseña para guardarla.";
      } else if (quitarFoto) {
        pintarMiniPreview(mini, "");
        estado.textContent = "Se quitará la foto al guardar.";
      } else {
        pintarMiniPreview(mini, urlActual);
        estado.textContent = tieneFoto ? "Puedes cambiarla o quitarla." : "Aún no tienes foto de perfil.";
      }
    });

    modalCampo.querySelector("#btn-quitar-foto")?.addEventListener("click", () => {
      fotoBase64 = "";
      fotoPendiente = "";
      quitarFoto = true;
      inputFile.value = "";
      bloqueConfirm.hidden = true;
      pintarMiniPreview(mini, "");
      estado.textContent = "Se quitará la foto al guardar. Confirma con tu contraseña.";
      mostrar(modalMensaje, "", false);
    });
  } else if (campo === "password") {
    modalCampo.innerHTML = `
      <div class="label-con-info">
        <label for="modal-valor">Nueva contraseña</label>
        <span class="info-wrap" id="modal-password-info">${htmlInfoPassword("modal-password-tip")}</span>
      </div>
      <input id="modal-valor" name="valor" type="password" autocomplete="new-password" required placeholder="Nueva contraseña">
      <ul class="password-reqs live" id="modal-password-reqs">
        ${REQUISITOS.map((r) => `<li data-req="${r.id}">${r.texto}</li>`).join("")}
      </ul>
      <label for="modal-valor-2">Repite la nueva contraseña</label>
      <input id="modal-valor-2" name="valor2" type="password" autocomplete="new-password" required placeholder="Repite la nueva contraseña">
    `;
    const wrapInfo = modalCampo.querySelector("#modal-password-info");
    montarInfoPassword(wrapInfo);
    montarChecklistPassword(
      modalCampo.querySelector("#modal-valor"),
      modalCampo.querySelector("#modal-password-reqs"),
    );
  } else if (campo === "email") {
    modalCampo.innerHTML = `
      <label for="modal-valor">Nuevo correo electrónico</label>
      <input id="modal-valor" name="valor" type="email" autocomplete="email" required placeholder="nuevo@correo.com">
    `;
  } else {
    modalCampo.innerHTML = `
      <label for="modal-valor">${campo === "nombre" ? "Nuevo nombre" : "Nuevos apellidos"}</label>
      <input id="modal-valor" name="valor" type="text" required placeholder="${campo === "nombre" ? "Tu nombre" : "Tus apellidos"}">
    `;
  }

  modal.hidden = false;
  const primero = modalCampo.querySelector("#modal-valor");
  if (primero && campo !== "foto") primero.focus();
  else modalPassword.focus();
}

document.querySelectorAll("[data-campo]").forEach((boton) => {
  boton.addEventListener("click", () => abrirModal(boton.dataset.campo));
});

modal.querySelectorAll("[data-cerrar]").forEach((el) => {
  el.addEventListener("click", cerrarModal);
});

document.addEventListener("keydown", (evento) => {
  if (evento.key === "Escape" && !modal.hidden) cerrarModal();
});

formModal.addEventListener("submit", async (evento) => {
  evento.preventDefault();
  const password = modalPassword.value;
  if (!password) {
    mostrar(modalMensaje, "Debes confirmar con tu contraseña.", false);
    return;
  }

  const payload = {
    nombre: usuario.nombre,
    apellidos: usuario.apellidos,
    email: usuario.email,
    password,
    password_nueva: "",
    foto_base64: "",
    quitar_foto: false,
  };

  if (campoActual === "foto") {
    if (fotoPendiente) {
      mostrar(modalMensaje, "Confirma si quieres esa foto (Sí, esta).", false);
      return;
    }
    if (!fotoBase64 && !quitarFoto) {
      mostrar(modalMensaje, "Elige una foto o quita la actual.", false);
      return;
    }
    payload.foto_base64 = fotoBase64;
    payload.quitar_foto = quitarFoto;
  } else if (campoActual === "password") {
    const nueva = formModal.valor.value;
    const confirmacion = formModal.valor2.value;
    if (nueva !== confirmacion) {
      mostrar(modalMensaje, "Las contraseñas no coinciden.", false);
      return;
    }
    const errorPassword = validarPassword(nueva);
    if (errorPassword) {
      mostrar(modalMensaje, errorPassword, false);
      return;
    }
    payload.password_nueva = nueva;
  } else if (campoActual === "nombre") {
    const nombre = formModal.valor.value.trim();
    if (!nombre) {
      mostrar(modalMensaje, "El nombre es obligatorio.", false);
      return;
    }
    payload.nombre = nombre;
  } else if (campoActual === "apellidos") {
    const apellidos = formModal.valor.value.trim();
    if (!apellidos) {
      mostrar(modalMensaje, "Los apellidos son obligatorios.", false);
      return;
    }
    payload.apellidos = apellidos;
  } else if (campoActual === "email") {
    const email = formModal.valor.value.trim();
    if (!email) {
      mostrar(modalMensaje, "El correo es obligatorio.", false);
      return;
    }
    payload.email = email;
  }

  const boton = formModal.querySelector('button[type="submit"]');
  boton.disabled = true;
  const resultado = await logicaProxy.actualizarPerfil(payload);
  boton.disabled = false;
  if (resultado.obsoleta) return;

  if (!resultado.ok) {
    mostrar(modalMensaje, resultado.mensaje, false);
    return;
  }

  usuario = { ...resultado.usuario };
  pintar();
  cerrarModal();
  mostrar(mensajePagina, resultado.mensaje, true);
});

document.querySelector("#btn-cerrar-sesion")?.addEventListener("click", async () => {
  await logicaProxy.cerrarSesion();
  location.assign(volverUrl);
});

document.querySelector("#btn-borrar-cuenta")?.addEventListener("click", () => {
  confirmarBorrarCuenta();
});

const sesion = await logicaProxy.comprobarSesion();
if (!sesion.ok) {
  await logicaProxy.cerrarSesion();
  location.replace("landing.html");
} else {
  usuario = { ...sesion.usuario };
  actualizarLogoInicio(usuario);
  pintar();
  document.body.hidden = false;
}
