import { logicaProxy } from "./logicaProxy.js";
import { actualizarLogoInicio } from "./perfilUi.js";

function destino(usuario) {
  return usuario?.rol === "admin" ? "admin.html" : "user.html";
}

const local = logicaProxy.sesionActual();
if (!local) {
  actualizarLogoInicio(null);
} else {
  // Sesión viva: no te quedas en la landing.
  const remoto = await logicaProxy.comprobarSesion();
  if (remoto.ok && remoto.usuario) {
    location.replace(destino(remoto.usuario));
  } else if (!remoto.red) {
    await logicaProxy.cerrarSesion();
    actualizarLogoInicio(null);
  } else {
    // Sin red: usamos la sesión local igual para no dejar el limbo.
    location.replace(destino(local));
  }
}
