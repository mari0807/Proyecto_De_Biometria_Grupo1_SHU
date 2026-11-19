import { logicaProxy } from "./logicaProxy.js";
import { montarMenuPerfil, actualizarLogoInicio } from "./perfilUi.js";
import { montarGestionUsuarios } from "./adminUsuarios.js";

const menuPerfil = montarMenuPerfil({
  onCerrarSesion: async () => {
    await logicaProxy.cerrarSesion();
    location.replace("landing.html");
  },
});

const sesion = await logicaProxy.comprobarSesion();
if (!sesion.ok) {
  await logicaProxy.cerrarSesion();
  location.replace("landing.html");
} else if (sesion.usuario.rol !== "admin") {
  location.replace("user.html");
} else {
  menuPerfil.pintar(sesion.usuario);
  actualizarLogoInicio(sesion.usuario);
  montarGestionUsuarios();
  document.body.hidden = false;
}
