# ProformaTaller — Sistema de Proformas para Talleres

Aplicativo de escritorio que consume la base de datos `proformataller` (MySQL 8.0) para generar
proformas de servicios y repuestos. Proyecto del curso de Gestión de Datos e Información (UCSM).

- **Lenguaje:** Java SE 8 o superior (Swing para la interfaz y JDBC para la base de datos).
- **Sin frameworks:** solo la biblioteca estándar de Java y el controlador JDBC.
- **Base de datos:** MySQL 8.0 con InnoDB. Todas las operaciones se hacen con **procedimientos almacenados**;
  los **triggers** validan los datos, calculan el total de la proforma y registran la auditoría.

## Estructura

```
ProformaTaller/
├── sql/                     Scripts de la base de datos
│   ├── 00_script_completo.sql   (01 + 02 + 03 en un solo archivo)
│   ├── 01_creacion_bd.sql       tablas, índices, tabla de auditoría y usuario del aplicativo
│   ├── 02_programacion_bd.sql   31 triggers y 38 procedimientos almacenados
│   ├── 03_datos_prueba.sql      datos para probar la funcionalidad (20 proformas)
│   └── 04_consultas.sql         las 12 consultas del informe en SQL directo
├── src/proformas/           Código fuente documentado
│   ├── Principal.java           punto de entrada
│   ├── conexion/                ConexionBD, ConfiguracionBD, PruebaConexion
│   ├── modelo/                  Cliente, Mecanico, ItemCatalogo, Proforma, DetalleProforma, Reporte
│   ├── dao/                     acceso a datos: llamadas a los procedimientos (CallableStatement)
│   ├── vista/                   ventanas y paneles Swing
│   ├── util/                    estilo, fechas, mensajes y tablas
│   └── recursos/                logo e ícono del sistema
├── lib/                     Controlador JDBC (MariaDB Connector/J 2.7.6, compatible con MySQL 8)
├── dist/                    Ejecutable: ProformaTaller.jar + config.properties + .bat
├── docs/capturas/           Evidencias de las pruebas
├── recursos_exe/            Ícono para el ejecutable de Windows
├── build.bat / build.sh     Compilación desde el código fuente
├── crear_exe.bat            Genera ProformaTaller.exe (portable, con su propio Java) con jpackage
└── config.properties        Datos de conexión por defecto
```

## Instalación y uso

1. **Crear la base de datos** (como `root`). En MySQL Workbench abra y ejecute `sql/00_script_completo.sql`,
   o desde la consola:
   ```
   mysql -u root -p < sql/00_script_completo.sql
   ```
   El script crea el usuario del aplicativo `proformataller_app` (clave `ProformaTaller.2026`), que solo puede
   **ejecutar procedimientos**.
2. **Ejecutar el aplicativo:** doble clic en `dist/ejecutar.bat` o `java -jar dist/ProformaTaller.jar`.
3. En la ventana de ingreso escriba el usuario `proformataller_app` y la contraseña `ProformaTaller.2026`
   y pulse **Ingresar**. El servidor (localhost, 3306, `proformataller`) viene por defecto y se cambia con el
   botón de la tuerca.
4. Prueba por consola (opcional): `dist/prueba_conexion.bat`.

## Compilar desde el código fuente

Con el JDK instalado: `build.bat` (Windows) o `./build.sh` (Linux). Se genera `dist/ProformaTaller.jar`
con el controlador JDBC incluido.

> El código no depende del controlador: si se prefiere el MySQL Connector/J oficial, basta con colocar su
> `.jar` en `lib/` y ajustar el nombre en el script de compilación.

## Generar el ejecutable de Windows (.exe)

Con un JDK 14 o superior en Windows, primero compile (`build.bat`) y luego ejecute `crear_exe.bat`.
Se genera la carpeta `dist_exe/ProformaTaller/` con `ProformaTaller.exe` y las subcarpetas `app/` y `runtime/`
(un Java reducido incluido). Esa carpeta se copia completa a cualquier PC con Windows de 64 bits y funciona
sin instalar Java; solo se necesita MySQL para la base de datos.

## Módulos

| Módulo | Qué hace | Procedimientos |
|---|---|---|
| Inicio | Indicadores y últimas proformas | `sp_resumen_general`, `sp_proforma_listar` |
| Proformas | Registrar, modificar, eliminar y ver el detalle | `sp_proforma_registrar`, `sp_proforma_actualizar`, `sp_proforma_eliminar`, `sp_proforma_obtener`, `sp_proforma_detalle`, `sp_proforma_listar` |
| Clientes | CRUD de clientes | `sp_cliente_*` |
| Mecánicos | CRUD de mecánicos | `sp_mecanico_*` |
| Servicios / Repuestos | CRUD de los catálogos | `sp_servicio_*`, `sp_repuesto_*` |
| Reportes | 12 reportes (8 con JOIN), exportables a CSV | `sp_rep01` … `sp_rep12` |
| Auditoría | Movimientos registrados por los triggers | `sp_auditoria_listar` |
