import { abrirGestionUsuarios } from "./adminUsuarios.js";

/**
 * Navegación del panel de administración.
 *
 * El menú lateral ya no usa anclas: cambia la vista activa (Resumen/Sensores)
 * o abre directamente la ventana de la sección, sin desplazar la página.
 */

const contenido = document.querySelector("main.content");
const modal = document.querySelector("#modal-seccion");
const modalTitulo = document.querySelector("#seccion-titulo");
const modalKicker = document.querySelector("#seccion-kicker");
const modalTexto = document.querySelector("#seccion-texto");
const enlacesSidebar = [...document.querySelectorAll(".sidebar [data-seccion]")];

const VISTAS = new Set(["resumen", "sensores"]);

const MODALES = {
  historial: {
    kicker: "DATOS HISTÓRICOS",
    titulo: "Histórico de mapas",
    texto:
      "Consulta mapas anteriores para informes, auditorías o seguimiento de medidas correctoras.",
  },
  comunicaciones: {
    kicker: "COMUNICACIONES",
    titulo: "Enviar comunicaciones",
    texto: "Prepara comunicaciones dirigidas a los usuarios de la plataforma.",
  },
  recompensas: {
    kicker: "GESTIÓN ECONÓMICA",
    titulo: "Cobros y recompensas",
    texto: "Gestiona los cobros y las recompensas de la plataforma.",
  },
};

function cambiarHash(seccion) {
  if (location.hash === `#${seccion}`) return;
  history.replaceState(null, "", `#${seccion}`);
}

function scrollArriba() {
  const html = document.documentElement;
  const previo = html.style.scrollBehavior;
  html.style.scrollBehavior = "auto";
  window.scrollTo(0, 0);
  html.style.scrollBehavior = previo;
}

function marcarVista(vista) {
  enlacesSidebar.forEach((enlace) => {
    const activo = enlace.dataset.seccion === vista;
    enlace.classList.toggle("active", activo);
    if (activo) enlace.setAttribute("aria-current", "page");
    else enlace.removeAttribute("aria-current");
  });
}

function mostrarVista(vista) {
  if (!contenido) return;
  contenido.dataset.vista = vista;
  marcarVista(vista);
  scrollArriba();
  cambiarHash(vista);
}

function abrirModal(seccion) {
  const datos = MODALES[seccion];
  if (!datos || !modal) return;
  if (modalKicker) modalKicker.textContent = datos.kicker;
  if (modalTitulo) modalTitulo.textContent = datos.titulo;
  if (modalTexto) modalTexto.textContent = datos.texto;
  modal.hidden = false;
  modal.querySelector(".admin-modal-close")?.focus();
}

function cerrarModal() {
  if (!modal || modal.hidden) return;
  modal.hidden = true;
  const vista = contenido?.dataset.vista || "resumen";
  cambiarHash(vista);
}

function irA(seccion) {
  if (!seccion) return;
  if (VISTAS.has(seccion)) {
    mostrarVista(seccion);
    return;
  }
  if (seccion === "usuarios") {
    abrirGestionUsuarios();
    return;
  }
  abrirModal(seccion);
}

export function montarPanelAdmin() {
  document.querySelectorAll("[data-seccion]").forEach((elemento) => {
    elemento.addEventListener("click", (evento) => {
      evento.preventDefault();
      irA(elemento.dataset.seccion);
    });
  });

  modal?.querySelectorAll("[data-cerrar-seccion]").forEach((elemento) => {
    elemento.addEventListener("click", cerrarModal);
  });

  document.addEventListener("keydown", (evento) => {
    if (evento.key === "Escape") cerrarModal();
  });

  const inicial = location.hash.replace("#", "");
  if (inicial) irA(inicial);
}
