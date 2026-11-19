import { logicaProxy } from "./logicaProxy.js";

const API_BASE = "http://127.0.0.1:8080";

/**
 * Logo QuéAire:
 * - sin sesión → landing
 * - usuario → user
 * - admin → admin (su panel)
 */
export function actualizarLogoInicio(usuarioOAutenticado) {
  let destino = "landing.html";
  let etiqueta = "QuéAire, ir a la landing";

  if (usuarioOAutenticado && typeof usuarioOAutenticado === "object") {
    if (usuarioOAutenticado.rol === "admin") {
      destino = "admin.html";
      etiqueta = "QuéAire, ir al panel de administración";
    } else {
      destino = "user.html";
      etiqueta = "QuéAire, ir a la página de usuario";
    }
  } else if (usuarioOAutenticado === true) {
    destino = "user.html";
    etiqueta = "QuéAire, ir a la página de usuario";
  }

  document.querySelectorAll("a.brand").forEach((enlace) => {
    enlace.href = destino;
    enlace.setAttribute("aria-label", etiqueta);
  });
}

export function iniciales(usuario) {
  const n = (usuario?.nombre || "").charAt(0);
  const a = (usuario?.apellidos || "").charAt(0);
  return `${n}${a}`.toUpperCase() || "QA";
}

export function urlFoto(usuario) {
  if (!usuario?.foto) return "";
  if (usuario.foto.startsWith("http") || usuario.foto.startsWith("data:")) return usuario.foto;
  return `${API_BASE}${usuario.foto.startsWith("/") ? "" : "/"}${usuario.foto}`;
}

export function pintarAvatar(nodo, usuario) {
  if (!nodo) return;
  const foto = urlFoto(usuario);
  if (foto) {
    nodo.textContent = "";
    nodo.style.backgroundImage = `url("${foto}")`;
    nodo.classList.add("has-photo");
  } else {
    nodo.style.backgroundImage = "";
    nodo.classList.remove("has-photo");
    nodo.textContent = iniciales(usuario);
  }
}

export function montarMenuPerfil({ onCerrarSesion }) {
  ensureEstilosPerfil();
  const menu = document.querySelector("#profile-menu");
  const toggle = document.querySelector("#profile-toggle");
  const dropdown = document.querySelector("#profile-dropdown");
  if (!menu || !toggle || !dropdown) return { pintar() {} };

  function estaAbierto() {
    return toggle.getAttribute("aria-expanded") === "true";
  }

  function cerrarMenu() {
    dropdown.hidden = true;
    dropdown.classList.remove("is-open");
    toggle.setAttribute("aria-expanded", "false");
  }

  function abrirMenu() {
    dropdown.hidden = false;
    dropdown.classList.add("is-open");
    toggle.setAttribute("aria-expanded", "true");
  }

  function pintar(usuario) {
    if (!usuario) {
      menu.hidden = true;
      cerrarMenu();
      return;
    }
    menu.hidden = false;
    cerrarMenu();
    document.querySelector("#profile-name").textContent = usuario.nombre;
    pintarAvatar(document.querySelector("#profile-avatar"), usuario);
  }

  function urlPerfil() {
    const desde = /admin\.html$/i.test(location.pathname) ? "admin" : "user";
    return `perfil.html?desde=${desde}`;
  }

  cerrarMenu();

  toggle.addEventListener("click", (evento) => {
    evento.preventDefault();
    evento.stopPropagation();
    if (estaAbierto()) cerrarMenu();
    else abrirMenu();
  });

  dropdown.addEventListener("click", async (evento) => {
    const boton = evento.target.closest("[data-accion]");
    if (!boton || boton.disabled) return;
    const accion = boton.dataset.accion;
    cerrarMenu();
    if (accion === "perfil") {
      location.assign(urlPerfil());
      return;
    }
    if (accion === "cerrar-sesion") {
      await onCerrarSesion?.();
      return;
    }
    if (accion === "borrar-cuenta") {
      await confirmarBorrarCuenta();
    }
  });

  document.addEventListener("click", (evento) => {
    if (!menu.contains(evento.target)) cerrarMenu();
  });

  document.addEventListener("keydown", (evento) => {
    if (evento.key === "Escape") cerrarMenu();
  });

  return { pintar };
}

