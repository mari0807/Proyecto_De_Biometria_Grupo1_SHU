/**
 * Tests críticos de LogicaProxy (Node).
 * Ejecutar: node "Pagina web/js/tests/logicaProxyTest.mjs"
 */
import { crearLogicaProxy } from "../logicaProxy.js";

let fallos = 0;

function afirmar(condicion, mensaje) {
  if (condicion) {
    console.log(`OK  ${mensaje}`);
    return;
  }
  fallos += 1;
  console.error(`FALLO  ${mensaje}`);
}

function memoria() {
  const datos = new Map();
  return {
    getItem(clave) {
      return datos.has(clave) ? datos.get(clave) : null;
    },
    setItem(clave, valor) {
      datos.set(clave, String(valor));
    },
    removeItem(clave) {
      datos.delete(clave);
    },
  };
}

function fetchFalso(respuestas) {
  const llamadas = [];
  const impl = async (url, opciones = {}) => {
    llamadas.push({ url, opciones });
    const clave = `${opciones.method || "GET"} ${url}`;
    const respuesta = respuestas[clave] ?? respuestas["*"] ?? { ok: false, mensaje: "no mock" };
    return {
      async json() {
        return typeof respuesta === "function" ? respuesta(url, opciones) : respuesta;
      },
    };
  };
  impl.llamadas = llamadas;
  return impl;
}

const almacen = memoria();
const fetchImpl = fetchFalso({
  "POST http://localhost/api/login": {
    ok: true,
    mensaje: "Sesión iniciada.",
    token: "tok.abc.def",
    usuario: { id_usuario: 1, nombre: "Ana", apellidos: "Pérez", email: "ana@correo.com", rol: "usuario", foto: "/perfiles/u_1.png" },
  },
  "GET http://localhost/api/sesion": {
    ok: true,
    mensaje: "Sesión válida.",
    usuario: { id_usuario: 1, nombre: "Ana", apellidos: "Pérez", email: "ana@correo.com", rol: "usuario", foto: "/perfiles/u_1.png" },
  },
  "POST http://localhost/api/logout": { ok: true, mensaje: "Sesión cerrada." },
});

const proxy = crearLogicaProxy({
  fetchImpl,
  almacenamiento: almacen,
  apiBase: "http://localhost",
  ahora: () => Date.now(),
});

afirmar(proxy.urlRecurso("/perfiles/u_1.png") === "http://localhost/perfiles/u_1.png", "urlRecurso antepone apiBase");
afirmar(proxy.urlRecurso("http://cdn/x.png") === "http://cdn/x.png", "urlRecurso no altera URLs absolutas");

const login = await proxy.iniciarSesion("ana@correo.com", "Test1234!");
afirmar(login.ok === true && login.token === "tok.abc.def", "iniciarSesion guarda ResultadoAuth ok");
afirmar(proxy.sesionActual()?.email === "ana@correo.com", "sesionActual refleja el login");

const sesion = await proxy.comprobarSesion(login.token);
afirmar(sesion.ok === true && sesion.token === login.token, "comprobarSesion(token) valida con Bearer");
afirmar(
  fetchImpl.llamadas.some((l) => l.url.endsWith("/api/sesion") && l.opciones.headers?.Authorization === "Bearer tok.abc.def"),
  "comprobarSesion envía Authorization con el token recibido"
);

const cerrado = await proxy.cerrarSesion(login.token);
afirmar(cerrado === true, "cerrarSesion(token) devuelve verdadero");
afirmar(proxy.sesionActual() === null, "cerrarSesion borra la credencial local");
afirmar(
  fetchImpl.llamadas.some((l) => l.url.endsWith("/api/logout") && l.opciones.headers?.Authorization === "Bearer tok.abc.def"),
  "cerrarSesion avisa al API con el token"
);

const sinToken = await proxy.comprobarSesion("");
afirmar(sinToken.ok === false && sinToken.mensaje === "No hay sesión.", "comprobarSesion('') sin fetch si no hay token");

if (fallos > 0) {
  console.error(`\n${fallos} fallo(s)`);
  process.exit(1);
}
console.log("\nTodos los tests de LogicaProxy OK");
