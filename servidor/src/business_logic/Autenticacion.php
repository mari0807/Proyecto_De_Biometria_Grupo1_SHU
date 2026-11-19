<?php

declare(strict_types=1);

namespace QueAire\BusinessLogic;

use QueAire\Database\CorreoYaRegistrado;
use QueAire\Database\RepositorioUsuarios;

final class Autenticacion
{
    private const ROL_POR_DEFECTO = 'cliente';
    private const MIN_PASSWORD = 8;
    private const MAX_PASSWORD = 72;

    /** @var callable(): int */
    private $reloj;

    public function __construct(
        private RepositorioUsuarios $repositorio,
        private string $secreto,
        private int $ttlSegundos = 600,
        ?callable $reloj = null
    ) {
        $this->reloj = $reloj ?? static fn (): int => time();
    }

    /**
     * --------------------
     * nombre: Text, apellidos: Text, email: Text, password: Text --> registrar() --> ResultadoAuth
     * --------------------
     * Campos obligatorios completos, email con formato del CHECK de USUARIO,
     * hash bcrypt y rol cliente. Si el correo ya existe, no crea otra cuenta.
     */
    public function registrar(string $nombre, string $apellidos, string $email, string $password): ResultadoAuth
    {
        $nombre = trim($nombre);
        $apellidos = trim($apellidos);
        $email = strtolower(trim($email));

        if ($nombre === '') {
            return ResultadoAuth::fallo('datos_invalidos', 'El nombre es obligatorio.');
        }
        if ($apellidos === '') {
            return ResultadoAuth::fallo('datos_invalidos', 'Los apellidos son obligatorios.');
        }
        if ($email === '') {
            return ResultadoAuth::fallo('datos_invalidos', 'El correo electrónico es obligatorio.');
        }
        if (!$this->emailValido($email)) {
            return ResultadoAuth::fallo('datos_invalidos', 'El correo electrónico no tiene un formato correcto.');
        }
        $errorPassword = $this->errorPassword($password);
        if ($errorPassword !== null) {
            return ResultadoAuth::fallo('datos_invalidos', $errorPassword);
        }

        $idRol = $this->repositorio->idRolPorNombre(self::ROL_POR_DEFECTO);
        if ($idRol === null) {
            return ResultadoAuth::fallo('datos_invalidos', 'No se puede registrar: falta el rol cliente.');
        }

        $hash = password_hash($password, PASSWORD_BCRYPT);
        if ($hash === false || strlen($hash) > 255) {
            return ResultadoAuth::fallo('datos_invalidos', 'No se ha podido proteger la contraseña.');
        }

        try {
            $id = $this->repositorio->crear($nombre, $apellidos, $email, $hash, $idRol);
        } catch (CorreoYaRegistrado) {
            return ResultadoAuth::fallo('correo_registrado', 'Ese correo ya está registrado.');
        }

        return ResultadoAuth::acierto('Cuenta creada correctamente.', [
            'id_usuario' => $id,
            'nombre' => $nombre,
            'apellidos' => $apellidos,
            'email' => $email,
            'rol' => self::ROL_POR_DEFECTO,
            'foto' => '',
        ]);
    }

    /**
     * --------------------
     * email: Text, password: Text --> iniciarSesion() --> ResultadoAuth
     * --------------------
     * Solo cuentas activas. Actualiza ultimo_acceso. No devuelve password_hash ni foto_perfil.
     */
    public function iniciarSesion(string $email, string $password): ResultadoAuth
    {
        $email = strtolower(trim($email));
        $fila = $email === '' ? null : $this->repositorio->buscarPorEmail($email);
        if ($fila === null || !password_verify($password, $fila['password_hash'])) {
            return ResultadoAuth::fallo('credenciales_invalidas', 'Correo o contraseña incorrectos.');
        }
        if (!$fila['activo']) {
            return ResultadoAuth::fallo('cuenta_inactiva', 'La cuenta no está activa.');
        }

        $ahora = ($this->reloj)();
        $this->repositorio->registrarAcceso($fila['id_usuario'], gmdate('Y-m-d H:i:s', $ahora));
        $token = (new Token())->emitir($fila['id_usuario'], $fila['rol'], $this->secreto, $ahora, $this->ttlSegundos);

        return ResultadoAuth::acierto('Sesión iniciada.', $this->publico($fila), $token);
    }

