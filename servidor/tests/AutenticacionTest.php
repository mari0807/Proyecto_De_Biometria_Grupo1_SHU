<?php

declare(strict_types=1);

use QueAire\BusinessLogic\Logica;
use QueAire\Communication\ApiAuth;

require dirname(__DIR__) . '/src/database/CorreoYaRegistrado.php';
require dirname(__DIR__) . '/src/database/RepositorioUsuarios.php';
require dirname(__DIR__) . '/src/business_logic/ResultadoAuth.php';
require dirname(__DIR__) . '/src/business_logic/Token.php';
require dirname(__DIR__) . '/src/business_logic/Logica.php';
require dirname(__DIR__) . '/src/communication/ApiAuth.php';
require __DIR__ . '/RepositorioMemoria.php';

$fallos = 0;
const CLAVE_OK = 'Test1234!';

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

function auth(RepositorioMemoria $repo, ?callable $reloj = null): Logica
{
    return new Logica($repo, 'secreto-test', 3600, $reloj);
}

$repo = new RepositorioMemoria();
$servicio = auth($repo);
$api = new ApiAuth($servicio);

$vacio = $servicio->registrar('', 'Pérez', 'ana@correo.com', CLAVE_OK);
afirmar($vacio->codigo === 'datos_invalidos', 'H10 rechaza el nombre vacío');

$sinApellidos = $servicio->registrar('Ana', '  ', 'ana@correo.com', CLAVE_OK);
afirmar($sinApellidos->codigo === 'datos_invalidos', 'H10 rechaza apellidos vacíos');

$sinClave = $servicio->registrar('Ana', 'Pérez', 'ana@correo.com', '');
afirmar($sinClave->mensaje === 'La contraseña es obligatoria.', 'H10 exige contraseña');

$claveDebil = $servicio->registrar('Ana', 'Pérez', 'ana@correo.com', '1234');
afirmar($claveDebil->codigo === 'datos_invalidos' && str_contains($claveDebil->mensaje, '8 caracteres'), 'H10 exige contraseña segura');

$emailMalo = $servicio->registrar('Ana', 'Pérez', 'ana-correo', CLAVE_OK);
afirmar($emailMalo->mensaje === 'El correo electrónico no tiene un formato correcto.', 'H10 valida el formato del correo');

$creada = $servicio->registrar('Ana', 'Pérez', 'Ana@Correo.com', CLAVE_OK);
afirmar($creada->ok && $creada->mensaje === 'Cuenta creada correctamente.', 'H10 confirma el registro');
afirmar($creada->usuario['rol'] === 'usuario' && $creada->usuario['email'] === 'ana@correo.com', 'H10 asigna rol usuario y normaliza el email');
afirmar(!array_key_exists('password_hash', $creada->usuario), 'H10 no devuelve password_hash');

$duplicado = $servicio->registrar('Ana', 'Pérez', 'ana@correo.com', CLAVE_OK);
afirmar($duplicado->codigo === 'correo_registrado', 'H10 informa si el correo ya está registrado');

$mal = $servicio->iniciarSesion('ana@correo.com', 'no-es');
afirmar($mal->codigo === 'credenciales_invalidas', 'H12 informa credenciales incorrectas');

$reloj = 1_700_000_000;
$servicio = auth($repo, static fn (): int => $reloj);
$api = new ApiAuth($servicio);
$entrada = $servicio->iniciarSesion('ana@correo.com', CLAVE_OK);
afirmar($entrada->ok && $entrada->token !== '', 'H12 inicia sesión con credenciales válidas');
afirmar(!array_key_exists('password_hash', $entrada->usuario) && !array_key_exists('foto_perfil', $entrada->usuario), 'H12 no devuelve secreto ni foto');
afirmar($repo->usuarios[1]['ultimo_acceso'] === gmdate('Y-m-d H:i:s', $reloj), 'H12 actualiza ultimo_acceso');

$repo->usuarios[1]['activo'] = false;
$inactiva = $servicio->iniciarSesion('ana@correo.com', CLAVE_OK);
afirmar($inactiva->codigo === 'cuenta_inactiva', 'H12 rechaza una cuenta inactiva');
$repo->usuarios[1]['activo'] = true;

