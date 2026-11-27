@echo off
REM Prueba de conexion por consola con los datos de config.properties.
cd /d "%~dp0"
java -cp ProformaTaller.jar proformas.conexion.PruebaConexion
pause
