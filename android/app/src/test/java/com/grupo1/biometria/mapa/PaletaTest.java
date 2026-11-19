package com.grupo1.biometria.mapa;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PaletaTest {
    @Test
    public void colorDeLaApiPrevaleceSobreElFallback() {
        assertEquals("#E67E22", Paleta.color("mala", "#E67E22"));
    }

    @Test
    public void fallbackUsaLosColoresDeUmbral() {
        assertEquals("#2ECC71", Paleta.color("buena", ""));
        assertEquals("#F1C40F", Paleta.color("moderada", null));
        assertEquals("#E67E22", Paleta.color("mala", null));
        assertEquals("#E74C3C", Paleta.color("muy_mala", null));
        assertEquals("#95A5A6", Paleta.color("sin_datos", null));
    }

    @Test
    public void intensidadesYEtiquetasCoincidenConLaWeb() {
        assertEquals(0.25, Paleta.intensidad("buena"), 0.0001);
        assertEquals(0.5, Paleta.intensidad("moderada"), 0.0001);
        assertEquals(0.8, Paleta.intensidad("mala"), 0.0001);
        assertEquals(1.0, Paleta.intensidad("muy_mala"), 0.0001);
        assertEquals(0.3, Paleta.intensidad("otra"), 0.0001);
        assertEquals("Muy mala", Paleta.etiqueta("muy_mala"));
        assertEquals("Sin datos", Paleta.etiqueta(null));
    }
}