$caducada = auth($repo, static fn (): int => $reloj + 3601);
afirmar(!$caducada->comprobarSesion($entrada->token)->ok, 'H25 rechaza un token caducado');
afirmar($servicio->cerrarSesion($entrada->token) === true, 'H25 reconoce el token al cerrar sesión');
afirmar($servicio->cerrarSesion('token-roto') === false, 'H25 no da por válido un token roto');

[$estadoRegistro, $cuerpoRegistro] = $api->manejar('POST', '/api/registro', [
    'nombre' => 'Luis',
    'apellidos' => 'García',
    'email' => 'luis@correo.com',
    'password' => CLAVE_OK,
], null);
afirmar($estadoRegistro === 200 && ($cuerpoRegistro['mensaje'] ?? '') === 'Cuenta creada correctamente.', 'API registro 200');
afirmar(!isset($cuerpoRegistro['usuario']['password_hash']), 'API registro no filtra el hash');

[$estadoDup, ] = $api->manejar('POST', '/api/registro', [
    'nombre' => 'Luis',
    'apellidos' => 'García',
    'email' => 'luis@correo.com',
    'password' => CLAVE_OK,
], null);
afirmar($estadoDup === 409, 'API registro duplicado 409');

[$estadoLogin, $cuerpoLogin] = $api->manejar('POST', '/api/login', [
    'email' => 'luis@correo.com',
    'password' => 'mala',
], null);
afirmar($estadoLogin === 401, 'API login incorrecto 401');

[$estadoOk, $cuerpoOk] = $api->manejar('POST', '/api/login', [
    'email' => 'luis@correo.com',
    'password' => CLAVE_OK,
], null);
afirmar($estadoOk === 200 && is_string($cuerpoOk['token'] ?? null) && $cuerpoOk['token'] !== '', 'API login correcto devuelve token');

[$estadoSesion, $cuerpoSesion] = $api->manejar('GET', '/api/sesion', null, 'Bearer ' . $cuerpoOk['token']);
afirmar($estadoSesion === 200 && ($cuerpoSesion['usuario']['rol'] ?? '') === 'usuario', 'API sesión válida');

[$estadoLogout, $cuerpoLogout] = $api->manejar('POST', '/api/logout', null, 'Bearer ' . $cuerpoOk['token']);
afirmar($estadoLogout === 200 && ($cuerpoLogout['mensaje'] ?? '') === 'Sesión cerrada.', 'API logout 200');

[$estadoLogin2, $cuerpoLogin2] = $api->manejar('POST', '/api/login', [
    'email' => 'luis@correo.com',
    'password' => CLAVE_OK,
], null);
$tokenPerfil = $cuerpoLogin2['token'] ?? '';
$malPassword = $servicio->actualizarPerfil($tokenPerfil, 'Luis', 'García', 'luis@correo.com', 'otra', '');
afirmar($malPassword->codigo === 'credenciales_invalidas', 'editar perfil exige la contraseña actual');
$perfilOk = $servicio->actualizarPerfil($tokenPerfil, 'Luisito', 'García', 'luis@correo.com', CLAVE_OK, 'perfiles/u_2.png');
afirmar($perfilOk->ok && $perfilOk->usuario['nombre'] === 'Luisito' && $perfilOk->usuario['foto'] === '/perfiles/u_2.png', 'editar perfil actualiza nombre y foto');
$quitarFoto = $servicio->actualizarPerfil($tokenPerfil, 'Luisito', 'García', 'luis@correo.com', CLAVE_OK, '', '', true);
afirmar($quitarFoto->ok && $quitarFoto->usuario['foto'] === '', 'editar perfil puede quitar la foto');
$correoTomado = $servicio->actualizarPerfil($tokenPerfil, 'Luisito', 'García', 'ana@correo.com', CLAVE_OK, '');
afirmar($correoTomado->codigo === 'correo_registrado', 'editar perfil no permite un correo ya usado');

