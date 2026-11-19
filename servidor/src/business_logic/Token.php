<?php

declare(strict_types=1);

namespace QueAire\BusinessLogic;

final class Token
{
    /**
     * --------------------
     * id_usuario: N, rol: Text, secreto: Text, ahora: N, ttl: N --> emitir() --> Text
     * --------------------
     */
    public function emitir(int $idUsuario, string $rol, string $secreto, int $ahora, int $ttl): string
    {
        $cabecera = $this->codificar((string) json_encode(['alg' => 'HS256', 'typ' => 'JWT']));
        $cuerpo = $this->codificar((string) json_encode([
            'id_usuario' => $idUsuario,
            'rol' => $rol,
            'exp' => $ahora + $ttl,
        ]));
        $firma = $this->codificar(hash_hmac('sha256', $cabecera . '.' . $cuerpo, $secreto, true));

        return $cabecera . '.' . $cuerpo . '.' . $firma;
    }

    /**
     * --------------------
     * token: Text, secreto: Text, ahora: N --> leer() --> (id_usuario: N, rol: Text)
     * --------------------
     * Devuelve null si la firma no coincide o el token ha caducado.
     *
     * @return array{id_usuario: int, rol: string}|null
     */
    public function leer(string $token, string $secreto, int $ahora): ?array
    {
        $partes = explode('.', $token);
        if (count($partes) !== 3) {
            return null;
        }

        [$cabecera, $cuerpo, $firma] = $partes;
        $esperada = $this->codificar(hash_hmac('sha256', $cabecera . '.' . $cuerpo, $secreto, true));
        if (!hash_equals($esperada, $firma)) {
            return null;
        }

        $datos = json_decode($this->decodificar($cuerpo), true);
        if (!is_array($datos) || !isset($datos['id_usuario'], $datos['rol'], $datos['exp'])) {
            return null;
        }
        if ((int) $datos['exp'] <= $ahora) {
            return null;
        }

        return [
            'id_usuario' => (int) $datos['id_usuario'],
            'rol' => (string) $datos['rol'],
        ];
    }

    private function codificar(string $binario): string
    {
        return rtrim(strtr(base64_encode($binario), '+/', '-_'), '=');
    }

    private function decodificar(string $texto): string
    {
        $texto = strtr($texto, '-_', '+/');
        $resto = strlen($texto) % 4;
        if ($resto !== 0) {
            $texto .= str_repeat('=', 4 - $resto);
        }
        $binario = base64_decode($texto, true);

        return $binario === false ? '' : $binario;
    }
}
