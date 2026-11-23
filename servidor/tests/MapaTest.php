<?php

declare(strict_types=1);

use QueAire\BusinessLogic\Logica;
use QueAire\Communication\ApiMapa;

require dirname(__DIR__) . '/src/database/CorreoYaRegistrado.php';
require dirname(__DIR__) . '/src/database/RepositorioUsuarios.php';
require dirname(__DIR__) . '/src/database/RepositorioMediciones.php';
require dirname(__DIR__) . '/src/business_logic/ResultadoAuth.php';
require dirname(__DIR__) . '/src/business_logic/Token.php';
require dirname(__DIR__) . '/src/business_logic/Logica.php';
require dirname(__DIR__) . '/src/communication/ApiMapa.php';
require __DIR__ . '/RepositorioMemoria.php';
require __DIR__ . '/RepositorioMedicionesMemoria.php';

$fallos = 0;

function afirmar(bool $condicion, string $mensaje): void
{
    global $fallos;
    if ($condicion) {
        echo "OK  $mensaje\n";
        return;
    }
    $fallos++;
    fwrite(STDERR, "FALLO  $mensaje\n");
}

$mediciones = new RepositorioMedicionesMemoria();
$mediciones->nodos[1] = [
    'id_nodo' => 1, 'nombre' => 'Campus UPV', 'estado' => 'activo',
    'contaminante' => 'CO2', 'unidad' => 'ppm', 'latitud' => 38.996460,
    'longitud' => -0.166030, 'activo' => true,
];
$mediciones->nodos[2] = [
    'id_nodo' => 2, 'nombre' => 'Port de Gandia', 'estado' => 'activo',
    'contaminante' => 'NO2', 'unidad' => 'ppb', 'latitud' => 38.974800,
    'longitud' => -0.165500, 'activo' => true,
];
$mediciones->nodos[3] = [
    'id_nodo' => 3, 'nombre' => 'Av. del Port', 'estado' => 'activo',
    'contaminante' => 'PM2.5', 'unidad' => 'µg/m³', 'latitud' => null,
    'longitud' => null, 'activo' => true,
];
$mediciones->nodos[4] = [
    'id_nodo' => 4, 'nombre' => 'Nodo inactivo', 'estado' => 'inactivo',
    'contaminante' => 'O3', 'unidad' => 'ppb', 'latitud' => 38.98,
    'longitud' => -0.16, 'activo' => false,
];

$mediciones->mediciones[] = ['id_nodo' => 1, 'valor' => 610.0, 'categoria' => 'buena', 'color' => '#2ECC71', 'fecha_hora' => '2026-11-19 08:00:00', 'latitud' => 38.996460, 'longitud' => -0.166030];
$mediciones->mediciones[] = ['id_nodo' => 1, 'valor' => 1650.0, 'categoria' => 'mala', 'color' => '#E67E22', 'fecha_hora' => '2026-11-19 10:00:00', 'latitud' => 38.996460, 'longitud' => -0.166030];
$mediciones->mediciones[] = ['id_nodo' => 2, 'valor' => 310.0, 'categoria' => 'muy_mala', 'color' => '#E74C3C', 'fecha_hora' => '2026-11-19 09:00:00', 'latitud' => 38.974800, 'longitud' => -0.165500];
$mediciones->mediciones[] = ['id_nodo' => 4, 'valor' => 40.0, 'categoria' => 'buena', 'color' => '#2ECC71', 'fecha_hora' => '2026-11-19 09:00:00', 'latitud' => 38.98, 'longitud' => -0.16];

$repoUsuarios = new RepositorioMemoria();
$logica = new Logica($repoUsuarios, 'secreto-test', 3600, null, $mediciones);

