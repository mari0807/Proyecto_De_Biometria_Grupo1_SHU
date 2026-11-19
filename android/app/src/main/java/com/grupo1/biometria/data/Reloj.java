package com.grupo1.biometria.data;

/**
 * Reloj inyectable para poder probar la caducidad de los tokens en los tests
 * sin esperar a que pase el tiempo real.
 */
public interface Reloj {

    /**
     * --------------------
     * --> ahora_ms() --> instante: N
     * --------------------
     */
    long ahoraMs();
}