    /**
     * --------------------
     * token: Text --> cerrarSesion() --> B
     * --------------------
     * No hay sesión en base de datos. Devuelve si el token era válido;
     * la credencial almacenada la borra el cliente.
     */
    public function cerrarSesion(string $token): bool
    {
        return (new Token())->leer($token, $this->secreto, ($this->reloj)()) !== null;
    }

    /**
     * --------------------
     * token: Text --> comprobarSesion() --> ResultadoAuth
     * --------------------
     * Rechaza token caducado, firma inválida o cuenta inactiva.
     */
    public function comprobarSesion(string $token): ResultadoAuth
    {
        $datos = (new Token())->leer($token, $this->secreto, ($this->reloj)());
        if ($datos === null) {
            return ResultadoAuth::fallo('sesion_invalida', 'La sesión no es válida.');
        }

        $fila = $this->repositorio->buscarPorId($datos['id_usuario']);
        if ($fila === null || !$fila['activo']) {
            return ResultadoAuth::fallo('sesion_invalida', 'La cuenta no está activa.');
        }

        return ResultadoAuth::acierto('Sesión válida.', $this->publico($fila), '');
    }

    /**
     * --------------------
     * token: Text, nombre: Text, apellidos: Text, email: Text,
     * password: Text, foto_perfil: Text, password_nueva: Text --> actualizarPerfil() --> ResultadoAuth
     * --------------------
     * Exige la contraseña actual. foto_perfil vacío = no cambia la foto.
     * quitarFoto = true borra la foto (foto_perfil vacío en BD).
     * password_nueva vacío = no cambia la contraseña.
     * No guarda binarios: solo la ruta relativa ya guardada en el sistema de ficheros.
     */
    public function actualizarPerfil(
        string $token,
        string $nombre,
        string $apellidos,
        string $email,
        string $password,
        string $fotoPerfil = '',
        string $passwordNueva = '',
        bool $quitarFoto = false
    ): ResultadoAuth {
        $datos = (new Token())->leer($token, $this->secreto, ($this->reloj)());
        if ($datos === null) {
            return ResultadoAuth::fallo('sesion_invalida', 'La sesión no es válida.');
        }

        $fila = $this->repositorio->buscarPorId($datos['id_usuario']);
        if ($fila === null || !$fila['activo']) {
            return ResultadoAuth::fallo('sesion_invalida', 'La cuenta no está activa.');
        }
        if ($password === '' || !password_verify($password, $fila['password_hash'])) {
            return ResultadoAuth::fallo('credenciales_invalidas', 'La contraseña no es correcta.');
        }

        $nombre = trim($nombre);
        $apellidos = trim($apellidos);
        $email = strtolower(trim($email));
        $fotoPerfil = trim($fotoPerfil);
        $passwordNueva = trim($passwordNueva);

        if ($nombre === '') {
            return ResultadoAuth::fallo('datos_invalidos', 'El nombre es obligatorio.');
        }
        if ($apellidos === '') {
            return ResultadoAuth::fallo('datos_invalidos', 'Los apellidos son obligatorios.');
        }
        if ($email === '') {
            return ResultadoAuth::fallo('datos_invalidos', 'El correo electrónico es obligatorio.');
        }
        if (!$this->emailValido($email)) {
            return ResultadoAuth::fallo('datos_invalidos', 'El correo electrónico no tiene un formato correcto.');
        }
        if ($quitarFoto && $fotoPerfil !== '') {
            return ResultadoAuth::fallo('datos_invalidos', 'No se puede cambiar y quitar la foto a la vez.');
        }
        if ($fotoPerfil !== '' && (strlen($fotoPerfil) > 255 || str_contains($fotoPerfil, '..'))) {
            return ResultadoAuth::fallo('datos_invalidos', 'La foto de perfil no es válida.');
        }
        if ($passwordNueva !== '') {
            $errorPassword = $this->errorPassword($passwordNueva);
            if ($errorPassword !== null) {
                return ResultadoAuth::fallo('datos_invalidos', $errorPassword);
            }
        }

        $rutaFoto = $quitarFoto ? '' : ($fotoPerfil === '' ? null : $fotoPerfil);
        $hashNuevo = $passwordNueva === '' ? null : password_hash($passwordNueva, PASSWORD_BCRYPT);
        try {
            $this->repositorio->actualizarPerfil(
                $fila['id_usuario'],
                $nombre,
                $apellidos,
                $email,
                $rutaFoto,
                $hashNuevo
            );
        } catch (CorreoYaRegistrado) {
            return ResultadoAuth::fallo('correo_registrado', 'Ese correo ya está registrado.');
        }

        $actualizado = $this->repositorio->buscarPorId($fila['id_usuario']);
        if ($actualizado === null) {
            return ResultadoAuth::fallo('sesion_invalida', 'La cuenta no está activa.');
        }

        return ResultadoAuth::acierto('Perfil actualizado correctamente.', $this->publico($actualizado), '');
    }

