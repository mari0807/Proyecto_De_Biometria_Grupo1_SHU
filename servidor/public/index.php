<?php

declare(strict_types=1);

use QueAire\BusinessLogic\Logica;
use QueAire\Communication\ApiAuth;
use QueAire\Communication\ApiMapa;
use QueAire\Database\Conexion;
use QueAire\Database\MedicionRepositorio;
use QueAire\Database\UsuarioRepositorio;

require dirname(__DIR__) . '/src/database/CorreoYaRegistrado.php';
require dirname(__DIR__) . '/src/database/RepositorioUsuarios.php';
require dirname(__DIR__) . '/src/database/RepositorioMediciones.php';
require dirname(__DIR__) . '/src/database/Conexion.php';
require dirname(__DIR__) . '/src/database/UsuarioRepositorio.php';
require dirname(__DIR__) . '/src/database/MedicionRepositorio.php';
require dirname(__DIR__) . '/src/business_logic/ResultadoAuth.php';
require dirname(__DIR__) . '/src/business_logic/Token.php';
require dirname(__DIR__) . '/src/business_logic/Logica.php';
require dirname(__DIR__) . '/src/communication/ApiAuth.php';
require dirname(__DIR__) . '/src/communication/ApiMapa.php';

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

$logica = new Logica(
    new UsuarioRepositorio($pdo),
    $config['auth_secreto'],
    $config['token_ttl'],
    null,
    new MedicionRepositorio($pdo)
);
$api = new ApiAuth($logica);
$apiMapa = new ApiMapa($logica);


$ruta = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
$ruta = is_string($ruta) ? $ruta : '/';
$cuerpo = json_decode(file_get_contents('php://input') ?: '', true);
$autorizacion = $_SERVER['HTTP_AUTHORIZATION'] ?? ($_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? null);
$autorizacion = is_string($autorizacion) ? $autorizacion : null;

if (str_starts_with($ruta, '/api/mapa')) {
    [$estado, $respuesta] = $apiMapa->manejar(
        $metodo,
        $ruta,
        is_array($cuerpo) ? $cuerpo : null,
        $autorizacion
    );
} else {
    [$estado, $respuesta] = $api->manejar(
        $metodo,
        $ruta,
        is_array($cuerpo) ? $cuerpo : null,
        $autorizacion
    );
}

http_response_code($estado);
if ($respuesta !== null) {
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($respuesta, JSON_UNESCAPED_UNICODE);
}
