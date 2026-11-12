package polimarket.componentes;

import java.util.List;

import polimarket.modelo.Bodega;
import polimarket.modelo.OrdenCompra;
import polimarket.modelo.Producto;
import polimarket.modelo.Ventas;

/** Interfaz provista por BodegaComponent. */
public interface IBodega {

    Bodega getBodega();

    List<Producto> listarInventario();

    Producto buscarProducto(int idProducto);

    int consultarStock(Producto p);

    List<Producto> listarProductosBajoMinimo();

    /** Emite una orden de compra al proveedor del producto. */
    OrdenCompra solicitarReabastecimiento(Producto p);

    List<OrdenCompra> listarOrdenesCompra();

    /** Recibe una orden EN_TRANSITO e ingresa las unidades a bodega. */
    OrdenCompra registrarRecepcion(int idOrden);

    OrdenCompra cancelarOrden(int idOrden);

    /** Emite órdenes para los productos de la venta que quedaron bajo el mínimo. */
    void revisarStockMinimo(Ventas venta);
}
