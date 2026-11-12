#!/usr/bin/env bash
# Compila y ejecuta PoliMarket (Linux / macOS). Requiere JDK 17 o superior.
set -e
cd "$(dirname "$0")"
rm -rf bin
mkdir -p bin
javac -encoding UTF-8 --release 17 -d bin $(find src -name "*.java")
cp src/polimarket/clientes/web/index.html bin/polimarket/clientes/web/
echo "Compilación correcta. Iniciando PoliMarket..."
java -cp bin polimarket.Main