    /**
     * --------------------
     * token: Text, password: Text --> borrarCuenta() --> ResultadoAuth
     * --------------------
     * Baja lógica (activo = FALSE). Exige la contraseña actual.
     */
    public function borrarCuenta(string $token, string $password): ResultadoAuth
    {
        $datos = (new Token())->leer($token, $this->secreto, ($this->reloj)());
        if ($datos === null) {
            return ResultadoAuth::fallo('sesion_invalida', 'La sesión no es válida.');
        }

        $fila = $this->repositorio->buscarPorId($datos['id_usuario']);
        if ($fila === null || !$fila['activo']) {
            return ResultadoAuth::fallo('sesion_invalida', 'La cuenta no está activa.');
        }
        if ($password === '' || !password_verify($password, $fila['password_hash'])) {
            return ResultadoAuth::fallo('credenciales_invalidas', 'La contraseña no es correcta.');
        }

        try {
            $this->repositorio->eliminar($fila['id_usuario']);
        } catch (\RuntimeException $e) {
            return ResultadoAuth::fallo('datos_invalidos', $e->getMessage());
        }

        return ResultadoAuth::acierto('Cuenta eliminada correctamente.', ResultadoAuth::usuarioVacio(), '');
    }

    /**
     * --------------------
     * token: Text --> listarUsuarios() --> (ok: B, codigo: Text, mensaje: Text, usuarios: *)
     * --------------------
     * Solo admin. No incluye password_hash.
     *
     * @return array{ok: bool, codigo: string, mensaje: string, usuarios: list<array{id_usuario: int, nombre: string, apellidos: string, email: string, rol: string, activo: bool, foto: string}>}
     */
    public function listarUsuarios(string $token): array
    {
        $admin = $this->exigirAdmin($token);
        if ($admin !== null) {
            return ['ok' => false, 'codigo' => $admin->codigo, 'mensaje' => $admin->mensaje, 'usuarios' => []];
        }

        $usuarios = [];
        foreach ($this->repositorio->listarTodos() as $fila) {
            $usuarios[] = [
                'id_usuario' => $fila['id_usuario'],
                'nombre' => $fila['nombre'],
                'apellidos' => $fila['apellidos'],
                'email' => $fila['email'],
                'rol' => $fila['rol'],
                'activo' => $fila['activo'],
                'foto' => $this->publico($fila)['foto'],
            ];
        }

        return ['ok' => true, 'codigo' => 'ok', 'mensaje' => 'Listado de usuarios.', 'usuarios' => $usuarios];
    }

