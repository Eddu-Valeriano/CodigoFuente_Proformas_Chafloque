#!/bin/bash
# Compila el aplicativo y genera dist/ProformaTaller.jar (requiere JDK 8 o superior).
set -e
cd "$(dirname "$0")"
rm -rf build && mkdir -p build/classes dist
find src -name "*.java" > build/fuentes.txt
javac --release 8 -encoding UTF-8 -Xlint:-options -d build/classes @build/fuentes.txt
cp -r src/proformas/recursos build/classes/proformas/
# Se incluye el controlador JDBC dentro del .jar para que sea un solo ejecutable
(cd build/classes && jar xf ../../lib/mariadb-java-client-2.7.6.jar && rm -rf META-INF/MANIFEST.MF META-INF/*.SF META-INF/*.RSA META-INF/*.DSA)
jar cfe dist/ProformaTaller.jar proformas.Principal -C build/classes .
cp -n config.properties dist/ 2>/dev/null || true
echo "Listo: dist/ProformaTaller.jar"
