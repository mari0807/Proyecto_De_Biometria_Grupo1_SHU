import assert from "node:assert/strict";
import test from "node:test";
import { crearLogicaProxy } from "./logicaProxy.js";

function almacenamiento() {
  const mapa = new Map();
  return {
    getItem: (clave) => (mapa.has(clave) ? mapa.get(clave) : null),
    setItem: (clave, valor) => mapa.set(clave, String(valor)),
    removeItem: (clave) => mapa.delete(clave),
  };
}

function jsonRespuesta(datos) {
  return {
    json: async () => datos,
  };
}

test("registrar envía el alta y no guarda sesión", async () => {
  const llamadas = [];
  const proxy = crearLogicaProxy({
    almacenamiento: almacenamiento(),
    apiBase: "http://api",
    fetchImpl: async (url, opciones) => {
      llamadas.push({ url, opciones });
      return jsonRespuesta({
        ok: true,
        mensaje: "Cuenta creada correctamente.",
        usuario: { id_usuario: 4, nombre: "Ana", apellidos: "Pérez", email: "ana@correo.com", rol: "usuario" },
      });
    },
  });

  const resultado = await proxy.registrar("Ana", "Pérez", "ana@correo.com", "1234");
  assert.equal(resultado.ok, true);
  assert.equal(resultado.mensaje, "Cuenta creada correctamente.");
  assert.equal(llamadas[0].url, "http://api/api/registro");
  assert.deepEqual(JSON.parse(llamadas[0].opciones.body), {
    nombre: "Ana",
    apellidos: "Pérez",
    email: "ana@correo.com",
    password: "1234",
  });
  assert.equal(proxy.sesionActual(), null);
});

test("iniciarSesion guarda la credencial y una respuesta tardía no la pisa", async () => {
  let resolverPrimera;
  let primera = true;
  const proxy = crearLogicaProxy({
    almacenamiento: almacenamiento(),
    apiBase: "http://api",
    fetchImpl: (url) => {
      if (primera) {
        primera = false;
        return new Promise((resolve) => {
          resolverPrimera = () => resolve(jsonRespuesta({
            ok: true,
            token: "token-viejo",
            usuario: { nombre: "Viejo", apellidos: "Uno", email: "v@correo.com", rol: "usuario", id_usuario: 1 },
          }));
        });
      }
      return Promise.resolve(jsonRespuesta({
        ok: true,
        token: "token-nuevo",
        usuario: { nombre: "Nuevo", apellidos: "Dos", email: "n@correo.com", rol: "admin", id_usuario: 2 },
      }));
    },
  });

  const lenta = proxy.iniciarSesion("v@correo.com", "1234");
  const rapida = await proxy.iniciarSesion("n@correo.com", "1234");
  resolverPrimera();
  const vieja = await lenta;

  assert.equal(vieja.obsoleta, true);
  assert.equal(rapida.usuario.rol, "admin");
  assert.equal(proxy.sesionActual().token, "token-nuevo");
});

test("cerrarSesion borra la credencial aunque el servidor falle", async () => {
  let avisos = 0;
  const memoria = almacenamiento();
  const proxy = crearLogicaProxy({
    almacenamiento: memoria,
    apiBase: "http://api",
    fetchImpl: async (url) => {
      if (url.endsWith("/api/login")) {
        return jsonRespuesta({
          ok: true,
          token: "token-vivo",
          usuario: { nombre: "Ana", apellidos: "Pérez", email: "ana@correo.com", rol: "usuario", id_usuario: 3 },
        });
      }
      avisos += 1;
      throw new Error("red");
    },
  });

  await proxy.iniciarSesion("ana@correo.com", "1234");
  const cerrada = await proxy.cerrarSesion();
  assert.equal(cerrada, true);
  assert.equal(proxy.sesionActual(), null);
  assert.equal(avisos, 1);
  assert.equal(memoria.getItem("queaire_sesion"), null);
});

test("actualizarPerfil guarda el usuario nuevo en la sesión", async () => {
  const memoria = almacenamiento();
  const proxy = crearLogicaProxy({
    almacenamiento: memoria,
    apiBase: "http://api",
    fetchImpl: async (url, opciones) => {
      if (url.endsWith("/api/login")) {
        return jsonRespuesta({
          ok: true,
          token: "token-vivo",
          usuario: { nombre: "Ana", apellidos: "Pérez", email: "ana@correo.com", rol: "cliente", id_usuario: 3, foto: "" },
        });
      }
      assert.equal(url, "http://api/api/perfil");
      assert.equal(opciones.method, "PUT");
      return jsonRespuesta({
        ok: true,
        mensaje: "Perfil actualizado correctamente.",
        usuario: { nombre: "Anita", apellidos: "Pérez", email: "ana@correo.com", rol: "cliente", id_usuario: 3, foto: "/perfiles/u_3.png" },
      });
    },
  });

  await proxy.iniciarSesion("ana@correo.com", "1234");
  const resultado = await proxy.actualizarPerfil({
    nombre: "Anita",
    apellidos: "Pérez",
    email: "ana@correo.com",
    password: "1234",
    foto_base64: "data:image/png;base64,aaa",
  });
  assert.equal(resultado.ok, true);
  assert.equal(proxy.sesionActual().nombre, "Anita");
  assert.equal(proxy.sesionActual().foto, "/perfiles/u_3.png");
});

test("comprobarSesion inválida elimina la credencial", async () => {
  const proxy = crearLogicaProxy({
    almacenamiento: almacenamiento(),
    apiBase: "http://api",
    fetchImpl: async (url) => {
      if (url.endsWith("/api/login")) {
        return jsonRespuesta({
          ok: true,
          token: "token-vivo",
          usuario: { nombre: "Ana", apellidos: "Pérez", email: "ana@correo.com", rol: "usuario", id_usuario: 3 },
        });
      }
      return jsonRespuesta({ ok: false, mensaje: "La sesión no es válida." });
    },
  });

  await proxy.iniciarSesion("ana@correo.com", "1234");
  const comprobacion = await proxy.comprobarSesion();
  assert.equal(comprobacion.ok, false);
  assert.equal(proxy.sesionActual(), null);
});
