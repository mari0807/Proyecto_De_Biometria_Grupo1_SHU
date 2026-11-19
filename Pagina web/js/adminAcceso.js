import { logicaProxy } from "./logicaProxy.js";
import { montarMenuPerfil, actualizarLogoInicio } from "./perfilUi.js";
import { montarGestionUsuarios } from "./adminUsuarios.js";
import { montarPanelAdmin } from "./adminPanel.js";

const menuPerfil = montarMenuPerfil({
  onCerrarSesion: async () => {
    await logicaProxy.cerrarSesion();
    location.replace("user.html");
  },
});

const sesion = await logicaProxy.comprobarSesion();
if (!sesion.ok) {
  await logicaProxy.cerrarSesion();
  location.replace("user.html#login");
} else if (sesion.usuario.rol !== "admin") {
  location.replace("user.html");
} else {
  menuPerfil.pintar(sesion.usuario);
  actualizarLogoInicio(sesion.usuario);
  montarGestionUsuarios();
  montarPanelAdmin();
  document.body.hidden = false;
}