$repoAdmin = new RepositorioMemoria();
$authAdmin = auth($repoAdmin);
$authAdmin->registrar('Admin', 'Root', 'root@correo.com', CLAVE_OK);
$repoAdmin->usuarios[1]['rol'] = 'admin';
$repoAdmin->usuarios[1]['id_rol'] = 1;
$loginAdmin = $authAdmin->iniciarSesion('root@correo.com', CLAVE_OK);
$tokenAdmin = $loginAdmin->token;
$authAdmin->registrar('User', 'One', 'u1@correo.com', CLAVE_OK);
$idCliente = 2;
$listaCliente = $servicio->listarUsuarios($tokenPerfil);
afirmar($listaCliente['codigo'] === 'sin_permiso', 'listar usuarios exige admin');
$listaAdmin = $authAdmin->listarUsuarios($tokenAdmin);
afirmar($listaAdmin['ok'] && count($listaAdmin['usuarios']) >= 2, 'admin lista usuarios');
$editAdmin = $authAdmin->actualizarUsuarioAdmin($tokenAdmin, $idCliente, 'User', 'Uno', 'u1@correo.com', false, 'usuario');
afirmar($editAdmin->ok && $repoAdmin->usuarios[$idCliente]['activo'] === false, 'admin puede dar de baja');
[$httpLista, $jsonLista] = (new ApiAuth($authAdmin))->manejar('GET', '/api/usuarios', null, 'Bearer ' . $tokenAdmin);
afirmar($httpLista === 200 && ($jsonLista['ok'] ?? false), 'API listar usuarios 200');
$alta = $authAdmin->crearUsuarioAdmin($tokenAdmin, 'Nueva', 'Cuenta', 'nueva@correo.com', 'usuario');
afirmar($alta['ok'] && strlen($alta['password_temporal']) >= 8, 'admin puede dar de alta usuarios');
$genPass = $authAdmin->generarPasswordAdmin($tokenAdmin, $idCliente);
afirmar($genPass['ok'] && strlen($genPass['password_temporal']) >= 8, 'admin genera contraseña temporal');
afirmar(password_verify($genPass['password_temporal'], $repoAdmin->usuarios[$idCliente]['password_hash']), 'la temporal sustituye al hash');
$borrarOtro = $authAdmin->borrarUsuarioAdmin($tokenAdmin, $idCliente);
afirmar($borrarOtro->ok && !isset($repoAdmin->usuarios[$idCliente]), 'admin borra físicamente otra cuenta');
$borrarYo = $authAdmin->borrarUsuarioAdmin($tokenAdmin, 1);
afirmar($borrarYo->codigo === 'datos_invalidos', 'admin no puede borrarse a sí mismo');

$idLuis = $perfilOk->usuario['id_usuario'];
$borrarMal = $servicio->borrarCuenta($tokenPerfil, 'otra');
afirmar($borrarMal->codigo === 'credenciales_invalidas', 'borrar cuenta exige la contraseña');
[$httpBorrar, $jsonBorrar] = $api->manejar('DELETE', '/api/cuenta', ['password' => CLAVE_OK], 'Bearer ' . $tokenPerfil);
afirmar($httpBorrar === 200 && ($jsonBorrar['ok'] ?? false), 'API borrar cuenta 200');
afirmar(!isset($repo->usuarios[$idLuis]), 'borrar cuenta propia elimina la fila');
$sesionTrasBaja = $servicio->comprobarSesion($tokenPerfil);
afirmar($sesionTrasBaja->codigo === 'sesion_invalida', 'tras borrar, la sesión deja de valer');

$logica = (string) file_get_contents(dirname(__DIR__) . '/src/business_logic/Logica.php');
$comunicacion = (string) file_get_contents(dirname(__DIR__) . '/src/communication/ApiAuth.php');
afirmar(!preg_match('/HttpRequest|HttpResponse|\bPDO\b|SELECT |INSERT |UPDATE /', $logica), 'la lógica no depende de HTTP ni de SQL');
afirmar(!preg_match('/SELECT |INSERT |UPDATE |DELETE FROM/', $comunicacion), 'la comunicación no escribe SQL');

if ($fallos > 0) {
    fwrite(STDERR, "$fallos fallo(s)\n");
    exit(1);
}

echo "Todos los tests de autenticación han pasado.\n";