$puntos = $logica->puntos();
afirmar($puntos['ok'] && count($puntos['puntos']) === 2, 'H13 devuelve solo nodos activos con datos');
afirmar($puntos['puntos'][0]['id_nodo'] === 1 && $puntos['puntos'][0]['categoria'] === 'mala', 'H13 usa la ultima medicion del nodo');
afirmar($puntos['puntos'][0]['color'] === '#E67E22' && $puntos['puntos'][0]['unidad'] === 'ppm', 'H13 incluye color y unidad');
afirmar($puntos['puntos'][1]['id_nodo'] === 2 && $puntos['puntos'][1]['categoria'] === 'muy_mala', 'H13 clasifica cada punto por su contaminante');
afirmar($puntos['puntos'][0]['latitud'] === 38.996460 && $puntos['puntos'][0]['longitud'] === -0.166030, 'H13 incluye coordenadas');

$detalle = $logica->detalleZona(1);
afirmar($detalle['ok'] && $detalle['zona']['actual']['valor'] === 1650.0, 'H18 devuelve el valor actual de la zona');
afirmar(count($detalle['zona']['historial']) === 2, 'H18 devuelve el historial completo');
afirmar(
    $detalle['zona']['historial'][0]['fecha_hora'] === '2026-11-19 08:00:00'
    && $detalle['zona']['historial'][1]['fecha_hora'] === '2026-11-19 10:00:00',
    'H18 ordena el historial cronologicamente'
);
afirmar($detalle['zona']['unidad'] === 'ppm' && $detalle['zona']['contaminante'] === 'CO2', 'H18 incluye unidad y contaminante');

$sinDatos = $logica->detalleZona(3);
afirmar($sinDatos['ok'] && $sinDatos['zona']['actual'] === null && $sinDatos['zona']['historial'] === [], 'H18 informa cuando no hay mediciones');

$inexistente = $logica->detalleZona(99);
afirmar(!$inexistente['ok'] && $inexistente['codigo'] === 'no_encontrado', 'H18 avisa de zona inexistente');

$logica->registrar('Ana', 'Pérez', 'ana@correo.com', 'Test1234!');
$token = $logica->iniciarSesion('ana@correo.com', 'Test1234!')->token;

$api = new ApiMapa($logica);

[$sinToken, ] = $api->manejar('GET', '/api/mapa', null, null);
afirmar($sinToken === 401, 'H13 exige token en /api/mapa');

[$conToken, $cuerpoPuntos] = $api->manejar('GET', '/api/mapa', null, 'Bearer ' . $token);
afirmar($conToken === 200 && ($cuerpoPuntos['ok'] ?? false) && count($cuerpoPuntos['puntos']) === 2, 'H13 responde 200 con token valido');

[$detalleOk, $cuerpoDetalle] = $api->manejar('GET', '/api/mapa/zona/1', null, 'Bearer ' . $token);
afirmar($detalleOk === 200 && ($cuerpoDetalle['zona']['id_nodo'] ?? 0) === 1, 'H18 responde 200 con el detalle de la zona');

[$detalle404, ] = $api->manejar('GET', '/api/mapa/zona/99', null, 'Bearer ' . $token);
afirmar($detalle404 === 404, 'H18 responde 404 si la zona no existe');

[$detalleSinToken, ] = $api->manejar('GET', '/api/mapa/zona/1', null, null);
afirmar($detalleSinToken === 401, 'H18 exige token en el detalle');

$codigoLogica = (string) file_get_contents(dirname(__DIR__) . '/src/business_logic/Logica.php');
$comunicacion = (string) file_get_contents(dirname(__DIR__) . '/src/communication/ApiMapa.php');
afirmar(!preg_match('/HttpRequest|HttpResponse|\bPDO\b|SELECT |INSERT |UPDATE /', $codigoLogica), 'la logica no depende de HTTP ni de SQL');
afirmar(!preg_match('/SELECT |INSERT |UPDATE |DELETE FROM/', $comunicacion), 'la comunicacion del mapa no escribe SQL');

if ($fallos > 0) {
    fwrite(STDERR, "$fallos fallo(s)\n");
    exit(1);
}

echo "Todos los tests del mapa han pasado.\n";
