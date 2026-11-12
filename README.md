# PoliMarket – Actividad Sumativa U2 (Punto 2: construcción del sistema)

Implementación en **Java** del sistema PoliMarket, fiel a `DiagramaClases.wsd` y `DiagramaComponentes.wsd`.

## Estructura

```
src/polimarket/
├── Main.java                       Lanza los dos clientes
├── modelo/                         Diagrama de clases
│   RecursosHumanos, Vendedor, Ventas, DetalleVenta, Cliente, Producto, Bodega,
│   Proveedor, OrdenCompra, DetalleOrdenCompra, Entregas
│   EstadoVenta, EstadoEntrega, EstadoOrden (enumeraciones)
├── componentes/                    Diagrama de componentes – Capa de Negocio
│   Interfaces provistas: IRecursosHumanos, IVentas, IBodega, IProveedores, IEntregas
│   Componentes: RecursosHumanosFacade, VentasFacade, BodegaComponent,
│                ProveedoresComponent, EntregasComponent
│   PoliMarketComponentes: ensambla los componentes a través de sus interfaces
├── datos/                          Capa de Datos
│   RepositorioPoliMarket (datos de ejemplo en memoria), Bitacora (registro de eventos)
└── clientes/                       Capa de Presentación / Clientes
    ├── escritorio/ModuloVendedoresApp.java  CLIENTE 1: escritorio, Java Swing
    └── web/SistemaEntregasLogistica.java    CLIENTE 2: web, HTML + JavaScript (index.html)
                                             vía API HTTP/JSON
```

## Interfaces consumidas por cada cliente

| Cliente | Interfaces | Uso |
|---------|------------|-----|
| ModuloVendedoresApp (escritorio) | IRecursosHumanos | Autorizar, revocar o renovar vendedores; validar la autorización vigente al iniciar sesión |
| | IVentas | Clientes, productos disponibles, disponibilidad, registrar y anular ventas |
| | IBodega | Pestaña Inventario: stock y productos bajo el mínimo |
| SistemaEntregasLogistica (web) | IEntregas | Ventas por entregar, programar, despachar y marcar entregada o fallida |
| | IBodega | Inventario, órdenes de compra, registrar recepción y cancelar |
| | IProveedores | Proveedor de cada producto; el proveedor despacha la orden |
| | IRecursosHumanos | Vigencia de la autorización del vendedor de cada venta; listado de personal (solo lectura) |

## Requisitos funcionales

| RF | Descripción | Componente |
|----|-------------|------------|
| RF1 | Autorizar vendedores por 1 año y validar su vigencia | RecursosHumanosFacade |
| RF2 | Registrar ventas con sus líneas de detalle | VentasFacade (valida con IRecursosHumanos e IBodega) |
| RF3 | Consultar la disponibilidad de productos | BodegaComponent |
| RF4 | Emitir y recibir órdenes de compra cuando el stock baja del mínimo | BodegaComponent → ProveedoresComponent |
| RF5 | Programar entregas, registrar la salida de bodega y facturar | EntregasComponent → VentasFacade, BodegaComponent |

## Ciclos de vida

- **Venta:** BORRADOR → CONFIRMADA (el vendedor la registra; sus unidades quedan comprometidas) →
  FACTURADA (Entregas registra la salida de bodega y se asigna el número `FV-…`). Se puede ANULAR
  mientras esté en BORRADOR o CONFIRMADA y sin entrega programada.
- **Entrega:** PROGRAMADA (guía `GU-…`) → EN_RUTA (salida de bodega) → ENTREGADA o FALLIDA.
- **Orden de compra:** EMITIDA (`OC-…`) → EN_TRANSITO (el proveedor despacha y factura) → RECIBIDA
  (el stock ingresa a bodega). Una orden EMITIDA se puede CANCELAR.
- **Autorización de un vendedor:** dura 1 año desde que RH la otorga. Una autorización vencida
  impide vender.

## Cómo ejecutar

Requisito: **JDK 17 o superior** (`java -version` y `javac -version`).

**Linux / macOS**
```bash
cd codigoActividadSumativa
./ejecutar.sh
```

**Windows**: doble clic en `ejecutar.bat`, o desde una consola en la carpeta: `ejecutar.bat`.

**Manual (Linux / macOS)**
```bash
cd codigoActividadSumativa
mkdir bin
javac -encoding UTF-8 -d bin $(find src -name "*.java")
cp src/polimarket/clientes/web/index.html bin/polimarket/clientes/web/
java -cp bin polimarket.Main
```

Se abre la ventana de escritorio y el cliente web queda en **http://localhost:8080** (botón
"Abrir cliente web de Entregas"). Si el puerto 8080 está ocupado se usa el siguiente libre; la URL
aparece en la consola.

## Guion de demostración

1. **Recursos Humanos** (escritorio): Carlos Pérez tiene la autorización vigente, Ana Martínez no
   está autorizada y Jorge Ramírez la tiene **vencida**. Intente iniciar sesión con Jorge: el
   sistema lo impide. Selecciónelo y pulse **Autorizar / renovar**.
2. Inicie sesión y, en **Registrar venta**, agregue productos y pulse **Registrar venta**. La venta
   queda CONFIRMADA. Por ejemplo, venda 15 unidades de "Agua 600 ml" para provocar luego una orden
   de compra.
3. **Web → Entregas:** la venta aparece con su vendedor y la vigencia de su autorización. Programe
   la entrega, pulse **Despachar** (la venta queda FACTURADA) y luego **Marcar entregada** o **Fallida**.
4. **Web → Inventario y órdenes de compra:** aparece la orden EMITIDA para el producto que quedó bajo
   el mínimo. Pulse **Proveedor despacha** y luego **Registrar recepción**: el stock aumenta.
5. **Web → Personal:** listado de solo lectura con el estado de autorización de cada vendedor.
6. En el escritorio, el **Historial** muestra el número de factura y el estado, el **Inventario**
   refleja el stock y la **Bitácora** registra todos los eventos.

Los datos se guardan en memoria: al cerrar la aplicación se reinician.
