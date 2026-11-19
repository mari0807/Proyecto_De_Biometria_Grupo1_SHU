<?php

declare(strict_types=1);

namespace QueAire\Database;

use PDO;
use PDOException;

final class UsuarioRepositorio implements RepositorioUsuarios
{
    public function __construct(private PDO $pdo)
    {
    }

    public function buscarPorEmail(string $email): ?array
    {
        $stmt = $this->pdo->prepare(
            'SELECT u.id_usuario, u.nombre, u.apellidos, u.email, u.password_hash,
                    u.foto_perfil, u.activo, r.nombre AS rol
             FROM USUARIO u
             JOIN ROLES r ON r.id_rol = u.id_rol
             WHERE u.email = ?
             LIMIT 1'
        );
        $stmt->execute([$email]);
        $fila = $stmt->fetch();

        return $fila === false ? null : $this->normalizar($fila);
    }

    public function buscarPorId(int $idUsuario): ?array
    {
        $stmt = $this->pdo->prepare(
            'SELECT u.id_usuario, u.nombre, u.apellidos, u.email, u.password_hash,
                    u.foto_perfil, u.activo, r.nombre AS rol
             FROM USUARIO u
             JOIN ROLES r ON r.id_rol = u.id_rol
             WHERE u.id_usuario = ?
             LIMIT 1'
        );
        $stmt->execute([$idUsuario]);
        $fila = $stmt->fetch();

        return $fila === false ? null : $this->normalizar($fila);
    }

    public function idRolPorNombre(string $nombreRol): ?int
    {
        $stmt = $this->pdo->prepare('SELECT id_rol FROM ROLES WHERE nombre = ? LIMIT 1');
        $stmt->execute([$nombreRol]);
        $id = $stmt->fetchColumn();

        return $id === false ? null : (int) $id;
    }

    public function crear(
        string $nombre,
        string $apellidos,
        string $email,
        string $passwordHash,
        int $idRol
    ): int {
        $this->pdo->beginTransaction();
        try {
            $stmt = $this->pdo->prepare(
                'INSERT INTO USUARIO (nombre, apellidos, email, password_hash, id_rol, activo)
                 VALUES (?, ?, ?, ?, ?, TRUE)'
            );
            $stmt->execute([$nombre, $apellidos, $email, $passwordHash, $idRol]);
            $id = (int) $this->pdo->lastInsertId();
            $this->pdo->commit();

            return $id;
        } catch (PDOException $e) {
            if ($this->pdo->inTransaction()) {
                $this->pdo->rollBack();
            }
            if ((int) ($e->errorInfo[1] ?? 0) === 1062) {
                throw new CorreoYaRegistrado();
            }
            throw $e;
        }
    }

    public function registrarAcceso(int $idUsuario, string $fechaUtc): void
    {
        $stmt = $this->pdo->prepare(
            'UPDATE USUARIO SET ultimo_acceso = ? WHERE id_usuario = ?'
        );
        $stmt->execute([$fechaUtc, $idUsuario]);
    }

    public function actualizarPerfil(
        int $idUsuario,
        string $nombre,
        string $apellidos,
        string $email,
        ?string $fotoPerfil,
        ?string $passwordHash = null
    ): void {
        $this->pdo->beginTransaction();
        try {
            $sets = ['nombre = ?', 'apellidos = ?', 'email = ?'];
            $valores = [$nombre, $apellidos, $email];
            if ($fotoPerfil !== null) {
                $sets[] = 'foto_perfil = ?';
                $valores[] = $fotoPerfil;
            }
            if ($passwordHash !== null) {
                $sets[] = 'password_hash = ?';
                $valores[] = $passwordHash;
            }
            $valores[] = $idUsuario;
            $sql = 'UPDATE USUARIO SET ' . implode(', ', $sets) . ' WHERE id_usuario = ?';
            $stmt = $this->pdo->prepare($sql);
            $stmt->execute($valores);
            $this->pdo->commit();
        } catch (PDOException $e) {
            if ($this->pdo->inTransaction()) {
                $this->pdo->rollBack();
            }
            if ((int) ($e->errorInfo[1] ?? 0) === 1062) {
                throw new CorreoYaRegistrado();
            }
            throw $e;
        }
    }

    public function desactivar(int $idUsuario): void
    {
        $stmt = $this->pdo->prepare('UPDATE USUARIO SET activo = FALSE WHERE id_usuario = ?');
        $stmt->execute([$idUsuario]);
    }

    public function eliminar(int $idUsuario): void
    {
        try {
            $stmt = $this->pdo->prepare('DELETE FROM USUARIO WHERE id_usuario = ?');
            $stmt->execute([$idUsuario]);
        } catch (PDOException $e) {
            if ((int) ($e->errorInfo[1] ?? 0) === 1451) {
                throw new \RuntimeException(
                    'No se puede borrar: el usuario tiene datos vinculados (p. ej. nodos).',
                    0,
                    $e
                );
            }
            throw $e;
        }
    }

    public function listarTodos(): array
    {
        $stmt = $this->pdo->query(
            'SELECT u.id_usuario, u.nombre, u.apellidos, u.email, u.password_hash,
                    u.foto_perfil, u.activo, r.nombre AS rol
             FROM USUARIO u
             JOIN ROLES r ON r.id_rol = u.id_rol
             ORDER BY u.id_usuario ASC'
        );
        $filas = $stmt === false ? [] : $stmt->fetchAll();
        $salida = [];
        foreach ($filas as $fila) {
            $n = $this->normalizar($fila);
            unset($n['password_hash']);
            $salida[] = $n;
        }

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
        $this->pdo->beginTransaction();
        try {
            $stmt = $this->pdo->prepare(
                'UPDATE USUARIO
                 SET nombre = ?, apellidos = ?, email = ?, activo = ?, id_rol = ?
                 WHERE id_usuario = ?'
            );
            $stmt->execute([$nombre, $apellidos, $email, $activo ? 1 : 0, $idRol, $idUsuario]);
            $this->pdo->commit();
        } catch (PDOException $e) {
            if ($this->pdo->inTransaction()) {
                $this->pdo->rollBack();
            }
            if ((int) ($e->errorInfo[1] ?? 0) === 1062) {
                throw new CorreoYaRegistrado();
            }
            throw $e;
        }
    }

    public function actualizarPassword(int $idUsuario, string $passwordHash): void
    {
        $stmt = $this->pdo->prepare('UPDATE USUARIO SET password_hash = ? WHERE id_usuario = ?');
        $stmt->execute([$passwordHash, $idUsuario]);
    }

    /**
     * @param array<string, mixed> $fila
     * @return array{
     *   id_usuario: int,
     *   nombre: string,
     *   apellidos: string,
     *   email: string,
     *   password_hash: string,
     *   foto_perfil: string,
     *   activo: bool,
     *   rol: string
     * }
     */
    private function normalizar(array $fila): array
    {
        return [
            'id_usuario' => (int) $fila['id_usuario'],
            'nombre' => (string) $fila['nombre'],
            'apellidos' => (string) $fila['apellidos'],
            'email' => (string) $fila['email'],
            'password_hash' => (string) $fila['password_hash'],
            'foto_perfil' => (string) ($fila['foto_perfil'] ?? ''),
            'activo' => (bool) $fila['activo'],
            'rol' => (string) $fila['rol'],
        ];
    }
}
