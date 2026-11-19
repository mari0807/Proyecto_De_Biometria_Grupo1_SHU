package com.queaire.app;

import com.queaire.app.model.PuntoMapa;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class FormatoTest {
    @Test
    public void fechaSeMuestraEnUtcComoEnLaWeb() {
        assertEquals("19/11/2026 16:00 UTC", Formato.fecha("2026-11-19 16:00:00"));
        assertEquals("19/11/2026 16:00 UTC", Formato.fecha("2026-11-19T16:00:00"));
        assertEquals("19/11 16:00", Formato.fechaCorta("2026-11-19 16:00:00"));
    }

    @Test
    public void valorRespetaDecimales() {
        assertEquals("610", Formato.valor(610.0));
        assertEquals("31.50", Formato.valor(31.5));
    }

    @Test
    public void maximaFechaSeleccionaLaUltimaMedicion() {
        PuntoMapa viejo = punto(1, "2026-11-19 14:00:00");
        PuntoMapa reciente = punto(2, "2026-11-19 16:00:00");
        assertEquals("2026-11-19 16:00:00", Formato.maximaFecha(Arrays.asList(viejo, reciente)));
    }

    private static PuntoMapa punto(int id, String fecha) {
        return new PuntoMapa(id, "Nodo", 38.9, -0.1, "CO2", "ppm", 610, "buena", "", fecha);
    }
}
