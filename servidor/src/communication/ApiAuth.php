<?php

declare(strict_types=1);

namespace QueAire\Communication;

use QueAire\BusinessLogic\Autenticacion;

final class ApiAuth
{
    public function __construct(private Autenticacion $autenticacion)
    {
    }

    /**
     * @param array<string, mixed>|null $cuerpo
     * @return array{0: int, 1: array<string, mixed>|null}
     */
    public function manejar(string $metodo, string $ruta, ?array $cuerpo, ?string $autorizacion): array
    {
        $ruta = rtrim($ruta, '/') ?: '/';
        if ($metodo === 'OPTIONS') {
            return [204, null];
        }

        if ($metodo === 'POST' && $ruta === '/api/registro') {
            return $this->registro($cuerpo ?? []);
        }
        if ($metodo === 'POST' && $ruta === '/api/login') {
            return $this->login($cuerpo ?? []);
        }
        if ($metodo === 'POST' && $ruta === '/api/logout') {
            $this->autenticacion->cerrarSesion($this->token($autorizacion));

            return [200, ['ok' => true, 'mensaje' => 'Sesión cerrada.']];
        }
        if ($metodo === 'GET' && $ruta === '/api/sesion') {
            return $this->sesion($autorizacion);
        }
        if ($metodo === 'PUT' && $ruta === '/api/perfil') {
            return $this->perfil($cuerpo ?? [], $autorizacion);
        }
        if ($metodo === 'DELETE' && $ruta === '/api/cuenta') {
            return $this->borrarCuenta($cuerpo ?? [], $autorizacion);
        }
        if ($metodo === 'GET' && $ruta === '/api/usuarios') {
            return $this->listarUsuarios($autorizacion);
        }
        if ($metodo === 'POST' && $ruta === '/api/usuarios') {
            return $this->crearUsuarioAdmin($cuerpo ?? [], $autorizacion);
        }
        if ($metodo === 'PUT' && preg_match('#^/api/usuarios/(\d+)$#', $ruta, $m) === 1) {
            return $this->actualizarUsuarioAdmin((int) $m[1], $cuerpo ?? [], $autorizacion);
        }
        if ($metodo === 'DELETE' && preg_match('#^/api/usuarios/(\d+)$#', $ruta, $m) === 1) {
            return $this->borrarUsuarioAdmin((int) $m[1], $autorizacion);
        }
        if ($metodo === 'POST' && preg_match('#^/api/usuarios/(\d+)/password$#', $ruta, $m) === 1) {
            return $this->generarPasswordAdmin((int) $m[1], $autorizacion);
        }

        return [404, ['ok' => false, 'mensaje' => 'Ruta no encontrada.']];
    }

    /**
     * @param array<string, mixed> $cuerpo
     * @return array{0: int, 1: array<string, mixed>}
     */
    private function registro(array $cuerpo): array
    {
        $resultado = $this->autenticacion->registrar(
            $this->texto($cuerpo, 'nombre'),
            $this->texto($cuerpo, 'apellidos'),
            $this->texto($cuerpo, 'email'),
            $this->texto($cuerpo, 'password')
        );
        $json = $resultado->aJson();
        unset($json['token']);

        return [$this->estado($resultado->codigo), $json];
    }

    /**
     * @param array<string, mixed> $cuerpo
     * @return array{0: int, 1: array<string, mixed>}
     */
    private function login(array $cuerpo): array
    {
        $resultado = $this->autenticacion->iniciarSesion(
            $this->texto($cuerpo, 'email'),
            $this->texto($cuerpo, 'password')
        );

        return [$this->estado($resultado->codigo), $resultado->aJson()];
    }

    /**
     * @return array{0: int, 1: array<string, mixed>}
     */
    private function sesion(?string $autorizacion): array
    {
        $resultado = $this->autenticacion->comprobarSesion($this->token($autorizacion));
        $json = $resultado->aJson();
        unset($json['token']);

        return [$this->estado($resultado->codigo), $json];
    }

    /**
     * @param array<string, mixed> $cuerpo
     * @return array{0: int, 1: array<string, mixed>}
     */
    private function perfil(array $cuerpo, ?string $autorizacion): array
    {
        $token = $this->token($autorizacion);
        $quitarFoto = ($cuerpo['quitar_foto'] ?? false) === true;
        $rutaFoto = '';
        if (!$quitarFoto) {
            $rutaFoto = $this->guardarFotoSiViene($cuerpo, $token);
            if ($rutaFoto === false) {
                return [400, ['ok' => false, 'mensaje' => 'La foto de perfil no es válida.']];
            }
        }

        $resultado = $this->autenticacion->actualizarPerfil(
            $token,
            $this->texto($cuerpo, 'nombre'),
            $this->texto($cuerpo, 'apellidos'),
            $this->texto($cuerpo, 'email'),
            $this->texto($cuerpo, 'password'),
            $rutaFoto,
            $this->texto($cuerpo, 'password_nueva'),
            $quitarFoto
        );
        $json = $resultado->aJson();
        unset($json['token']);

        return [$this->estado($resultado->codigo), $json];
    }

    /**
     * @param array<string, mixed> $cuerpo
     * @return array{0: int, 1: array<string, mixed>}
     */
    private function borrarCuenta(array $cuerpo, ?string $autorizacion): array
    {
        $resultado = $this->autenticacion->borrarCuenta(
            $this->token($autorizacion),
            $this->texto($cuerpo, 'password')
        );
        $json = $resultado->aJson();
        unset($json['token'], $json['usuario']);

        return [$this->estado($resultado->codigo), $json];
    }

