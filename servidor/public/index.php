<?php

declare(strict_types=1);

use QueAire\BusinessLogic\Autenticacion;
use QueAire\Communication\ApiAuth;
use QueAire\Database\Conexion;
use QueAire\Database\UsuarioRepositorio;

require dirname(__DIR__) . '/src/database/CorreoYaRegistrado.php';
require dirname(__DIR__) . '/src/database/RepositorioUsuarios.php';
require dirname(__DIR__) . '/src/database/Conexion.php';
require dirname(__DIR__) . '/src/database/UsuarioRepositorio.php';
require dirname(__DIR__) . '/src/business_logic/ResultadoAuth.php';
require dirname(__DIR__) . '/src/business_logic/Token.php';
require dirname(__DIR__) . '/src/business_logic/Autenticacion.php';
require dirname(__DIR__) . '/src/communication/ApiAuth.php';

$rutaArchivo = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
$fichero = __DIR__ . (is_string($rutaArchivo) ? $rutaArchivo : '/');
if (
    ($rutaArchivo === '/perfiles' || str_starts_with((string) $rutaArchivo, '/perfiles/'))
    && is_file($fichero)
) {
    return false;
}

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Authorization, Content-Type');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');

$metodo = $_SERVER['REQUEST_METHOD'] ?? 'GET';
if ($metodo === 'OPTIONS') {
    http_response_code(204);
    exit;
}

/** @var array{db: array{host: string, puerto: string, nombre: string, usuario: string, password: string}, auth_secreto: string, token_ttl: int} $config */
$config = require dirname(__DIR__) . '/config.php';

try {
    $pdo = Conexion::abrir($config['db']);
} catch (Throwable $e) {
    http_response_code(500);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode([
        'ok' => false,
        'mensaje' => 'Base de datos no disponible.',
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

$api = new ApiAuth(new Autenticacion(
    new UsuarioRepositorio($pdo),
    $config['auth_secreto'],
    $config['token_ttl']
));


$ruta = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
$cuerpo = json_decode(file_get_contents('php://input') ?: '', true);
$autorizacion = $_SERVER['HTTP_AUTHORIZATION'] ?? ($_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? null);

[$estado, $respuesta] = $api->manejar(
    $metodo,
    is_string($ruta) ? $ruta : '/',
    is_array($cuerpo) ? $cuerpo : null,
    is_string($autorizacion) ? $autorizacion : null
);

http_response_code($estado);
if ($respuesta !== null) {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($respuesta, JSON_UNESCAPED_UNICODE);
}
