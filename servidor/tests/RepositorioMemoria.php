<?php

declare(strict_types=1);

use QueAire\Database\CorreoYaRegistrado;
use QueAire\Database\RepositorioUsuarios;

final class RepositorioMemoria implements RepositorioUsuarios
{
    /** @var array<int, array{id_usuario: int, nombre: string, apellidos: string, email: string, password_hash: string, foto_perfil: string, activo: bool, rol: string, id_rol: int, ultimo_acceso: ?string}> */
    public array $usuarios = [];

    /** @var array<string, int> */
    public array $roles = ['admin' => 1, 'cliente' => 3];

    private int $siguiente = 1;

    public function buscarPorEmail(string $email): ?array
    {
        foreach ($this->usuarios as $usuario) {
            if (strtolower($usuario['email']) === strtolower($email)) {
                return $this->fila($usuario);
            }
        }

        return null;
    }

    public function buscarPorId(int $idUsuario): ?array
    {
        if (!isset($this->usuarios[$idUsuario])) {
            return null;
        }

        return $this->fila($this->usuarios[$idUsuario]);
    }

    public function idRolPorNombre(string $nombreRol): ?int
    {
        return $this->roles[$nombreRol] ?? null;
    }

    public function crear(
        string $nombre,
        string $apellidos,
        string $email,
        string $passwordHash,
        int $idRol
    ): int {
        if ($this->buscarPorEmail($email) !== null) {
            throw new CorreoYaRegistrado();
        }

        $id = $this->siguiente++;
        $rol = array_search($idRol, $this->roles, true);
        $this->usuarios[$id] = [
            'id_usuario' => $id,
            'nombre' => $nombre,
            'apellidos' => $apellidos,
            'email' => $email,
            'password_hash' => $passwordHash,
            'foto_perfil' => '',
            'activo' => true,
            'rol' => $rol === false ? '' : (string) $rol,
            'id_rol' => $idRol,
            'ultimo_acceso' => null,
        ];

        return $id;
    }

    public function registrarAcceso(int $idUsuario, string $fechaUtc): void
    {
        if (isset($this->usuarios[$idUsuario])) {
            $this->usuarios[$idUsuario]['ultimo_acceso'] = $fechaUtc;
        }
    }

    public function actualizarPerfil(
        int $idUsuario,
        string $nombre,
        string $apellidos,
        string $email,
        ?string $fotoPerfil,
        ?string $passwordHash = null
    ): void {
        if (!isset($this->usuarios[$idUsuario])) {
            return;
        }
        foreach ($this->usuarios as $otro) {
            if ($otro['id_usuario'] !== $idUsuario && strtolower($otro['email']) === strtolower($email)) {
                throw new CorreoYaRegistrado();
            }
        }
        $this->usuarios[$idUsuario]['nombre'] = $nombre;
        $this->usuarios[$idUsuario]['apellidos'] = $apellidos;
        $this->usuarios[$idUsuario]['email'] = $email;
        if ($fotoPerfil !== null) {
            $this->usuarios[$idUsuario]['foto_perfil'] = $fotoPerfil;
        }
        if ($passwordHash !== null) {
            $this->usuarios[$idUsuario]['password_hash'] = $passwordHash;
        }
    }

    public function desactivar(int $idUsuario): void
    {
        if (isset($this->usuarios[$idUsuario])) {
            $this->usuarios[$idUsuario]['activo'] = false;
        }
    }

    public function eliminar(int $idUsuario): void
    {
        unset($this->usuarios[$idUsuario]);
    }

    public function listarTodos(): array
    {
        $salida = [];
        foreach ($this->usuarios as $usuario) {
            $n = $this->fila($usuario);
            unset($n['password_hash']);
            $salida[] = $n;
        }
        usort($salida, static fn (array $a, array $b): int => $a['id_usuario'] <=> $b['id_usuario']);

        return $salida;
    }

    public function actualizarPorAdmin(
        int $idUsuario,
        string $nombre,
        string $apellidos,
        string $email,
        bool $activo,
        int $idRol
    ): void {
        if (!isset($this->usuarios[$idUsuario])) {
            return;
        }
        foreach ($this->usuarios as $otro) {
            if ($otro['id_usuario'] !== $idUsuario && strtolower($otro['email']) === strtolower($email)) {
                throw new CorreoYaRegistrado();
            }
        }
        $rol = 'cliente';
        foreach ($this->roles as $nombreRol => $id) {
            if ($id === $idRol) {
                $rol = $nombreRol;
                break;
            }
        }
        $this->usuarios[$idUsuario]['nombre'] = $nombre;
        $this->usuarios[$idUsuario]['apellidos'] = $apellidos;
        $this->usuarios[$idUsuario]['email'] = $email;
        $this->usuarios[$idUsuario]['activo'] = $activo;
        $this->usuarios[$idUsuario]['id_rol'] = $idRol;
        $this->usuarios[$idUsuario]['rol'] = $rol;
    }

    public function actualizarPassword(int $idUsuario, string $passwordHash): void
    {
        if (isset($this->usuarios[$idUsuario])) {
            $this->usuarios[$idUsuario]['password_hash'] = $passwordHash;
        }
    }

    /**
     * @param array{id_usuario: int, nombre: string, apellidos: string, email: string, password_hash: string, foto_perfil?: string, activo: bool, rol: string} $usuario
     * @return array{id_usuario: int, nombre: string, apellidos: string, email: string, password_hash: string, foto_perfil: string, activo: bool, rol: string}
     */
    private function fila(array $usuario): array
    {
        return [
            'id_usuario' => $usuario['id_usuario'],
            'nombre' => $usuario['nombre'],
            'apellidos' => $usuario['apellidos'],
            'email' => $usuario['email'],
            'password_hash' => $usuario['password_hash'],
            'foto_perfil' => (string) ($usuario['foto_perfil'] ?? ''),
            'activo' => $usuario['activo'],
            'rol' => $usuario['rol'],
        ];
    }
}
