<?php

declare(strict_types=1);

namespace QueAire\Database;

use PDO;

final class MedicionRepositorio implements RepositorioMediciones
{
    private const SIN_DATOS = 'sin_datos';
    private const COLOR_SIN_DATOS = '#95A5A6';

    public function __construct(private PDO $pdo)
    {
    }

    public function puntosMapa(): array
    {
        $stmt = $this->pdo->query(
            'SELECT n.id_nodo, n.nombre,
                    m.latitud, m.longitud, m.valor, m.fecha_hora,
                    c.nombre AS contaminante, c.unidad_medida AS unidad,
                    u.categoria, u.color
             FROM NODO_SENSOR n
             JOIN CONTAMINANTE c ON c.id_contaminante = n.id_contaminante
             JOIN MEDICION m ON m.id_nodo = n.id_nodo
               AND m.id_contaminante = n.id_contaminante
               AND m.fecha_hora = (
                     SELECT MAX(m2.fecha_hora)
                     FROM MEDICION m2
                     WHERE m2.id_nodo = n.id_nodo
                       AND m2.id_contaminante = n.id_contaminante)
             LEFT JOIN UMBRAL u ON u.id_contaminante = m.id_contaminante
               AND m.valor >= u.valor_desde
               AND (u.valor_hasta IS NULL OR m.valor < u.valor_hasta)
             WHERE n.activo = TRUE
             ORDER BY n.id_nodo ASC'
        );
        $filas = $stmt === false ? [] : $stmt->fetchAll();

        $puntos = [];
        foreach ($filas as $fila) {
            $puntos[] = [
                'id_nodo' => (int) $fila['id_nodo'],
                'nombre' => (string) ($fila['nombre'] ?? ''),
                'latitud' => (float) $fila['latitud'],
                'longitud' => (float) $fila['longitud'],
                'contaminante' => (string) $fila['contaminante'],
                'unidad' => (string) $fila['unidad'],
                'valor' => (float) $fila['valor'],
                'categoria' => (string) ($fila['categoria'] ?? self::SIN_DATOS),
                'color' => (string) ($fila['color'] ?? self::COLOR_SIN_DATOS),
                'fecha_hora' => (string) $fila['fecha_hora'],
            ];
        }

        return $puntos;
    }

    public function buscarNodo(int $idNodo): ?array
    {
        $stmt = $this->pdo->prepare(
            'SELECT n.id_nodo, n.nombre, n.estado,
                    c.nombre AS contaminante, c.unidad_medida AS unidad,
                    m.latitud, m.longitud
             FROM NODO_SENSOR n
             JOIN CONTAMINANTE c ON c.id_contaminante = n.id_contaminante
             LEFT JOIN MEDICION m ON m.id_nodo = n.id_nodo
               AND m.id_contaminante = n.id_contaminante
               AND m.fecha_hora = (
                     SELECT MAX(m2.fecha_hora)
                     FROM MEDICION m2
                     WHERE m2.id_nodo = n.id_nodo
                       AND m2.id_contaminante = n.id_contaminante)
             WHERE n.id_nodo = ? AND n.activo = TRUE
             LIMIT 1'
        );
        $stmt->execute([$idNodo]);
        $fila = $stmt->fetch();
        if ($fila === false) {
            return null;
        }

        return [
            'id_nodo' => (int) $fila['id_nodo'],
            'nombre' => (string) ($fila['nombre'] ?? ''),
            'estado' => (string) $fila['estado'],
            'contaminante' => (string) $fila['contaminante'],
            'unidad' => (string) $fila['unidad'],
            'latitud' => $fila['latitud'] === null ? null : (float) $fila['latitud'],
            'longitud' => $fila['longitud'] === null ? null : (float) $fila['longitud'],
        ];
    }

    public function ultimaMedicion(int $idNodo): ?array
    {
        $stmt = $this->pdo->prepare(
            'SELECT m.valor, m.fecha_hora, u.categoria, u.color
             FROM MEDICION m
             LEFT JOIN UMBRAL u ON u.id_contaminante = m.id_contaminante
               AND m.valor >= u.valor_desde
               AND (u.valor_hasta IS NULL OR m.valor < u.valor_hasta)
             WHERE m.id_nodo = ?
             ORDER BY m.fecha_hora DESC
             LIMIT 1'
        );
        $stmt->execute([$idNodo]);
        $fila = $stmt->fetch();
        if ($fila === false) {
            return null;
        }

        return [
            'valor' => (float) $fila['valor'],
            'categoria' => (string) ($fila['categoria'] ?? self::SIN_DATOS),
            'color' => (string) ($fila['color'] ?? self::COLOR_SIN_DATOS),
            'fecha_hora' => (string) $fila['fecha_hora'],
        ];
    }

    public function historial(int $idNodo, int $limite): array
    {
        $limite = max(1, min($limite, 500));
        $stmt = $this->pdo->prepare(
            'SELECT m.valor, m.fecha_hora, u.categoria, u.color
             FROM MEDICION m
             LEFT JOIN UMBRAL u ON u.id_contaminante = m.id_contaminante
               AND m.valor >= u.valor_desde
               AND (u.valor_hasta IS NULL OR m.valor < u.valor_hasta)
             WHERE m.id_nodo = ?
             ORDER BY m.fecha_hora DESC
             LIMIT ' . $limite
        );
        $stmt->execute([$idNodo]);
        $filas = $stmt->fetchAll();

        $historial = [];
        foreach ($filas as $fila) {
            $historial[] = [
                'fecha_hora' => (string) $fila['fecha_hora'],
                'valor' => (float) $fila['valor'],
                'categoria' => (string) ($fila['categoria'] ?? self::SIN_DATOS),
                'color' => (string) ($fila['color'] ?? self::COLOR_SIN_DATOS),
            ];
        }

        return array_reverse($historial);
    }
}
