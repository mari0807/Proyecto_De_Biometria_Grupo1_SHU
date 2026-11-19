import { logicaProxy } from "./logicaProxy.js";

const CENTRO_GANDIA = [38.9748, -0.1745];
const ZOOM_INICIAL = 13;

const COLOR_POR_DEFECTO = {
  buena: "#70b83e",
  moderada: "#edae35",
  mala: "#e26571",
  muy_mala: "#c0392b",
  sin_datos: "#95A5A6",
};

const ETIQUETA_CATEGORIA = {
  buena: "Buena",
  moderada: "Moderada",
  mala: "Mala",
  muy_mala: "Muy mala",
  sin_datos: "Sin datos",
};

const INTENSIDAD = { buena: 0.25, moderada: 0.5, mala: 0.8, muy_mala: 1 };

const contenedor = document.querySelector("#mapa-canvas");
const estado = document.querySelector("#mapa-estado");
const actualizacion = document.querySelector("#mapa-actualizacion");
const panel = document.querySelector("#zona-detalle");

const mapa =
  contenedor && globalThis.L
    ? globalThis.L.map(contenedor, { scrollWheelZoom: true }).setView(CENTRO_GANDIA, ZOOM_INICIAL)
    : null;

let capaCalor = null;
let capaMarcadores = null;

if (mapa) {
  globalThis.L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "&copy; OpenStreetMap",
  }).addTo(mapa);
}

function escapar(texto) {
  return String(texto ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function mostrarEstado(texto) {
  if (!estado) return;
  estado.hidden = !texto;
  estado.textContent = texto || "";
}

function formatearFecha(fechaHora) {
  const texto = String(fechaHora ?? "");
  const coincide = texto.match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})/);
  if (!coincide) return texto || "sin fecha";
  const [, anio, mes, dia, hora, minuto] = coincide;
  return `${dia}/${mes}/${anio} ${hora}:${minuto} UTC`;
}

function colorCategoria(puntos) {
  const colores = { ...COLOR_POR_DEFECTO };
  for (const punto of puntos) {
    if (punto.categoria && punto.color) colores[punto.categoria] = punto.color;
  }
  return colores;
}

function pintarLeyenda(puntos) {
  if (!mapa) return;
  const colores = colorCategoria(puntos);
  const leyenda = globalThis.L.control({ position: "bottomleft" });
  leyenda.onAdd = () => {
    const div = globalThis.L.DomUtil.create("div", "map-legend");
    div.setAttribute("aria-label", "Leyenda de calidad del aire");
    div.innerHTML = Object.keys(ETIQUETA_CATEGORIA)
      .map(
        (categoria) =>
          `<span><i style="background:${colores[categoria] || COLOR_POR_DEFECTO.sin_datos}"></i>${ETIQUETA_CATEGORIA[categoria]}</span>`
      )
      .join("");
    globalThis.L.DomEvent.disableClickPropagation(div);
    return div;
  };
  leyenda.addTo(mapa);
}

function pintarPuntos(puntos) {
  if (!mapa) return;
  if (capaCalor) mapa.removeLayer(capaCalor);
  if (capaMarcadores) mapa.removeLayer(capaMarcadores);

  if (puntos.length === 0) return;

  const calor = puntos.map((p) => [
    Number(p.latitud),
    Number(p.longitud),
    INTENSIDAD[p.categoria] ?? 0.3,
  ]);
  capaCalor = globalThis.L.heatLayer(calor, { radius: 38, blur: 26, maxZoom: 15 }).addTo(mapa);

  capaMarcadores = globalThis.L.layerGroup().addTo(mapa);
  for (const punto of puntos) {
    const marcador = globalThis.L.circleMarker([Number(punto.latitud), Number(punto.longitud)], {
      radius: 8,
      color: "#ffffff",
      weight: 3,
      fillColor: punto.color || COLOR_POR_DEFECTO.sin_datos,
      fillOpacity: 1,
    });
    marcador.bindTooltip(
      `<b>${escapar(punto.nombre)}</b><br>${escapar(punto.contaminante)}: ${escapar(punto.valor)} ${escapar(punto.unidad)}`
    );
    marcador.on("click", () => cargarZona(punto.id_nodo));
    marcador.addTo(capaMarcadores);
  }
}

