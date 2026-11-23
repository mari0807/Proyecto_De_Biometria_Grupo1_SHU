import { logicaProxy } from "./logicaProxy.js";
import { montarMenuPerfil, actualizarLogoInicio } from "./perfilUi.js";
import {
  validarPassword,
  htmlInfoPassword,
  montarInfoPassword,
  montarChecklistPassword,
  REQUISITOS,
} from "./password.js";

const formLogin = document.querySelector("#form-login");
const formRegistro = document.querySelector("#form-registro");
const infoRegistro = document.querySelector("#register-password-info");
const listaReqs = document.querySelector("#register-password-reqs");
const inputRegistroPassword = document.querySelector("#register-password");
const errorPasswordRegistro = document.querySelector("#register-password-error");
const mensajeLogin = document.querySelector("#login-message");
const mensajeRegistro = document.querySelector("#register-message");
const authGuest = document.querySelector("#auth-guest");
const volverPanel = document.querySelector("#volver-panel");

function mostrar(elemento, texto, ok) {
  if (!elemento) return;
  elemento.hidden = false;
  elemento.textContent = texto;
  elemento.classList.toggle("ok", ok);
  elemento.classList.toggle("error", !ok);
}

function ocultar(elemento) {
  if (!elemento) return;
  elemento.hidden = true;
  elemento.textContent = "";
}

function destinoTrasLogin(rol) {
  return rol === "admin" ? "admin.html" : "user.html";
}

function mostrarRegistroExito(destino) {
  document.querySelector("#registro-exito")?.remove();
  const capa = document.createElement("div");
  capa.id = "registro-exito";
  capa.className = "registro-exito";
  capa.setAttribute("role", "status");
  capa.setAttribute("aria-live", "polite");
  capa.innerHTML = `
    <div class="registro-exito-card">
      <div class="registro-exito-check" aria-hidden="true">✓</div>
      <h2>¡Te has registrado con éxito!</h2>
      <p>Tu cuenta ya está lista. En un momento entramos…</p>
    </div>
  `;
  document.body.appendChild(capa);
  location.hash = "";
  setTimeout(() => location.assign(destino), 2200);
}

const menuPerfil = montarMenuPerfil({
  onCerrarSesion: async () => {
    await logicaProxy.cerrarSesion(logicaProxy.sesionActual()?.token || "");
    pintarSesion(null);
    location.hash = "";
    location.reload();
  },
});

function pintarSesion(usuario) {
  const hay = Boolean(usuario);
  if (authGuest) authGuest.hidden = hay;
  if (volverPanel) volverPanel.hidden = !hay || usuario.rol !== "admin";
  menuPerfil.pintar(hay ? usuario : null);
  actualizarLogoInicio(hay ? usuario : null);
}

if (infoRegistro) {
  infoRegistro.innerHTML = htmlInfoPassword("register-password-tip");
  montarInfoPassword(infoRegistro);
}
if (listaReqs) {
  listaReqs.innerHTML = REQUISITOS.map((r) => `<li data-req="${r.id}">${r.texto}</li>`).join("");
  montarChecklistPassword(inputRegistroPassword, listaReqs);
}

formLogin?.addEventListener("submit", async (evento) => {
  evento.preventDefault();
  const datos = new FormData(formLogin);
  const email = String(datos.get("email") || "").trim();
  const password = String(datos.get("password") || "");
  if (!email || !password) {
    mostrar(mensajeLogin, "Completa el correo y la contraseña.", false);
    return;
  }
  const boton = formLogin.querySelector("button[type=submit]");
  boton.disabled = true;
  const resultado = await logicaProxy.iniciarSesion(email, password);
  boton.disabled = false;
  if (resultado.obsoleta) return;
  if (!resultado.ok) {
    mostrar(mensajeLogin, resultado.mensaje, false);
    return;
  }
  location.assign(destinoTrasLogin(resultado.usuario.rol));
});

formRegistro?.addEventListener("submit", async (evento) => {
  evento.preventDefault();
  const datos = new FormData(formRegistro);
  const nombre = String(datos.get("nombre") || "").trim();
  const apellidos = String(datos.get("apellidos") || "").trim();
  const email = String(datos.get("email") || "").trim();
  const password = String(datos.get("password") || "");
  if (!nombre || !apellidos || !email || !password) {
    mostrar(mensajeRegistro, "Completa los campos obligatorios.", false);
    return;
  }
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
    mostrar(mensajeRegistro, "El correo electrónico no tiene un formato correcto.", false);
    return;
  }
  const errorPassword = validarPassword(password);
  if (errorPassword) {
    mostrar(errorPasswordRegistro, errorPassword, false);
    ocultar(mensajeRegistro);
    inputRegistroPassword?.focus();
    return;
  }
  ocultar(errorPasswordRegistro);
  const boton = formRegistro.querySelector("button[type=submit]");
  boton.disabled = true;
  const resultado = await logicaProxy.registrar(nombre, apellidos, email, password);
  if (resultado.obsoleta) {
    boton.disabled = false;
    return;
  }
  if (!resultado.ok) {
    boton.disabled = false;
    mostrar(mensajeRegistro, resultado.mensaje, false);
    return;
  }
  const sesion = await logicaProxy.iniciarSesion(email, password);
  if (sesion.obsoleta) {
    boton.disabled = false;
    return;
  }
  if (!sesion.ok) {
    boton.disabled = false;
    mostrarRegistroExito("user.html#login");
    return;
  }
  mostrarRegistroExito(destinoTrasLogin(sesion.usuario.rol));
});

pintarSesion(null);
const local = logicaProxy.sesionActual();
if (local) {
  pintarSesion(local);
  const remoto = await logicaProxy.comprobarSesion(local.token || "");
  if (remoto.obsoleta) {
    /* ignore */
  } else if (remoto.red) {
    pintarSesion(local);
  } else if (remoto.ok && remoto.usuario) {
    pintarSesion(remoto.usuario);
  } else {
    await logicaProxy.cerrarSesion(local.token || "");
    pintarSesion(null);
  }
}
