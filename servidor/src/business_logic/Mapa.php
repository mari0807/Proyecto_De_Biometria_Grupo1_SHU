<?php

declare(strict_types=1);

namespace QueAire\BusinessLogic;

use QueAire\Database\RepositorioMediciones;

/**
 *                 --------- Mapa ------------------
 *                 |
 *                 | repositorio: RepositorioMediciones
 *                 | historialMaximo: N
 *                 |
 * --> puntos() --> (ok: B, codigo: Text, mensaje: Text, puntos: *)
 *                 |
 *                 |
 * id_nodo: N --> detalleZona() --> (ok: B, codigo: Text, mensaje: Text, zona: *)
 *                 |
 *                 ------------------------------------------------
 *
 * Reglas:
 * - Solo nodos activos con datos; cada punto lleva categoria y color (UMBRAL).
 * - Sin HTTP ni SQL: el acceso a datos lo hace el repositorio.
 */
final class Mapa
{
    private const HISTORIAL_MAXIMO = 48;

    public function __construct(
        private RepositorioMediciones $repositorio,
        private int $historialMaximo = self::HISTORIAL_MAXIMO
    ) {
    }

    /**
     * --------------------
     * --> puntos() --> (ok: B, codigo: Text, mensaje: Text, puntos: *)
     * --------------------
     * Puntos del mapa: ultima medicion de cada nodo activo con su color.
     *
     * @return array{ok: bool, codigo: string, mensaje: string, puntos: list<array<string, mixed>>}
     */
    public function puntos(): array
    {
        $puntos = $this->repositorio->puntosMapa();

        return [
            'ok' => true,
            'codigo' => 'ok',
            'mensaje' => $puntos === []
                ? 'No hay mediciones disponibles.'
                : 'Puntos del mapa cargados.',
            'puntos' => $puntos,
        ];
    }

    /**
     * --------------------
     * id_nodo: N --> detalleZona() --> (ok: B, codigo: Text, mensaje: Text, zona: *)
     * --------------------
     * Detalle de una zona (nodo): datos, medicion actual e historico.
     *
     * @return array{ok: bool, codigo: string, mensaje: string, zona: array<string, mixed>|null}
     */
    public function detalleZona(int $idNodo): array
    {
        $nodo = $this->repositorio->buscarNodo($idNodo);
        if ($nodo === null) {
            return [
                'ok' => false,
                'codigo' => 'no_encontrado',
                'mensaje' => 'No se ha encontrado la zona.',
                'zona' => null,
            ];
        }

        $actual = $this->repositorio->ultimaMedicion($idNodo);
        $historial = $this->repositorio->historial($idNodo, $this->historialMaximo);

        return [
            'ok' => true,
            'codigo' => 'ok',
            'mensaje' => $actual === null
                ? 'La zona no tiene mediciones disponibles.'
                : 'Detalle de la zona cargado.',
            'zona' => [
                'id_nodo' => $nodo['id_nodo'],
                'nombre' => $nodo['nombre'],
                'estado' => $nodo['estado'],
                'contaminante' => $nodo['contaminante'],
                'unidad' => $nodo['unidad'],
                'latitud' => $nodo['latitud'],
                'longitud' => $nodo['longitud'],
                'actual' => $actual,
                'historial' => $historial,
            ],
        ];
    }
}
