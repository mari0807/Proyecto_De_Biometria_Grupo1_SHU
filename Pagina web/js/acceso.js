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
const enLanding = Boolean(formLogin && formRegistro);

const infoRegistro = document.querySelector("#register-password-info");
const listaReqs = document.querySelector("#register-password-reqs");
const inputRegistroPassword = document.querySelector("#register-password");
const errorPasswordRegistro = document.querySelector("#register-password-error");
const mensajeLogin = document.querySelector("#login-message");
const mensajeRegistro = document.querySelector("#register-message");
const acceso = document.querySelector("#acceso-portador");
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

function destino(rol) {
  return rol === "admin" ? "admin.html" : "user.html";
}

if (enLanding) {
  if (infoRegistro) {
    infoRegistro.innerHTML = htmlInfoPassword("register-password-tip");
    montarInfoPassword(infoRegistro);
  }
  if (listaReqs) {
    listaReqs.innerHTML = REQUISITOS.map((r) => `<li data-req="${r.id}">${r.texto}</li>`).join("");
    montarChecklistPassword(inputRegistroPassword, listaReqs);
  }

  formLogin.addEventListener("submit", async (evento) => {
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
    location.assign(destino(resultado.usuario.rol));
  });

  formRegistro.addEventListener("submit", async (evento) => {
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
    boton.disabled = false;
    if (sesion.obsoleta) return;
    if (!sesion.ok) {
      mostrar(mensajeRegistro, "Cuenta creada. Inicia sesión para continuar.", true);
      location.hash = "login";
      return;
    }
    location.assign(destino(sesion.usuario.rol));
  });
} else {
  // user.html / mapa: hace falta sesión; si no, vuelta a la landing con el login.
  const menuPerfil = montarMenuPerfil({
    onCerrarSesion: async () => {
      await logicaProxy.cerrarSesion();
      location.assign("landing.html");
    },
  });

  function pintarSesion(usuario) {
    const hay = Boolean(usuario);
    if (acceso) acceso.hidden = true;
    if (volverPanel) volverPanel.hidden = !hay || usuario.rol !== "admin";
    menuPerfil.pintar(hay ? usuario : null);
    actualizarLogoInicio(hay ? usuario : null);
  }

  const local = logicaProxy.sesionActual();
  if (!local) {
    location.replace("landing.html#login");
  } else {
    pintarSesion(local);
    const remoto = await logicaProxy.comprobarSesion();
    if (remoto.obsoleta) {
      /* ignore */
    } else if (remoto.red) {
      pintarSesion(local);
    } else if (remoto.ok && remoto.usuario) {
      pintarSesion(remoto.usuario);
    } else {
      await logicaProxy.cerrarSesion();
      location.replace("landing.html#login");
    }
  }
}