export function leerArchivo(archivo) {
  return new Promise((resolve, reject) => {
    const lector = new FileReader();
    lector.onload = () => resolve(String(lector.result || ""));
    lector.onerror = () => reject(lector.error);
    lector.readAsDataURL(archivo);
  });
}

export async function confirmarBorrarCuenta() {
  return new Promise((resolve) => {
    ensureEstilosPerfil();
    let modal = document.querySelector("#modal-borrar-cuenta");
    if (modal) modal.remove();

    modal = document.createElement("div");
    modal.id = "modal-borrar-cuenta";
    modal.className = "perfil-confirm";
    modal.innerHTML = `
      <div class="perfil-confirm-backdrop" data-cerrar></div>
      <div class="perfil-confirm-card" role="dialog" aria-modal="true" aria-labelledby="borrar-titulo">
        <div id="borrar-paso-password">
          <h2 id="borrar-titulo">Borrar cuenta</h2>
          <p>Introduce tu contraseña para continuar.</p>
          <label for="borrar-password">Contraseña</label>
          <input id="borrar-password" type="password" autocomplete="current-password" placeholder="Tu contraseña">
          <p class="perfil-confirm-msg" id="borrar-mensaje" hidden></p>
          <div class="perfil-confirm-actions">
            <button type="button" class="perfil-confirm-btn ghost" data-cerrar>Cancelar</button>
            <button type="button" class="perfil-confirm-btn danger" id="borrar-siguiente">Continuar</button>
          </div>
        </div>
        <div id="borrar-paso-seguro" hidden>
          <h2>¿Estás seguro?</h2>
          <p>¿Estás seguro de que quieres borrar tu cuenta? Esta acción no se puede deshacer.</p>
          <p class="perfil-confirm-msg" id="borrar-mensaje-2" hidden></p>
          <div class="perfil-confirm-actions">
            <button type="button" class="perfil-confirm-btn ghost" data-cerrar>Cancelar</button>
            <button type="button" class="perfil-confirm-btn danger" id="borrar-confirmar">Confirmar</button>
          </div>
        </div>
      </div>
    `;
    document.body.appendChild(modal);

    const pasoPassword = modal.querySelector("#borrar-paso-password");
    const pasoSeguro = modal.querySelector("#borrar-paso-seguro");
    const input = modal.querySelector("#borrar-password");
    const mensaje = modal.querySelector("#borrar-mensaje");
    const mensaje2 = modal.querySelector("#borrar-mensaje-2");
    const btnSiguiente = modal.querySelector("#borrar-siguiente");
    const btnConfirmar = modal.querySelector("#borrar-confirmar");
    let password = "";

    function cerrar() {
      modal.remove();
      resolve(false);
    }

    modal.querySelectorAll("[data-cerrar]").forEach((nodo) => {
      nodo.onclick = cerrar;
    });

    btnSiguiente.onclick = () => {
      password = input.value;
      if (!password) {
        mensaje.hidden = false;
        mensaje.textContent = "Introduce tu contraseña.";
        mensaje.className = "perfil-confirm-msg error";
        return;
      }
      mensaje.hidden = true;
      pasoPassword.hidden = true;
      pasoSeguro.hidden = false;
    };

    btnConfirmar.onclick = async () => {
      btnConfirmar.disabled = true;
      const resultado = await logicaProxy.borrarCuenta(password);
      btnConfirmar.disabled = false;
      if (!resultado.ok) {
        mensaje2.hidden = false;
        mensaje2.textContent = resultado.mensaje || "No se ha podido borrar la cuenta.";
        mensaje2.className = "perfil-confirm-msg error";
        pasoSeguro.hidden = true;
        pasoPassword.hidden = false;
        password = "";
        input.value = "";
        input.focus();
        return;
      }
      modal.remove();
      location.assign("landing.html");
      resolve(true);
    };

    input.focus();
  });
}

