<?php

declare(strict_types=1);

namespace QueAire\Database;

use PDO;

final class Conexion
{
    /**
     * @param array{host: string, puerto: string, nombre: string, usuario: string, password: string} $config
     */
    public static function abrir(array $config): PDO
    {
        $dsn = sprintf(
            'mysql:host=%s;port=%s;dbname=%s;charset=utf8mb4',
            $config['host'],
            $config['puerto'],
            $config['nombre']
        );

        $pdo = new PDO($dsn, $config['usuario'], $config['password'], [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        ]);
        $pdo->exec("SET time_zone = '+00:00'");

        return $pdo;
    }
}
