<?php

declare(strict_types=1);

namespace QueAire\Communication;

use QueAire\BusinessLogic\Autenticacion;
use QueAire\BusinessLogic\Mapa;

/**
 * Capa communication de H13 (mapa) y H18 (detalle de zona).
 * Exige sesion valida (Authorization: Bearer <token>). No escribe SQL:
 * traduce la logica de Mapa a HTTP.
 */
final class ApiMapa
{
    public function __construct(
        private Autenticacion $autenticacion,
        private Mapa $mapa
    ) {
    }

    /**
     * @param array<string, mixed>|null $cuerpo
     * @return array{0: int, 1: array<string, mixed>|null}
     */
    public function manejar(string $metodo, string $ruta, ?array $cuerpo, ?string $autorizacion): array
    {
        $ruta = rtrim($ruta, '/') ?: '/';

        if ($metodo === 'GET' && $ruta === '/api/mapa') {
            return $this->puntos($autorizacion);
        }
        if ($metodo === 'GET' && preg_match('#^/api/mapa/zona/(\d+)$#', $ruta, $m) === 1) {
            return $this->detalle((int) $m[1], $autorizacion);
        }

        return [404, ['ok' => false, 'mensaje' => 'Ruta no encontrada.']];
    }

    /**
     * @return array{0: int, 1: array<string, mixed>}
     */
    private function puntos(?string $autorizacion): array
    {
        $sesion = $this->autenticacion->comprobarSesion($this->token($autorizacion));
        if (!$sesion->ok) {
            return [401, ['ok' => false, 'mensaje' => 'La sesión no es válida.']];
        }

        $resultado = $this->mapa->puntos();

        return [200, [
            'ok' => true,
            'mensaje' => $resultado['mensaje'],
            'puntos' => $resultado['puntos'],
        ]];
    }

    /**
     * @return array{0: int, 1: array<string, mixed>}
     */
    private function detalle(int $idNodo, ?string $autorizacion): array
    {
        $sesion = $this->autenticacion->comprobarSesion($this->token($autorizacion));
        if (!$sesion->ok) {
            return [401, ['ok' => false, 'mensaje' => 'La sesión no es válida.']];
        }

        $resultado = $this->mapa->detalleZona($idNodo);
        if (!$resultado['ok']) {
            return [404, ['ok' => false, 'mensaje' => $resultado['mensaje']]];
        }

        return [200, [
            'ok' => true,
            'mensaje' => $resultado['mensaje'],
            'zona' => $resultado['zona'],
        ]];
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