    /**
     * @return array{0: int, 1: array<string, mixed>}
     */
    private function listarUsuarios(?string $autorizacion): array
    {
        $resultado = $this->autenticacion->listarUsuarios($this->token($autorizacion));

        return [$this->estado($resultado['codigo']), [
            'ok' => $resultado['ok'],
            'mensaje' => $resultado['mensaje'],
            'usuarios' => $resultado['usuarios'],
        ]];
    }

    /**
     * @param array<string, mixed> $cuerpo
     * @return array{0: int, 1: array<string, mixed>}
     */
    private function crearUsuarioAdmin(array $cuerpo, ?string $autorizacion): array
    {
        $resultado = $this->autenticacion->crearUsuarioAdmin(
            $this->token($autorizacion),
            $this->texto($cuerpo, 'nombre'),
            $this->texto($cuerpo, 'apellidos'),
            $this->texto($cuerpo, 'email'),
            $this->texto($cuerpo, 'rol')
        );

        return [$this->estado($resultado['codigo']), [
            'ok' => $resultado['ok'],
            'mensaje' => $resultado['mensaje'],
            'password_temporal' => $resultado['password_temporal'],
            'usuario' => $resultado['usuario'],
        ]];
    }

    /**
     * @param array<string, mixed> $cuerpo
     * @return array{0: int, 1: array<string, mixed>}
     */
    private function actualizarUsuarioAdmin(int $idUsuario, array $cuerpo, ?string $autorizacion): array
    {
        $activo = $cuerpo['activo'] ?? true;
        $resultado = $this->autenticacion->actualizarUsuarioAdmin(
            $this->token($autorizacion),
            $idUsuario,
            $this->texto($cuerpo, 'nombre'),
            $this->texto($cuerpo, 'apellidos'),
            $this->texto($cuerpo, 'email'),
            is_bool($activo) ? $activo : filter_var($activo, FILTER_VALIDATE_BOOLEAN),
            $this->texto($cuerpo, 'rol')
        );
        $json = $resultado->aJson();
        unset($json['token']);

        return [$this->estado($resultado->codigo), $json];
    }

    /**
     * @return array{0: int, 1: array<string, mixed>}
     */
    private function borrarUsuarioAdmin(int $idUsuario, ?string $autorizacion): array
    {
        $resultado = $this->autenticacion->borrarUsuarioAdmin($this->token($autorizacion), $idUsuario);
        $json = $resultado->aJson();
        unset($json['token']);

        return [$this->estado($resultado->codigo), $json];
    }

    /**
     * @return array{0: int, 1: array<string, mixed>}
     */
    private function generarPasswordAdmin(int $idUsuario, ?string $autorizacion): array
    {
        $resultado = $this->autenticacion->generarPasswordAdmin($this->token($autorizacion), $idUsuario);

        return [$this->estado($resultado['codigo']), [
            'ok' => $resultado['ok'],
            'mensaje' => $resultado['mensaje'],
            'password_temporal' => $resultado['password_temporal'],
            'usuario' => $resultado['usuario'],
        ]];
    }

    /**
     * Guarda la imagen en public/perfiles y devuelve la ruta relativa.
     * Cadena vacía = no hay foto nueva. false = la imagen no sirve.
     */
    private function guardarFotoSiViene(array $cuerpo, string $token): string|false
    {
        $base64 = $this->texto($cuerpo, 'foto_base64');
        if ($base64 === '') {
            return '';
        }

        if (!preg_match('#^data:image/(png|jpe?g|webp);base64,#i', $base64, $tipo)) {
            return false;
        }
        $extension = strtolower($tipo[1]) === 'jpeg' ? 'jpg' : strtolower($tipo[1]);
        $binario = base64_decode(substr($base64, strpos($base64, ',') + 1), true);
        if ($binario === false || $binario === '' || strlen($binario) > 2_000_000) {
            return false;
        }

        $sesion = $this->autenticacion->comprobarSesion($token);
        if (!$sesion->ok) {
            return false;
        }

        $carpeta = dirname(__DIR__, 2) . '/public/perfiles';
        if (!is_dir($carpeta) && !mkdir($carpeta, 0775, true) && !is_dir($carpeta)) {
            return false;
        }

        $nombre = 'u_' . $sesion->usuario['id_usuario'] . '.' . $extension;
        $rutaCompleta = $carpeta . DIRECTORY_SEPARATOR . $nombre;
        if (file_put_contents($rutaCompleta, $binario) === false) {
            return false;
        }

        return 'perfiles/' . $nombre;
    }

    private function estado(string $codigo): int
    {
        return match ($codigo) {
            'ok' => 200,
            'datos_invalidos' => 400,
            'correo_registrado' => 409,
            'credenciales_invalidas', 'cuenta_inactiva', 'sesion_invalida' => 401,
            'sin_permiso' => 403,
            default => 500,
        };
    }

    /**
     * @param array<string, mixed> $cuerpo
     */
    private function texto(array $cuerpo, string $campo): string
    {
        $valor = $cuerpo[$campo] ?? '';

        return is_string($valor) ? $valor : '';
    }

    private function token(?string $autorizacion): string
    {
        if ($autorizacion === null) {
            return '';
        }
        if (str_starts_with($autorizacion, 'Bearer ')) {
            return trim(substr($autorizacion, 7));
        }

        return trim($autorizacion);
    }
}
