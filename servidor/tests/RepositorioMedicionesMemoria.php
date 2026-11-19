<?php

declare(strict_types=1);

use QueAire\Database\RepositorioMediciones;

final class RepositorioMedicionesMemoria implements RepositorioMediciones
{
    /**
     * @var array<int, array{id_nodo: int, nombre: string, estado: string, contaminante: string, unidad: string, latitud: ?float, longitud: ?float, activo: bool}>
     */
    public array $nodos = [];

    /**
     * @var list<array{id_nodo: int, valor: float, categoria: string, color: string, fecha_hora: string, latitud: float, longitud: float}>
     */
    public array $mediciones = [];

    public function puntosMapa(): array
    {
        $puntos = [];
        foreach ($this->nodos as $nodo) {
            if (!$nodo['activo']) {
                continue;
            }
            $ultima = $this->ultimaMedicion($nodo['id_nodo']);
            if ($ultima === null) {
                continue;
            }
            $puntos[] = [
                'id_nodo' => $nodo['id_nodo'],
                'nombre' => $nodo['nombre'],
                'latitud' => $this->ultimaUbicacion($nodo['id_nodo'])['latitud'],
                'longitud' => $this->ultimaUbicacion($nodo['id_nodo'])['longitud'],
                'contaminante' => $nodo['contaminante'],
                'unidad' => $nodo['unidad'],
                'valor' => $ultima['valor'],
                'categoria' => $ultima['categoria'],
                'color' => $ultima['color'],
                'fecha_hora' => $ultima['fecha_hora'],
            ];
        }

        return $puntos;
    }

    public function buscarNodo(int $idNodo): ?array
    {
        $nodo = $this->nodos[$idNodo] ?? null;
        if ($nodo === null || !$nodo['activo']) {
            return null;
        }
        $ubicacion = $this->ultimaUbicacion($idNodo);

        return [
            'id_nodo' => $nodo['id_nodo'],
            'nombre' => $nodo['nombre'],
            'estado' => $nodo['estado'],
            'contaminante' => $nodo['contaminante'],
            'unidad' => $nodo['unidad'],
            'latitud' => $ubicacion['latitud'],
            'longitud' => $ubicacion['longitud'],
        ];
    }

    public function ultimaMedicion(int $idNodo): ?array
    {
        $ultima = null;
        foreach ($this->mediciones as $m) {
            if ($m['id_nodo'] !== $idNodo) {
                continue;
            }
            if ($ultima === null || $m['fecha_hora'] > $ultima['fecha_hora']) {
                $ultima = $m;
            }
        }
        if ($ultima === null) {
            return null;
        }

        return [
            'valor' => $ultima['valor'],
            'categoria' => $ultima['categoria'],
            'color' => $ultima['color'],
            'fecha_hora' => $ultima['fecha_hora'],
        ];
    }

    public function historial(int $idNodo, int $limite): array
    {
        $propias = array_values(array_filter(
            $this->mediciones,
            static fn (array $m): bool => $m['id_nodo'] === $idNodo
        ));
        usort($propias, static fn (array $a, array $b): int => $a['fecha_hora'] <=> $b['fecha_hora']);
        $propias = array_slice($propias, -max(1, $limite));

        return array_map(
            static fn (array $m): array => [
                'fecha_hora' => $m['fecha_hora'],
                'valor' => $m['valor'],
                'categoria' => $m['categoria'],
                'color' => $m['color'],
            ],
            $propias
        );
    }

    /**
     * @return array{latitud: ?float, longitud: ?float}
     */
    private function ultimaUbicacion(int $idNodo): array
    {
        $ultima = null;
        foreach ($this->mediciones as $m) {
            if ($m['id_nodo'] !== $idNodo) {
                continue;
            }
            if ($ultima === null || $m['fecha_hora'] > $ultima['fecha_hora']) {
                $ultima = $m;
            }
        }

        return $ultima === null
            ? ['latitud' => null, 'longitud' => null]
            : ['latitud' => $ultima['latitud'], 'longitud' => $ultima['longitud']];
    }
}
