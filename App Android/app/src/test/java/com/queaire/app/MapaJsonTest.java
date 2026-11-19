package com.queaire.app;

import com.queaire.app.model.PuntoMapa;
import com.queaire.app.model.ZonaDetalle;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MapaJsonTest {
    @Test
    public void parseaPuntosConColorDeLaApi() throws Exception {
        String json = "{\"ok\":true,\"puntos\":[{"
                + "\"id_nodo\":2,\"nombre\":\"Port de Gandia\","
                + "\"latitud\":38.9748,\"longitud\":-0.1655,"
                + "\"contaminante\":\"NO2\",\"unidad\":\"ppb\",\"valor\":180,"
                + "\"categoria\":\"mala\",\"color\":\"#E67E22\","
                + "\"fecha_hora\":\"2026-11-19 16:00:00\"}]}";

        List<PuntoMapa> puntos = MapaJson.puntos(json);
        assertEquals(1, puntos.size());
        assertEquals(2, puntos.get(0).getIdNodo());
        assertEquals("Port de Gandia", puntos.get(0).getNombre());
        assertEquals(38.9748, puntos.get(0).getLatitud(), 0.00001);
        assertEquals("#E67E22", puntos.get(0).getColor());
        assertTrue(MapaJson.ok(json));
    }

    @Test
    public void aplicaFallbackCuandoNoHayColor() throws Exception {
        String json = "{\"ok\":true,\"puntos\":[{"
                + "\"id_nodo\":1,\"nombre\":\"Campus UPV\",\"latitud\":38.99646,"
                + "\"longitud\":-0.16603,\"contaminante\":\"CO2\",\"unidad\":\"ppm\","
                + "\"valor\":610,\"categoria\":\"buena\",\"color\":\"\","
                + "\"fecha_hora\":\"2026-11-19 16:00:00\"}]}";

        assertEquals("#2ECC71", MapaJson.puntos(json).get(0).getColor());
    }

    @Test
    public void parseaZonaSinMediciones() throws Exception {
        String json = "{\"ok\":true,\"zona\":{\"id_nodo\":3,\"nombre\":\"Av. del Port\","
                + "\"estado\":\"activo\",\"contaminante\":\"PM2.5\",\"unidad\":\"µg/m³\","
                + "\"latitud\":38.9908,\"longitud\":-0.158,\"actual\":null,\"historial\":[]}}";

        ZonaDetalle zona = MapaJson.zona(json);
        assertEquals(3, zona.getIdNodo());
        assertNull(zona.getActual());
        assertTrue(zona.getHistorial().isEmpty());
    }

    @Test
    public void parseaHistorialYRespetaOkFalse() throws Exception {
        String json = "{\"ok\":true,\"zona\":{\"id_nodo\":1,\"nombre\":\"Campus UPV\","
                + "\"estado\":\"activo\",\"contaminante\":\"CO2\",\"unidad\":\"ppm\","
                + "\"latitud\":38.99646,\"longitud\":-0.16603,"
                + "\"actual\":{\"valor\":610,\"categoria\":\"buena\",\"color\":\"#2ECC71\","
                + "\"fecha_hora\":\"2026-11-19 16:00:00\"},"
                + "\"historial\":[{\"valor\":610,\"categoria\":\"buena\",\"color\":\"#2ECC71\","
                + "\"fecha_hora\":\"2026-11-19 16:00:00\"},"
                + "{\"valor\":1650,\"categoria\":\"mala\",\"color\":\"#E67E22\","
                + "\"fecha_hora\":\"2026-11-19 14:00:00\"}]}}";

        ZonaDetalle zona = MapaJson.zona(json);
        assertEquals(2, zona.getHistorial().size());
        assertEquals("#E67E22", zona.getHistorial().get(1).getColor());
        assertEquals("La sesión no es válida.",
                MapaJson.mensaje("{\"ok\":false,\"mensaje\":\"La sesión no es válida.\"}", "error"));
        assertFalse(MapaJson.ok("{\"ok\":false}"));
    }
}
