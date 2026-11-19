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

:: Junction ASCII al repo. Apache en Windows no resuelve bien rutas con
:: caracteres no ASCII (p. ej. "Biometría"): en UTF-8 arranca pero devuelve
:: 403 en todo, y en ANSI no arranca. Todos los Alias pasan por este atajo
:: sin acentos, asi el montaje funciona igual en cualquier PC del equipo.
set "ATAJO=%XAMPP%\queaire_repo"
if exist "%ATAJO%" rmdir "%ATAJO%" 2>nul
mklink /J "%ATAJO%" "%REPO%" >nul 2>&1
if errorlevel 1 (
  echo Aviso: no se pudo crear el enlace "%ATAJO%".
) else (
  echo OK enlace %ATAJO%
)

:: Rutas Apache con barras / (siempre a traves del atajo ASCII)
set "REPO_A=%ATAJO:\=/%"
set "WEB=%REPO_A%/Pagina web"
set "API=%REPO_A%/servidor/public"
set "FOTOS=%REPO_A%/servidor/public/perfiles"

(
echo # QueAire - generado por activar-xampp.bat
echo # Los Alias apuntan al junction ASCII %ATAJO%: no sustituir por la ruta
echo # real del repo si contiene acentos, porque Apache los resuelve mal.
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

:: Firewall: permitir que el movil (misma WiFi) llegue al Apache del puerto 80.
netsh advfirewall firewall show rule name="QueAire Apache 80" >nul 2>&1
if errorlevel 1 (
  netsh advfirewall firewall add rule name="QueAire Apache 80" dir=in action=allow protocol=TCP localport=80 >nul 2>&1
  if errorlevel 1 (
    echo Aviso: no se pudo anadir la regla de firewall del puerto 80.
  ) else (
    echo OK regla de firewall puerto 80
  )
) else (
  echo OK regla de firewall puerto 80 ^(ya existia^)
)

findstr /C:"queaire.conf" "%HTTPD%" >nul 2>&1
if errorlevel 1 (
  echo.>> "%HTTPD%"
  echo # QueAire>> "%HTTPD%"
  echo Include conf/extra/queaire.conf>> "%HTTPD%"
)

:: Enlaces en htdocs (por si Alias falla en alguna instalacion), tambien
:: a traves del atajo ASCII
call :enlace "%HTDOCS%\queaire" "%ATAJO%\Pagina web"
call :enlace "%HTDOCS%\api" "%ATAJO%\servidor\public"
call :enlace "%HTDOCS%\perfiles" "%ATAJO%\servidor\public\perfiles"

echo.
echo Configurado: %CONF%
echo.
echo 1. En XAMPP: Stop Apache si estaba en marcha, luego Start Apache + MySQL
echo 2. Web: http://localhost/queaire/user.html
echo 3. API: http://localhost/api/
echo 4. BD:  http://localhost/phpmyadmin
echo 5. Movil en la misma WiFi: en la app, servidor = http://IP_DEL_PC
echo    (la IP local del PC se ve con: ipconfig)
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