    /**
     * --------------------
     * token: Text, id_usuario: N, nombre: Text, apellidos: Text,
     * email: Text, activo: B, rol: Text --> actualizarUsuarioAdmin() --> ResultadoAuth
     * --------------------
     * Solo admin. Puede cambiar datos, rol (admin|cliente) y alta/baja lógica.
     */
    public function actualizarUsuarioAdmin(
        string $token,
        int $idUsuario,
        string $nombre,
        string $apellidos,
        string $email,
        bool $activo,
        string $rol
    ): ResultadoAuth {
        $admin = $this->exigirAdmin($token);
        if ($admin !== null) {
            return $admin;
        }

        $datos = (new Token())->leer($token, $this->secreto, ($this->reloj)());
        $idAdmin = $datos['id_usuario'] ?? 0;

        $fila = $this->repositorio->buscarPorId($idUsuario);
        if ($fila === null) {
            return ResultadoAuth::fallo('datos_invalidos', 'No se ha encontrado ese usuario.');
        }

        $nombre = trim($nombre);
        $apellidos = trim($apellidos);
        $email = strtolower(trim($email));
        $rol = strtolower(trim($rol));

        if ($nombre === '') {
            return ResultadoAuth::fallo('datos_invalidos', 'El nombre es obligatorio.');
        }
        if ($apellidos === '') {
            return ResultadoAuth::fallo('datos_invalidos', 'Los apellidos son obligatorios.');
        }
        if ($email === '' || !$this->emailValido($email)) {
            return ResultadoAuth::fallo('datos_invalidos', 'El correo electrónico no tiene un formato correcto.');
        }
        if ($rol !== 'admin' && $rol !== 'cliente') {
            return ResultadoAuth::fallo('datos_invalidos', 'El rol debe ser admin o cliente.');
        }
        if ($idUsuario === $idAdmin && !$activo) {
            return ResultadoAuth::fallo('datos_invalidos', 'No puedes darte de baja a ti mismo.');
        }
        if ($idUsuario === $idAdmin && $rol !== 'admin') {
            return ResultadoAuth::fallo('datos_invalidos', 'No puedes quitarte el rol de administrador.');
        }

        $idRol = $this->repositorio->idRolPorNombre($rol);
        if ($idRol === null) {
            return ResultadoAuth::fallo('datos_invalidos', 'El rol indicado no existe.');
        }

        try {
            $this->repositorio->actualizarPorAdmin($idUsuario, $nombre, $apellidos, $email, $activo, $idRol);
        } catch (CorreoYaRegistrado) {
            return ResultadoAuth::fallo('correo_registrado', 'Ese correo ya está registrado.');
        }

        $actualizado = $this->repositorio->buscarPorId($idUsuario);
        if ($actualizado === null) {
            return ResultadoAuth::fallo('datos_invalidos', 'No se ha encontrado ese usuario.');
        }

        return ResultadoAuth::acierto('Usuario actualizado correctamente.', $this->publico($actualizado), '');
    }

