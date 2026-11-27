@echo off
REM Compila el aplicativo y genera dist\ProformaTaller.jar (requiere JDK 8 o superior en el PATH).
cd /d "%~dp0"
if exist build rmdir /s /q build
mkdir build\classes
if not exist dist mkdir dist
dir /s /b src\*.java > build\fuentes.txt
javac -encoding UTF-8 -source 8 -target 8 -d build\classes @build\fuentes.txt || goto error
xcopy /e /i /q src\proformas\recursos build\classes\proformas\recursos > nul
cd build\classes
jar xf ..\..\lib\mariadb-java-client-2.7.6.jar
del /q META-INF\MANIFEST.MF 2> nul
cd ..\..
jar cfe dist\ProformaTaller.jar proformas.Principal -C build\classes .
if not exist dist\config.properties copy config.properties dist\ > nul
echo Listo: dist\ProformaTaller.jar
goto fin
:error
echo Error de compilacion.
:fin
pause
