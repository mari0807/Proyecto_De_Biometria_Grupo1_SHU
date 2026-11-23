import { logicaProxy } from "./logicaProxy.js";
import { actualizarLogoInicio } from "./perfilUi.js";

function destino(usuario) {
  return usuario?.rol === "admin" ? "admin.html" : "user.html";
}

const local = logicaProxy.sesionActual();
if (!local) {
  actualizarLogoInicio(null);
} else {
  const remoto = await logicaProxy.comprobarSesion(local.token || "");
  if (remoto.ok && remoto.usuario) {
    location.replace(destino(remoto.usuario));
  } else if (!remoto.red) {
    await logicaProxy.cerrarSesion(local.token || "");
    actualizarLogoInicio(null);
  } else {
    location.replace(destino(local));
  }
}
