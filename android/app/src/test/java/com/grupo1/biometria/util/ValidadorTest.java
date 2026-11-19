package com.grupo1.biometria.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests de validación de campos. Reproducen las reglas del backend
 * (`Autenticacion.php` / `password.js`) para que la app falle igual que la web.
 */
public class ValidadorTest {

    // ---------- estaVacio ----------

    @Test
    public void null_o_blanco_es_vacio() {
        assertTrue(Validador.estaVacio(null));
        assertTrue(Validador.estaVacio(""));
        assertTrue(Validador.estaVacio("   "));
    }

    @Test
    public void con_contenido_no_es_vacio() {
        assertFalse(Validador.estaVacio("a"));
        assertFalse(Validador.estaVacio(" usuario "));
    }

    // ---------- emailValido ----------

    @Test
    public void correos_validos() {
        assertTrue(Validador.emailValido("usuario@ejemplo.com"));
        assertTrue(Validador.emailValido("nombre.apellido+tag@dominio.universidad.es"));
        assertTrue(Validador.emailValido("  con_espacios@ejemplo.es  "));
    }

    @Test
    public void correos_invalidos() {
        assertFalse(Validador.emailValido(null));
        assertFalse(Validador.emailValido(""));
        assertFalse(Validador.emailValido("sin-arroba.com"));
        assertFalse(Validador.emailValido("sin@dominio"));
        assertFalse(Validador.emailValido("@sinlocal.com"));
        assertFalse(Validador.emailValido("dos@@ejemplo.com"));
        assertFalse(Validador.emailValido("con espacios@ejemplo.com"));
    }

    @Test
    public void correo_mas_largo_de_254_no_es_valido() {
        String largo = "a".repeat(250) + "@ejemplo.com";
        assertFalse(Validador.emailValido(largo));
    }

    // ---------- errorPassword (política del backend) ----------

    @Test
    public void password_cumplida_no_tiene_error() {
        assertNull(Validador.errorPassword("Abcdef1!"));
        assertTrue(Validador.passwordValida("Abcdef1!"));
    }

    @Test
    public void password_vacia_es_obligatoria() {
        assertEquals("La contraseña es obligatoria.", Validador.errorPassword(""));
        assertEquals("La contraseña es obligatoria.", Validador.errorPassword(null));
    }

    @Test
    public void password_corta_de_8_caracteres() {
        assertNotNull(Validador.errorPassword("Abc1!"));
        assertEquals("La contraseña no cumple: al menos 8 caracteres.",
                Validador.errorPassword("Abc1!"));
    }

    @Test
    public void password_sin_mayuscula() {
        assertEquals("La contraseña no cumple: al menos una mayúscula (a-z).",
                Validador.errorPassword("abcdef1!"));
    }

    @Test
    public void password_sin_minuscula() {
        assertEquals("La contraseña no cumple: al menos una minúscula (a-z).",
                Validador.errorPassword("ABCDEF1!"));
    }

    @Test
    public void password_sin_numero() {
        assertEquals("La contraseña no cumple: al menos un número (0-9).",
                Validador.errorPassword("Abcdefg!"));
    }

    @Test
    public void password_sin_signo_especial() {
        assertEquals("La contraseña no cumple: al menos un signo especial (!@#$…).",
                Validador.errorPassword("Abcdefg1"));
    }

    @Test
    public void password_de_mas_de_72_caracteres() {
        String larga = "Aa1!" + "a".repeat(72);
        assertEquals("La contraseña no puede superar 72 caracteres.",
                Validador.errorPassword(larga));
    }

    // ---------- requisitosPassword (marcadores en vivo del registro) ----------

    @Test
    public void requisitos_todos_cumplidos_con_password_valida() {
        boolean[] estados = Validador.requisitosPassword("Abcdef1!");
        assertEquals(5, estados.length);
        for (boolean estado : estados) {
            assertTrue(estado);
        }
    }

    @Test
    public void requisitos_vacios_no_cumplen_ninguno() {
        boolean[] estados = Validador.requisitosPassword("");
        for (boolean estado : estados) {
            assertFalse(estado);
        }
        assertFalse(Validador.requisitosPassword(null)[0]);
    }

    @Test
    public void requisitos_se_marcan_individualmente() {
        boolean[] sinMayuscula = Validador.requisitosPassword("abcdef1!");
        assertFalse(sinMayuscula[1]);
        assertTrue(sinMayuscula[2]);
        assertTrue(sinMayuscula[3]);
        assertTrue(sinMayuscula[4]);

        boolean[] sinSigno = Validador.requisitosPassword("Abcdefg1");
        assertTrue(sinSigno[0]);
        assertTrue(sinSigno[1]);
        assertTrue(sinSigno[2]);
        assertTrue(sinSigno[3]);
        assertFalse(sinSigno[4]);

        boolean[] corta = Validador.requisitosPassword("Ab1!");
        assertFalse(corta[0]);
        assertTrue(corta[1]);
        assertTrue(corta[2]);
        assertTrue(corta[3]);
        assertTrue(corta[4]);
    }
}
