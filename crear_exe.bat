@echo off
REM Genera el ejecutable portable de Windows con jpackage (incluido en el JDK 14 o superior).
REM Resultado: dist_exe\ProformaTaller\ProformaTaller.exe + app\ + runtime\ (Java incluido).
REM Antes se debe compilar con build.bat para tener dist\ProformaTaller.jar actualizado.
setlocal
cd /d "%~dp0"
if not "%~1"=="" (set "DEST=%~1") else (set "DEST=%~dp0dist_exe")
set "ENTRADA=%TEMP%\proformataller_exe_entrada"
set "MODULOS=java.base,java.desktop,java.logging,java.management,java.naming,java.security.jgss,java.sql,java.sql.rowset,jdk.localedata"

set "JPACKAGE=jpackage"
where jpackage >nul 2>nul
if errorlevel 1 (
    if exist "%JAVA_HOME%\bin\jpackage.exe" (
        set "JPACKAGE=%JAVA_HOME%\bin\jpackage.exe"
    ) else (
        echo No se encontro jpackage. Instale un JDK 14 o superior y agreguelo al PATH.
        goto fin
    )
)
if not exist "dist\ProformaTaller.jar" (
    echo Falta dist\ProformaTaller.jar. Ejecute primero build.bat.
    goto fin
)

if exist "%ENTRADA%" rmdir /s /q "%ENTRADA%"
mkdir "%ENTRADA%"
copy /y "dist\ProformaTaller.jar" "%ENTRADA%\" >nul
if not exist "%DEST%" mkdir "%DEST%"
if exist "%DEST%\ProformaTaller" rmdir /s /q "%DEST%\ProformaTaller"

echo Generando ProformaTaller.exe, espere un momento...
"%JPACKAGE%" --type app-image ^
  --name ProformaTaller ^
  --input "%ENTRADA%" ^
  --main-jar ProformaTaller.jar ^
  --main-class proformas.Principal ^
  --icon "%~dp0recursos_exe\ProformaTaller.ico" ^
  --app-version 1.0 ^
  --vendor "ProformaTaller" ^
  --description "ProformaTaller - Sistema de Proformas para Talleres" ^
  --add-modules %MODULOS% ^
  --jlink-options "--strip-debug --no-header-files --no-man-pages --include-locales=es,en" ^
  --dest "%DEST%"
if errorlevel 1 (
    echo.
    echo Ocurrio un error al generar el .exe.
    goto fin
)
rmdir /s /q "%ENTRADA%"
echo.
echo Listo: %DEST%\ProformaTaller\ProformaTaller.exe
:fin
echo.
pause
