<?php

declare(strict_types=1);

namespace QueAire\BusinessLogic;

final class ResultadoAuth
{
    /**
     * @param array{id_usuario: int, nombre: string, apellidos: string, email: string, rol: string, foto: string} $usuario
     */
    private function __construct(
        public bool $ok,
        public string $codigo,
        public string $mensaje,
        public array $usuario,
        public string $token
    ) {
    }

    public static function fallo(string $codigo, string $mensaje): self
    {
        return new self(false, $codigo, $mensaje, self::usuarioVacio(), '');
    }

    /**
     * @param array{id_usuario: int, nombre: string, apellidos: string, email: string, rol: string, foto?: string} $usuario
     */
    public static function acierto(string $mensaje, array $usuario, string $token = ''): self
    {
        if (!isset($usuario['foto'])) {
            $usuario['foto'] = '';
        }

        return new self(true, 'ok', $mensaje, $usuario, $token);
    }

    /**
     * @return array{id_usuario: int, nombre: string, apellidos: string, email: string, rol: string, foto: string}
     */
    public static function usuarioVacio(): array
    {
        return [
            'id_usuario' => 0,
            'nombre' => '',
            'apellidos' => '',
            'email' => '',
            'rol' => '',
            'foto' => '',
        ];
    }

    /**
     * @return array{ok: bool, mensaje: string, usuario: array{id_usuario: int, nombre: string, apellidos: string, email: string, rol: string, foto: string}, token: string}
     */
    public function aJson(): array
    {
        return [
            'ok' => $this->ok,
            'mensaje' => $this->mensaje,
            'usuario' => $this->usuario,
            'token' => $this->token,
        ];
    }
}
