<?php

declare(strict_types=1);

$secreto = getenv('QUEAIRE_AUTH_SECRETO');
$password = getenv('QUEAIRE_DB_PASSWORD');

return [
    'db' => [
        'host' => getenv('QUEAIRE_DB_HOST') ?: '127.0.0.1',
        'puerto' => getenv('QUEAIRE_DB_PORT') ?: '3306',
        'nombre' => getenv('QUEAIRE_DB_NAME') ?: 'QueAire_Biometria',
        'usuario' => getenv('QUEAIRE_DB_USER') ?: 'root',
        'password' => $password === false ? '' : $password,
    ],
    'auth_secreto' => ($secreto === false || $secreto === '')
        ? 'queaire-dev-solo-local'
        : $secreto,
    'token_ttl' => 600,
];
