@echo off
setlocal EnableExtensions
chcp 65001 >nul

net session >nul 2>&1
if errorlevel 1 (
  echo Se necesitan permisos de administrador para configurar XAMPP...
  powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
  exit /b 0
)

set "XAMPP=C:\xampp"
set "HTDOCS=%XAMPP%\htdocs"
set "EXTRA=%XAMPP%\apache\conf\extra"
set "HTTPD=%XAMPP%\apache\conf\httpd.conf"
set "CONF=%EXTRA%\queaire.conf"
set "REPO=%~dp0"
if "%REPO:~-1%"=="\" set "REPO=%REPO:~0,-1%"

if not exist "%XAMPP%\apache\conf\httpd.conf" (
  echo No encuentro XAMPP en C:\xampp
  echo Si lo tienes en otra ruta, edita XAMPP en este .bat
  pause
  exit /b 1
)

if not exist "%REPO%\servidor\public\perfiles\" mkdir "%REPO%\servidor\public\perfiles"

:: Rutas Apache con barras /
set "REPO_A=%REPO:\=/%"
set "WEB=%REPO_A%/Pagina web"
set "API=%REPO_A%/servidor/public"
set "FOTOS=%REPO_A%/servidor/public/perfiles"

(
echo # QueAire — generado por activar-xampp.bat
echo Alias /queaire "%WEB%"
echo Alias /api "%API%"
echo Alias /perfiles "%FOTOS%"
echo.
echo ^<Directory "%WEB%"^>
echo     Options Indexes FollowSymLinks
echo     AllowOverride All
echo     Require all granted
echo ^</Directory^>
echo.
echo ^<Directory "%API%"^>
echo     Options FollowSymLinks
echo     AllowOverride All
echo     Require all granted
echo ^</Directory^>
echo.
echo ^<Directory "%FOTOS%"^>
echo     Options FollowSymLinks
echo     AllowOverride None
echo     Require all granted
echo ^</Directory^>
) > "%CONF%"

findstr /C:"queaire.conf" "%HTTPD%" >nul 2>&1
if errorlevel 1 (
  echo.>> "%HTTPD%"
  echo # QueAire>> "%HTTPD%"
  echo Include conf/extra/queaire.conf>> "%HTTPD%"
)

:: Enlaces en htdocs (por si Alias falla en alguna instalacion)
call :enlace "%HTDOCS%\queaire" "%REPO%\Pagina web"
call :enlace "%HTDOCS%\api" "%REPO%\servidor\public"
call :enlace "%HTDOCS%\perfiles" "%REPO%\servidor\public\perfiles"

echo.
echo Configurado: %CONF%
echo.
echo 1. En XAMPP: Stop Apache si estaba en marcha, luego Start Apache + MySQL
echo 2. Web: http://localhost/queaire/user.html
echo 3. API: http://localhost/api/
echo 4. BD:  http://localhost/phpmyadmin
echo.
pause
exit /b 0

:enlace
set "DEST=%~1"
set "ORIG=%~2"
if exist "%DEST%" rmdir "%DEST%" 2>nul
if exist "%DEST%\" (
  echo Aviso: "%DEST%" ya existe y no es enlace; se usa el Alias de Apache.
  exit /b 0
)
mklink /J "%DEST%" "%ORIG%" >nul 2>&1
if errorlevel 1 (
  echo Aviso: no se pudo enlazar "%DEST%" ^(no pasa nada, hay Alias^).
) else (
  echo OK enlace %DEST%
)
exit /b 0
