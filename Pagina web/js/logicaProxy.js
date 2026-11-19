const CLAVE = "queaire_sesion";

function expiracionMs(token) {
  const partes = String(token).split(".");
  if (partes.length !== 3) return null;
  try {
    const base64 = partes[1].replace(/-/g, "+").replace(/_/g, "/");
    const relleno = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
    const json = JSON.parse(atob(relleno));
    return typeof json.exp === "number" ? json.exp * 1000 : null;
  } catch {
    return null;
  }
}

function leerJson(texto) {
  try {
    return JSON.parse(texto);
  } catch {
    return null;
  }
}

export function crearLogicaProxy({ fetchImpl, almacenamiento, apiBase, ahora = () => Date.now() }) {
  let generacion = 0;

  function leer() {
    const crudo = almacenamiento.getItem(CLAVE);
    const sesion = leerJson(crudo);
    if (!sesion || typeof sesion.token !== "string" || !sesion.usuario) {
      if (crudo) almacenamiento.removeItem(CLAVE);
      return null;
    }
    const expira = expiracionMs(sesion.token);
    if (expira !== null && expira <= ahora()) {
      almacenamiento.removeItem(CLAVE);
      return null;
    }
    return sesion;
  }

  function guardar(sesion) {
    almacenamiento.setItem(CLAVE, JSON.stringify(sesion));
  }

  async function pedir(ruta, opciones) {
    const respuesta = await fetchImpl(`${apiBase}${ruta}`, opciones);
    const datos = await respuesta.json().catch(() => ({}));
    return datos && typeof datos === "object" ? datos : {};
  }

  function resultado(datos) {
    return {
      ok: Boolean(datos.ok),
      mensaje: datos.mensaje || (datos.ok ? "" : "No se ha podido completar la operación."),
      usuario: datos.usuario || null,
      token: datos.token || "",
      obsoleta: false,
      red: false,
    };
  }

  return {
    /**
     * --------------------
     * sesionActual() --> UsuarioPublico
     * --------------------
     */
    sesionActual() {
      const sesion = leer();
      return sesion ? { ...sesion.usuario, token: sesion.token } : null;
    },

    /**
     * --------------------
     * nombre: Text, apellidos: Text, email: Text, password: Text --> registrar() --> ResultadoAuth
     * --------------------
     */
    async registrar(nombre, apellidos, email, password) {
      const id = ++generacion;
      let datos;
      try {
        datos = await pedir("/api/registro", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ nombre, apellidos, email, password }),
        });
      } catch {
        return { ok: false, mensaje: "No se ha podido completar el registro.", usuario: null, token: "", obsoleta: id !== generacion, red: true };
      }
      if (id !== generacion) return { ok: false, mensaje: "", usuario: null, token: "", obsoleta: true, red: false };
      return resultado(datos);
    },

    /**
     * --------------------
     * email: Text, password: Text --> iniciarSesion() --> ResultadoAuth
     * --------------------
     */
    async iniciarSesion(email, password) {
      const id = ++generacion;
      let datos;
      try {
        datos = await pedir("/api/login", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email, password }),
        });
      } catch {
        return { ok: false, mensaje: "No se ha podido iniciar sesión.", usuario: null, token: "", obsoleta: id !== generacion, red: true };
      }
      if (id !== generacion) return { ok: false, mensaje: "", usuario: null, token: "", obsoleta: true, red: false };
      const salida = resultado(datos);
      if (salida.ok && salida.token && salida.usuario) {
        guardar({ token: salida.token, usuario: salida.usuario });
      }
      return salida;
    },

    /**
     * --------------------
     * cerrarSesion() --> B
     * --------------------
     * Borra la credencial local antes de avisar al servidor.
     */
    async cerrarSesion() {
      const id = ++generacion;
      const sesion = leer();
      almacenamiento.removeItem(CLAVE);
      if (sesion?.token) {
        try {
          await pedir("/api/logout", {
            method: "POST",
            headers: { Authorization: `Bearer ${sesion.token}` },
          });
        } catch {
          // La credencial local ya no está.
        }
      }
      return id === generacion;
    },

    /**
     * --------------------
     * comprobarSesion() --> ResultadoAuth
     * --------------------
     */
    async comprobarSesion() {
      const id = ++generacion;
      const sesion = leer();
      if (!sesion) {
        return { ok: false, mensaje: "No hay sesión.", usuario: null, token: "", obsoleta: false, red: false };
      }
      let datos;
      try {
        datos = await pedir("/api/sesion", {
          headers: { Authorization: `Bearer ${sesion.token}` },
        });
      } catch {
        return { ok: false, mensaje: "No se pudo comprobar la sesión.", usuario: null, token: "", obsoleta: id !== generacion, red: true };
      }
      if (id !== generacion) return { ok: false, mensaje: "", usuario: null, token: "", obsoleta: true, red: false };
      if (!datos.ok) {
        almacenamiento.removeItem(CLAVE);
        return resultado(datos);
      }
      guardar({ token: sesion.token, usuario: datos.usuario });
      return { ...resultado(datos), token: sesion.token };
    },

    /**
     * --------------------
     * nombre: Text, apellidos: Text, email: Text, password: Text,
     * password_nueva: Text, foto_base64: Text --> actualizarPerfil() --> ResultadoAuth
     * --------------------
     */
    async actualizarPerfil({
      nombre,
      apellidos,
      email,
      password,
      password_nueva = "",
      foto_base64 = "",
      quitar_foto = false,
    }) {
      const id = ++generacion;
      const sesion = leer();
      if (!sesion) {
        return { ok: false, mensaje: "No hay sesión.", usuario: null, token: "", obsoleta: false, red: false };
      }
      let datos;
      try {
        datos = await pedir("/api/perfil", {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${sesion.token}`,
          },
          body: JSON.stringify({
            nombre,
            apellidos,
            email,
            password,
            password_nueva,
            foto_base64,
            quitar_foto: Boolean(quitar_foto),
          }),
        });
      } catch {
        return { ok: false, mensaje: "No se ha podido actualizar el perfil.", usuario: null, token: "", obsoleta: id !== generacion, red: true };
      }
      if (id !== generacion) return { ok: false, mensaje: "", usuario: null, token: "", obsoleta: true, red: false };
      const salida = resultado(datos);
      if (salida.ok && salida.usuario) {
        guardar({ token: sesion.token, usuario: salida.usuario });
      }
      return { ...salida, token: sesion.token };
    },

    /**
     * --------------------
     * password: Text --> borrarCuenta() --> ResultadoAuth
     * --------------------
     */
    /**
     * --------------------
     * listarUsuarios() --> { ok, mensaje, usuarios }
     * --------------------
     */
    async crearUsuarioAdmin({ nombre, apellidos, email, rol }) {
      const sesion = leer();
      if (!sesion) {
        return { ok: false, mensaje: "No hay sesión.", password_temporal: "", usuario: null };
      }
      let datos;
      try {
        datos = await pedir("/api/usuarios", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${sesion.token}`,
          },
          body: JSON.stringify({ nombre, apellidos, email, rol }),
        });
      } catch {
        return { ok: false, mensaje: "No se ha podido crear el usuario.", password_temporal: "", usuario: null };
      }
      return {
        ok: Boolean(datos.ok),
        mensaje: datos.mensaje || "",
        password_temporal: datos.password_temporal || "",
        usuario: datos.usuario || null,
      };
    },

    async listarUsuarios() {
      const sesion = leer();
      if (!sesion) {
        return { ok: false, mensaje: "No hay sesión.", usuarios: [] };
      }
      let datos;
      try {
        datos = await pedir("/api/usuarios", {
          headers: { Authorization: `Bearer ${sesion.token}` },
        });
      } catch {
        return { ok: false, mensaje: "No se ha podido cargar el listado.", usuarios: [] };
      }
      return {
        ok: Boolean(datos.ok),
        mensaje: datos.mensaje || "",
        usuarios: Array.isArray(datos.usuarios) ? datos.usuarios : [],
      };
    },

    /**
     * --------------------
     * id, datos --> actualizarUsuarioAdmin() --> ResultadoAuth
     * --------------------
     */
    async actualizarUsuarioAdmin(idUsuario, { nombre, apellidos, email, activo, rol }) {
      const sesion = leer();
      if (!sesion) {
        return { ok: false, mensaje: "No hay sesión.", usuario: null };
      }
      let datos;
      try {
        datos = await pedir(`/api/usuarios/${idUsuario}`, {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${sesion.token}`,
          },
          body: JSON.stringify({ nombre, apellidos, email, activo, rol }),
        });
      } catch {
        return { ok: false, mensaje: "No se ha podido actualizar el usuario.", usuario: null };
      }
      return {
        ok: Boolean(datos.ok),
        mensaje: datos.mensaje || (datos.ok ? "" : "No se ha podido actualizar el usuario."),
        usuario: datos.usuario || null,
      };
    },

    async borrarUsuarioAdmin(idUsuario) {
      const sesion = leer();
      if (!sesion) {
        return { ok: false, mensaje: "No hay sesión.", usuario: null };
      }
      let datos;
      try {
        datos = await pedir(`/api/usuarios/${idUsuario}`, {
          method: "DELETE",
          headers: { Authorization: `Bearer ${sesion.token}` },
        });
      } catch {
        return { ok: false, mensaje: "No se ha podido borrar el usuario.", usuario: null };
      }
      return {
        ok: Boolean(datos.ok),
        mensaje: datos.mensaje || "",
        usuario: datos.usuario || null,
      };
    },

    async generarPasswordAdmin(idUsuario) {
      const sesion = leer();
      if (!sesion) {
        return { ok: false, mensaje: "No hay sesión.", password_temporal: "", usuario: null };
      }
      let datos;
      try {
        datos = await pedir(`/api/usuarios/${idUsuario}/password`, {
          method: "POST",
          headers: { Authorization: `Bearer ${sesion.token}` },
        });
      } catch {
        return { ok: false, mensaje: "No se ha podido generar la contraseña.", password_temporal: "", usuario: null };
      }
      return {
        ok: Boolean(datos.ok),
        mensaje: datos.mensaje || "",
        password_temporal: datos.password_temporal || "",
        usuario: datos.usuario || null,
      };
    },

    async borrarCuenta(password) {
      const id = ++generacion;
      const sesion = leer();
      if (!sesion) {
        return { ok: false, mensaje: "No hay sesión.", usuario: null, token: "", obsoleta: false, red: false };
      }
      let datos;
      try {
        datos = await pedir("/api/cuenta", {
          method: "DELETE",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${sesion.token}`,
          },
          body: JSON.stringify({ password }),
        });
      } catch {
        return { ok: false, mensaje: "No se ha podido borrar la cuenta.", usuario: null, token: "", obsoleta: id !== generacion, red: true };
      }
      if (id !== generacion) return { ok: false, mensaje: "", usuario: null, token: "", obsoleta: true, red: false };
      const salida = resultado(datos);
      if (salida.ok) {
        almacenamiento.removeItem(CLAVE);
      }
      return salida;
    },

    /**
     * --------------------
     * obtenerPuntosMapa() --> { ok, mensaje, puntos }
     * --------------------
     * Puntos del mapa: exige sesión (Bearer). No lanza: informa con ok=false.
     */
    async obtenerPuntosMapa() {
      const sesion = leer();
      if (!sesion) {
        return { ok: false, mensaje: "No hay sesión.", puntos: [] };
      }
      let datos;
      try {
        datos = await pedir("/api/mapa", {
          headers: { Authorization: `Bearer ${sesion.token}` },
        });
      } catch {
        return { ok: false, mensaje: "No se ha podido cargar el mapa.", puntos: [] };
      }
      return {
        ok: Boolean(datos.ok),
        mensaje: datos.mensaje || "",
        puntos: Array.isArray(datos.puntos) ? datos.puntos : [],
      };
    },

    /**
     * --------------------
     * id_nodo: N --> obtenerDetalleZona() --> { ok, mensaje, zona }
     * --------------------
     * Detalle de una zona (nodo): exige sesión (Bearer).
     */
    async obtenerDetalleZona(idNodo) {
      const sesion = leer();
      if (!sesion) {
        return { ok: false, mensaje: "No hay sesión.", zona: null };
      }
      let datos;
      try {
        datos = await pedir(`/api/mapa/zona/${idNodo}`, {
          headers: { Authorization: `Bearer ${sesion.token}` },
        });
      } catch {
        return { ok: false, mensaje: "No se ha podido cargar la zona.", zona: null };
      }
      return {
        ok: Boolean(datos.ok),
        mensaje: datos.mensaje || "",
        zona: datos.zona || null,
      };
    },
  };
}

export const logicaProxy = crearLogicaProxy({
  fetchImpl: (...args) => globalThis.fetch(...args),
  almacenamiento: globalThis.sessionStorage,
  apiBase: "http://localhost",
});
