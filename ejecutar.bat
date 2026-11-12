@echo off
REM Compila y ejecuta PoliMarket (Windows). Requiere JDK 17 o superior.
cd /d "%~dp0"
if exist bin rmdir /s /q bin
mkdir bin
dir /s /b src\*.java > fuentes.txt
javac -encoding UTF-8 --release 17 -d bin @fuentes.txt
if errorlevel 1 (
  del fuentes.txt
  echo Error de compilacion.
  pause
  exit /b 1
)
del fuentes.txt
copy src\polimarket\clientes\web\index.html bin\polimarket\clientes\web\ > nul
echo Compilacion correcta. Iniciando PoliMarket...
java -cp bin polimarket.Main
