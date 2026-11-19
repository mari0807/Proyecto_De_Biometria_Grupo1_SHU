package com.grupo1.biometria.data;

/** Respuesta HTTP cruda: código de estado y cuerpo (JSON o vacío). */
public final class RespuestaHttp {

    public final int estado;
    public final String cuerpo;

    public RespuestaHttp(int estado, String cuerpo) {
        this.estado = estado;
        this.cuerpo = cuerpo == null ? "" : cuerpo;
    }
}