    /**
     * --------------------
     * token: Text, nombre: Text, apellidos: Text, email: Text, rol: Text
     * --> crearUsuarioAdmin() --> (ok, mensaje, password_temporal, usuario)
     * --------------------
     * Alta por admin. Genera contraseña temporal (no se elige a mano).
     *
     * @return array{ok: bool, codigo: string, mensaje: string, password_temporal: string, usuario: array{id_usuario: int, nombre: string, apellidos: string, email: string, rol: string, foto: string}}
     */
    public function crearUsuarioAdmin(
        string $token,
        string $nombre,
        string $apellidos,
        string $email,
        string $rol
    ): array {
        $vacio = [
            'ok' => false,
            'codigo' => 'datos_invalidos',
            'mensaje' => '',
            'password_temporal' => '',
            'usuario' => ResultadoAuth::usuarioVacio(),
        ];

        $admin = $this->exigirAdmin($token);
        if ($admin !== null) {
            $vacio['codigo'] = $admin->codigo;
            $vacio['mensaje'] = $admin->mensaje;

            return $vacio;
        }

        $nombre = trim($nombre);
        $apellidos = trim($apellidos);
        $email = strtolower(trim($email));
        $rol = strtolower(trim($rol));

        if ($nombre === '') {
            $vacio['mensaje'] = 'El nombre es obligatorio.';

            return $vacio;
        }
        if ($apellidos === '') {
            $vacio['mensaje'] = 'Los apellidos son obligatorios.';

            return $vacio;
        }
        if ($email === '' || !$this->emailValido($email)) {
            $vacio['mensaje'] = 'El correo electrónico no tiene un formato correcto.';

            return $vacio;
        }
        if ($rol !== 'admin' && $rol !== 'cliente') {
            $vacio['mensaje'] = 'El rol debe ser admin o cliente.';

            return $vacio;
        }

        $idRol = $this->repositorio->idRolPorNombre($rol);
        if ($idRol === null) {
            $vacio['mensaje'] = 'El rol indicado no existe.';

            return $vacio;
        }

        $password = $this->generarPasswordTemporal();
        $hash = password_hash($password, PASSWORD_BCRYPT);
        if ($hash === false) {
            $vacio['mensaje'] = 'No se ha podido generar la contraseña.';

            return $vacio;
        }

        try {
            $id = $this->repositorio->crear($nombre, $apellidos, $email, $hash, $idRol);
        } catch (CorreoYaRegistrado) {
            $vacio['codigo'] = 'correo_registrado';
            $vacio['mensaje'] = 'Ese correo ya está registrado.';

            return $vacio;
        }

        return [
            'ok' => true,
            'codigo' => 'ok',
            'mensaje' => 'Usuario creado. Copia la contraseña temporal ahora.',
            'password_temporal' => $password,
            'usuario' => [
                'id_usuario' => $id,
                'nombre' => $nombre,
                'apellidos' => $apellidos,
                'email' => $email,
                'rol' => $rol,
                'foto' => '',
            ],
        ];
    }

    /**
     * --------------------
     * token: Text, id_usuario: N --> borrarUsuarioAdmin() --> ResultadoAuth
     * --------------------
     * Borrado físico en BD. No puede borrarse a sí mismo.
     */
    public function borrarUsuarioAdmin(string $token, int $idUsuario): ResultadoAuth
    {
        $admin = $this->exigirAdmin($token);
        if ($admin !== null) {
            return $admin;
        }

        $datos = (new Token())->leer($token, $this->secreto, ($this->reloj)());
        $idAdmin = (int) ($datos['id_usuario'] ?? 0);
        if ($idUsuario === $idAdmin) {
            return ResultadoAuth::fallo('datos_invalidos', 'No puedes borrar tu propia cuenta desde aquí.');
        }

        $fila = $this->repositorio->buscarPorId($idUsuario);
        if ($fila === null) {
            return ResultadoAuth::fallo('datos_invalidos', 'No se ha encontrado ese usuario.');
        }

        try {
            $this->repositorio->eliminar($idUsuario);
        } catch (\RuntimeException $e) {
            return ResultadoAuth::fallo('datos_invalidos', $e->getMessage());
        }

        return ResultadoAuth::acierto('Usuario eliminado de la base de datos.', ResultadoAuth::usuarioVacio(), '');
    }

    /**
     * --------------------
     * token: Text, id_usuario: N --> generarPasswordAdmin() --> (ok, mensaje, password_temporal, usuario)
     * --------------------
     * Genera una contraseña nueva. Nunca expone la anterior (solo había un hash).
     *
     * @return array{ok: bool, codigo: string, mensaje: string, password_temporal: string, usuario: array{id_usuario: int, nombre: string, apellidos: string, email: string, rol: string, foto: string}}
     */
    public function generarPasswordAdmin(string $token, int $idUsuario): array
    {
        $admin = $this->exigirAdmin($token);
        if ($admin !== null) {
            return [
                'ok' => false,
                'codigo' => $admin->codigo,
                'mensaje' => $admin->mensaje,
                'password_temporal' => '',
                'usuario' => ResultadoAuth::usuarioVacio(),
            ];
        }

        $fila = $this->repositorio->buscarPorId($idUsuario);
        if ($fila === null) {
            return [
                'ok' => false,
                'codigo' => 'datos_invalidos',
                'mensaje' => 'No se ha encontrado ese usuario.',
                'password_temporal' => '',
                'usuario' => ResultadoAuth::usuarioVacio(),
            ];
        }

        $nueva = $this->generarPasswordTemporal();
        $hash = password_hash($nueva, PASSWORD_BCRYPT);
        if ($hash === false) {
            return [
                'ok' => false,
                'codigo' => 'datos_invalidos',
                'mensaje' => 'No se ha podido generar la contraseña.',
                'password_temporal' => '',
                'usuario' => ResultadoAuth::usuarioVacio(),
            ];
        }

        $this->repositorio->actualizarPassword($idUsuario, $hash);

        return [
            'ok' => true,
            'codigo' => 'ok',
            'mensaje' => 'Contraseña nueva generada. Cópiala ahora: no se volverá a mostrar.',
            'password_temporal' => $nueva,
            'usuario' => $this->publico($fila),
        ];
    }

