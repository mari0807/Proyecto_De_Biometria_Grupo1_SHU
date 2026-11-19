<?php

declare(strict_types=1);

namespace QueAire\Database;

interface RepositorioUsuarios
{
    /**
     * @return array{
     *   id_usuario: int,
     *   nombre: string,
     *   apellidos: string,
     *   email: string,
     *   password_hash: string,
     *   foto_perfil: string,
     *   activo: bool,
     *   rol: string
     * }|null
     */
    public function buscarPorEmail(string $email): ?array;

    /**
     * @return array{
     *   id_usuario: int,
     *   nombre: string,
     *   apellidos: string,
     *   email: string,
     *   password_hash: string,
     *   foto_perfil: string,
     *   activo: bool,
     *   rol: string
     * }|null
     */
    public function buscarPorId(int $idUsuario): ?array;

    public function idRolPorNombre(string $nombreRol): ?int;

    /**
     * @throws CorreoYaRegistrado
     */
    public function crear(
        string $nombre,
        string $apellidos,
        string $email,
        string $passwordHash,
        int $idRol
    ): int;

    public function registrarAcceso(int $idUsuario, string $fechaUtc): void;

    /**
     * @throws CorreoYaRegistrado
     */
    public function actualizarPerfil(
        int $idUsuario,
        string $nombre,
        string $apellidos,
        string $email,
        ?string $fotoPerfil,
        ?string $passwordHash = null
    ): void;

    /** Baja lógica: activo = FALSE. */
    public function desactivar(int $idUsuario): void;

    /** Borrado físico de la fila USUARIO. */
    public function eliminar(int $idUsuario): void;

    /**
     * @return list<array{
     *   id_usuario: int,
     *   nombre: string,
     *   apellidos: string,
     *   email: string,
     *   foto_perfil: string,
     *   activo: bool,
     *   rol: string
     * }>
     */
    public function listarTodos(): array;

    /**
     * @throws CorreoYaRegistrado
     */
    public function actualizarPorAdmin(
        int $idUsuario,
        string $nombre,
        string $apellidos,
        string $email,
        bool $activo,
        int $idRol
    ): void;

    public function actualizarPassword(int $idUsuario, string $passwordHash): void;
}