function graficaEvolucion(historial, unidad) {
  const valores = historial.map((p) => Number(p.valor));
  const maximo = Math.max(...valores);
  const minimo = Math.min(...valores);
  const rango = maximo - minimo || 1;
  const ancho = 360;
  const alto = 100;
  const paso = historial.length > 1 ? ancho / (historial.length - 1) : 0;
  const puntos = historial
    .map((p, i) => {
      const x = Math.round(i * paso);
      const y = Math.round(alto - 8 - ((Number(p.valor) - minimo) / rango) * (alto - 20));
      return `${x} ${y}`;
    })
    .join(" ");

  return `
    <div class="trend">
      Evolución reciente
      <small>${escapar(unidad)}</small>
    </div>
    <div class="chart" role="img" aria-label="Gráfica de evolución de ${escapar(unidad)}">
      <svg viewBox="0 0 ${ancho} ${alto}" preserveAspectRatio="none">
        <polygon points="0 ${alto} ${puntos} ${ancho} ${alto}" fill="#eaf4f8"></polygon>
        <polyline points="${puntos}" fill="none" stroke="#198db1" stroke-width="3"></polyline>
      </svg>
      <div>
        <span>${formatearFecha(historial[0].fecha_hora)}</span>
        <span>${formatearFecha(historial[historial.length - 1].fecha_hora)}</span>
      </div>
    </div>`;
}

function pintarZona(zona) {
  if (!panel) return;
  if (!zona) {
    panel.innerHTML = `
      <small>DETALLE DE LA ZONA</small>
      <h2>Selecciona una zona</h2>
      <p class="zone-hint">Pulsa un punto del mapa para ver sus valores y su evolución.</p>`;
    return;
  }

  const actual = zona.actual;
  const color = actual?.color || COLOR_POR_DEFECTO.sin_datos;
  const categoria = ETIQUETA_CATEGORIA[actual?.categoria] || "Sin datos";
  const ubicacion =
    zona.latitud !== null && zona.longitud !== null
      ? `${Number(zona.latitud).toFixed(5)}, ${Number(zona.longitud).toFixed(5)}`
      : "ubicación no disponible";

  const bloqueActual = actual
    ? `<div class="quality" style="border-color:${color}55;background:${color}14">
         <b style="color:${color}">●</b>
         <span>
           <strong>${escapar(categoria)}</strong>
           <small>${escapar(zona.contaminante)} · ${escapar(ubicacion)}</small>
         </span>
       </div>
       <p class="label">Contaminante principal</p>
       <div class="reading">
         <b>${escapar(zona.contaminante)}</b>
         <strong>${escapar(actual.valor)}<small>${escapar(zona.unidad)}</small></strong>
       </div>
       <p class="zone-hint">Medición: ${formatearFecha(actual.fecha_hora)}</p>`
    : `<p class="zone-hint">No hay mediciones disponibles para esta zona.</p>`;

  const bloqueGrafica =
    actual && zona.historial.length >= 2 ? graficaEvolucion(zona.historial, zona.unidad) : "";

  panel.innerHTML = `
    <small>${escapar(zona.nombre)}</small>
    <h2>${escapar(zona.contaminante)} · ${escapar(ubicacion)}</h2>
    ${bloqueActual}
    ${bloqueGrafica}`;
}

async function cargarZona(idNodo) {
  const resultado = await logicaProxy.obtenerDetalleZona(idNodo);
  if (!resultado.ok) {
    mostrarEstado(resultado.mensaje || "No se ha podido cargar la zona.");
    return;
  }
  pintarZona(resultado.zona);
}

async function iniciar() {
  if (!mapa) {
    mostrarEstado("No se ha podido cargar el mapa interactivo.");
    return;
  }

  const resultado = await logicaProxy.obtenerPuntosMapa();
  if (!resultado.ok) {
    mostrarEstado(resultado.mensaje || "Inicia sesión para ver el mapa en directo.");
    if (actualizacion) actualizacion.textContent = "● Sin datos";
    return;
  }

  pintarLeyenda(resultado.puntos);
  pintarPuntos(resultado.puntos);

  if (resultado.puntos.length === 0) {
    mostrarEstado("No hay datos disponibles en este momento.");
    if (actualizacion) actualizacion.textContent = "● Sin datos";
    return;
  }

  const ultima = resultado.puntos
    .map((p) => p.fecha_hora)
    .sort()
    .pop();
  if (actualizacion) actualizacion.textContent = `● Última actualización: ${formatearFecha(ultima)}`;
}

iniciar();
