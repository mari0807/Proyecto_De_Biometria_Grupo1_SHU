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

const SEVERIDAD = { buena: 0, moderada: 0.5, mala: 0.75, muy_mala: 1 };

const ESCALA_CALOR = [
  { s: 0, rgb: [112, 184, 62] },
  { s: 0.5, rgb: [237, 174, 53] },
  { s: 0.75, rgb: [230, 126, 34] },
  { s: 1, rgb: [192, 57, 43] },
];

function colorSeveridad(severidad) {
  const valor = Math.max(0, Math.min(1, severidad));
  for (let i = 1; i < ESCALA_CALOR.length; i += 1) {
    const desde = ESCALA_CALOR[i - 1];
    const hasta = ESCALA_CALOR[i];
    if (valor <= hasta.s) {
      const t = (valor - desde.s) / (hasta.s - desde.s || 1);
      return desde.rgb.map((c, k) => Math.round(c + (hasta.rgb[k] - c) * t));
    }
  }
  return ESCALA_CALOR[ESCALA_CALOR.length - 1].rgb;
}

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

const CapaCalor = globalThis.L
  ? globalThis.L.Layer.extend({
      options: { radio: 60, paso: 4, potencia: 2, opacidad: 0.85 },

      initialize(opciones) {
        globalThis.L.setOptions(this, opciones);
        this._puntos = [];
      },

      onAdd(mapa) {
        this._mapa = mapa;
        if (!mapa.getPane("calor")) {
          const pane = mapa.createPane("calor");
          pane.style.zIndex = 350;
          pane.style.pointerEvents = "none";
        }
        this._canvas = globalThis.L.DomUtil.create("canvas", "mapa-calor");
        this._ctx = this._canvas.getContext("2d");
        mapa.getPane("calor").appendChild(this._canvas);
        mapa.on("moveend zoomend resize", this.redibujar, this);
        this.redibujar();
      },

      onRemove(mapa) {
        mapa.off("moveend zoomend resize", this.redibujar, this);
        if (this._canvas) globalThis.L.DomUtil.remove(this._canvas);
      },

      setDatos(puntos) {
        this._puntos = Array.isArray(puntos) ? puntos : [];
        this.redibujar();
      },

      redibujar() {
        const mapa = this._mapa;
        if (!mapa || !this._canvas) return;

        const tam = mapa.getSize();
        const canvas = this._canvas;
        if (canvas.width !== tam.x || canvas.height !== tam.y) {
          canvas.width = tam.x;
          canvas.height = tam.y;
        }
        globalThis.L.DomUtil.setPosition(canvas, mapa.containerPointToLayerPoint([0, 0]));

        const ctx = this._ctx;
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        if (this._puntos.length === 0) return;

        const proyeccion = this._puntos.map((p) => {
          const pt = mapa.latLngToContainerPoint([Number(p.latitud), Number(p.longitud)]);
          return { x: pt.x, y: pt.y, s: SEVERIDAD[p.categoria] ?? 0.5 };
        });

        const { radio, paso, potencia, opacidad } = this.options;
        for (let y = 0; y < canvas.height; y += paso) {
          for (let x = 0; x < canvas.width; x += paso) {
            let sumaPeso = 0;
            let sumaSev = 0;
            for (const q of proyeccion) {
              const distancia = Math.hypot(x - q.x, y - q.y);
              if (distancia >= radio) continue;
              const peso = (1 - distancia / radio) ** potencia;
              sumaPeso += peso;
              sumaSev += peso * q.s;
            }
            if (sumaPeso <= 0) continue;
            const [r, g, b] = colorSeveridad(sumaSev / sumaPeso);
            const alfa = Math.min(1, sumaPeso) * opacidad;
            ctx.fillStyle = `rgba(${r},${g},${b},${alfa.toFixed(3)})`;
            ctx.fillRect(x - paso / 2, y - paso / 2, paso + 1, paso + 1);
          }
        }
      },
    })
  : null;

function pintarPuntos(puntos) {
  if (!mapa) return;
  if (capaCalor) mapa.removeLayer(capaCalor);
  if (capaMarcadores) mapa.removeLayer(capaMarcadores);

  if (puntos.length === 0) return;

  capaCalor = new CapaCalor();
  capaCalor.addTo(mapa);
  capaCalor.setDatos(puntos);

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
