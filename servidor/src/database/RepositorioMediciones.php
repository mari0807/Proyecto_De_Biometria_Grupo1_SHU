<?php

declare(strict_types=1);

namespace QueAire\Database;

interface RepositorioMediciones
{
    /**
     * Ultima medicion de cada nodo activo, con su contaminante, unidad y
     * categoria de calidad. Solo se incluyen nodos con al menos una medicion
     * (sin coordenadas no hay punto que pintar en el mapa).
     *
     * @return list<array{
     *   id_nodo: int,
     *   nombre: string,
     *   latitud: float,
     *   longitud: float,
     *   contaminante: string,
     *   unidad: string,
     *   valor: float,
     *   categoria: string,
     *   color: string,
     *   fecha_hora: string
     * }>
     */
    public function puntosMapa(): array;

    /**
     * Datos base de un nodo activo (la ubicacion sale de su ultima medicion).
     *
     * @return array{
     *   id_nodo: int,
     *   nombre: string,
     *   estado: string,
     *   contaminante: string,
     *   unidad: string,
     *   latitud: ?float,
     *   longitud: ?float
     * }|null
     */
    public function buscarNodo(int $idNodo): ?array;

    /**
     * Medicion mas reciente de un nodo.
     *
     * @return array{valor: float, categoria: string, color: string, fecha_hora: string}|null
     */
    public function ultimaMedicion(int $idNodo): ?array;

    /**
     * Serie historica de un nodo, en orden cronologico ascendente.
     *
     * @return list<array{fecha_hora: string, valor: float, categoria: string, color: string}>
     */
    public function historial(int $idNodo, int $limite): array;
}
