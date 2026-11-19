/**
 * Requisitos: mínimo 8, mayúscula, minúscula, número y signo especial.
 */

export const REQUISITOS = [
  { id: "len", texto: "Al menos 8 caracteres", ok: (c) => c.length >= 8 },
  { id: "may", texto: "Al menos una mayúscula (A-Z)", ok: (c) => /[A-ZÁÉÍÓÚÜÑ]/.test(c) },
  { id: "min", texto: "Al menos una minúscula (a-z)", ok: (c) => /[a-záéíóúüñ]/.test(c) },
  { id: "num", texto: "Al menos un número (0-9)", ok: (c) => /[0-9]/.test(c) },
  { id: "esp", texto: "Al menos un signo especial (!@#$…)", ok: (c) => /[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9]/.test(c) },
];

export const REQUISITOS_PASSWORD =
  "Mínimo 8 caracteres, con mayúscula, minúscula, número y signo especial.";

export function validarPassword(password) {
  const clave = String(password || "");
  if (clave === "") return "La contraseña es obligatoria.";
  if (clave.length > 72) return "La contraseña no puede superar 72 caracteres.";
  for (const req of REQUISITOS) {
    if (!req.ok(clave)) {
      return `La contraseña no cumple: ${req.texto.toLowerCase()}.`;
    }
  }
  return "";
}

export function htmlInfoPassword(idTip = "password-tip") {
  const items = REQUISITOS.map((r) => `<li data-req="${r.id}">${r.texto}</li>`).join("");
  return `
    <button type="button" class="info-btn" aria-label="Requisitos de la contraseña" aria-describedby="${idTip}">i</button>
    <div class="info-tip" id="${idTip}" role="tooltip" hidden>
      <p class="info-tip-title">La contraseña debe tener:</p>
      <ul class="password-reqs">${items}</ul>
    </div>
  `;
}

export function montarInfoPassword(raiz) {
  const boton = raiz.querySelector(".info-btn");
  const tip = raiz.querySelector(".info-tip");
  if (!boton || !tip) return;

  function abrir() {
    tip.hidden = false;
  }
  function cerrar() {
    tip.hidden = true;
  }

  boton.addEventListener("click", (evento) => {
    evento.preventDefault();
    tip.hidden = !tip.hidden;
  });
  boton.addEventListener("mouseenter", abrir);
  boton.addEventListener("focus", abrir);
  boton.addEventListener("mouseleave", () => {
    if (document.activeElement !== boton) cerrar();
  });
  boton.addEventListener("blur", cerrar);
  document.addEventListener("click", (evento) => {
    if (!raiz.contains(evento.target)) cerrar();
  });
}

export function montarChecklistPassword(input, lista) {
  if (!input || !lista) return;

  function pintar() {
    const clave = input.value;
    const vacia = clave.length === 0;
    lista.querySelectorAll("[data-req]").forEach((li) => {
      const req = REQUISITOS.find((r) => r.id === li.dataset.req);
      if (!req) return;
      li.classList.remove("ok", "ko");
      if (vacia) return;
      li.classList.add(req.ok(clave) ? "ok" : "ko");
    });
  }

  input.addEventListener("input", pintar);
  input.addEventListener("focus", () => {
    const tip = lista.closest(".info-tip");
    if (tip) tip.hidden = false;
    pintar();
  });
  pintar();
}