    /** Cumple política: 8+, mayúscula, minúscula, número y signo. */
    private function generarPasswordTemporal(): string
    {
        $may = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
        $min = 'abcdefghijkmnpqrstuvwxyz';
        $num = '23456789';
        $esp = '!@#$%&*';
        $chars = [
            $may[random_int(0, strlen($may) - 1)],
            $min[random_int(0, strlen($min) - 1)],
            $num[random_int(0, strlen($num) - 1)],
            $esp[random_int(0, strlen($esp) - 1)],
        ];
        $todos = $may . $min . $num . $esp;
        for ($i = 0; $i < 6; $i++) {
            $chars[] = $todos[random_int(0, strlen($todos) - 1)];
        }
        shuffle($chars);

        return implode('', $chars);
    }

    private function exigirAdmin(string $token): ?ResultadoAuth
    {
        $sesion = $this->comprobarSesion($token);
        if (!$sesion->ok) {
            return $sesion;
        }
        if (($sesion->usuario['rol'] ?? '') !== 'admin') {
            return ResultadoAuth::fallo('sin_permiso', 'Solo un administrador puede hacer esto.');
        }

        return null;
    }

    private function emailValido(string $email): bool
    {
        if (strlen($email) > 254 || preg_match('/\s/', $email) === 1) {
            return false;
        }

        return preg_match('/^[^@]+@[^@]+\.[^@]+$/', $email) === 1;
    }

    /**
     * Mínimo 8, mayúscula, minúscula, número y signo especial. Máximo 72 (bcrypt).
     */
    private function errorPassword(string $password): ?string
    {
        if ($password === '') {
            return 'La contraseña es obligatoria.';
        }
        if (strlen($password) < self::MIN_PASSWORD) {
            return 'La contraseña debe tener al menos 8 caracteres.';
        }
        if (strlen($password) > self::MAX_PASSWORD) {
            return 'La contraseña no puede superar 72 caracteres.';
        }
        if (preg_match('/[A-ZÁÉÍÓÚÜÑ]/u', $password) !== 1) {
            return 'La contraseña debe incluir al menos una mayúscula.';
        }
        if (preg_match('/[a-záéíóúüñ]/u', $password) !== 1) {
            return 'La contraseña debe incluir al menos una minúscula.';
        }
        if (preg_match('/[0-9]/', $password) !== 1) {
            return 'La contraseña debe incluir al menos un número.';
        }
        if (preg_match('/[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9]/u', $password) !== 1) {
            return 'La contraseña debe incluir al menos un signo especial.';
        }

        return null;
    }

    /**
     * @param array{id_usuario: int, nombre: string, apellidos: string, email: string, rol: string, foto_perfil?: string} $fila
     * @return array{id_usuario: int, nombre: string, apellidos: string, email: string, rol: string, foto: string}
     */
    private function publico(array $fila): array
    {
        $ruta = trim((string) ($fila['foto_perfil'] ?? ''));

        return [
            'id_usuario' => $fila['id_usuario'],
            'nombre' => $fila['nombre'],
            'apellidos' => $fila['apellidos'],
            'email' => $fila['email'],
            'rol' => $fila['rol'],
            'foto' => $ruta === '' ? '' : '/perfiles/' . basename($ruta),
        ];
    }
}