function ensureEstilosPerfil() {
  if (document.querySelector("#perfil-ui-styles")) return;
  const estilo = document.createElement("style");
  estilo.id = "perfil-ui-styles";
  estilo.textContent = `
.profile-menu { position: relative; }
.profile {
  border: 1px solid var(--green, var(--blue, #198db1));
  background: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 6px 12px 6px 8px;
  border-radius: 999px;
  color: inherit;
  font: inherit;
  transition: background-color .18s ease, border-color .18s ease, box-shadow .18s ease, transform .15s ease;
}
.profile:hover {
  background: #eaf6fa;
  border-color: var(--deep, #155b91);
  box-shadow: 0 0 0 3px #198db122;
}
.avatar {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid var(--green, var(--blue, #198db1));
  background: #e6f2f6;
  color: var(--deep, #155b91);
  font-size: 11px;
  font-weight: 800;
  background-size: cover;
  background-position: center;
  overflow: hidden;
}
.avatar.has-photo { color: transparent; }
.profile-copy b { display: block; text-align: left; font-size: 14px; font-weight: 700; color: var(--deep, #155b91); }
.profile-dropdown { position: absolute; right: 0; top: calc(100% + 8px); min-width: 210px; padding: 8px; border: 1px solid var(--line, #e1eaf0); border-radius: 12px; background: #fff; box-shadow: 0 12px 30px #155b9120; z-index: 40; display: none; gap: 4px; }
.profile-dropdown.is-open { display: grid; }
.profile-dropdown[hidden] { display: none !important; }
.profile-item { border: 0; background: transparent; text-align: left; padding: 10px 12px; border-radius: 8px; color: var(--ink, #1c3c55); font: 600 12px "DM Sans", Arial, sans-serif; cursor: pointer; transition: background-color .18s ease, color .18s ease, box-shadow .18s ease; }
.profile-item:hover:not(:disabled) { background: #e7f4f9; color: var(--deep, #155b91); box-shadow: inset 0 0 0 1px #198db133; }
.profile-item:disabled { opacity: .45; cursor: default; }
.profile-item.danger { color: #b44555; }
.profile-item.danger:hover:not(:disabled) { background: #fdecef; color: #9c3040; box-shadow: inset 0 0 0 1px #e8a0aa55; }
.profile-separator { margin: 4px 6px; border: 0; border-top: 1px solid var(--line, #e1eaf0); }
.perfil-confirm { position: fixed; inset: 0; z-index: 80; display: grid; place-items: center; }
.perfil-confirm[hidden] { display: none !important; }
.perfil-confirm-backdrop { position: absolute; inset: 0; background: #0b2a4088; }
.perfil-confirm-card { position: relative; width: min(400px, calc(100vw - 32px)); padding: 22px; border-radius: 14px; background: #fff; border: 1px solid var(--line, #e1eaf0); box-shadow: 0 18px 40px #155b9133; }
.perfil-confirm-card h2 { margin: 0 0 8px; font: 800 20px/1.2 Manrope, Arial, sans-serif; color: var(--deep, #155b91); }
.perfil-confirm-card p { margin: 0 0 14px; color: #607887; font-size: 13px; line-height: 1.45; }
.perfil-confirm-card label { display: block; margin: 0 0 6px; font-size: 12px; font-weight: 700; color: var(--ink, #1c3c55); }
.perfil-confirm-card input { width: 100%; box-sizing: border-box; margin: 0 0 12px; padding: 10px 12px; border: 1px solid var(--line, #e1eaf0); border-radius: 9px; font: 500 13px "DM Sans", Arial, sans-serif; }
.perfil-confirm-msg { margin: 0 0 12px; font-size: 12px; }
.perfil-confirm-msg.error { color: #9c3040; }
.perfil-confirm-actions { display: flex; justify-content: flex-end; gap: 8px; }
.perfil-confirm-btn { border: 1px solid var(--line, #e1eaf0); border-radius: 9px; padding: 9px 14px; font: 700 12px "DM Sans", Arial, sans-serif; cursor: pointer; background: #fff; color: var(--deep, #155b91); }
.perfil-confirm-btn.ghost:hover { background: #eaf6fa; }
.perfil-confirm-btn.danger { background: #9c3040; border-color: #9c3040; color: #fff; }
.perfil-confirm-btn.danger:hover { filter: brightness(1.08); }
@media (max-width: 680px) {
  .profile-copy { display: none; }
  .profile { min-height: 40px; padding: 4px; }
}
`;
  document.head.appendChild(estilo);
}
