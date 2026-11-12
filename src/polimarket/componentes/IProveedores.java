package polimarket.componentes;

import java.util.List;

import polimarket.modelo.OrdenCompra;
import polimarket.modelo.Producto;
import polimarket.modelo.Proveedor;

/** Interfaz provista por ProveedoresComponent. */
public interface IProveedores {

    List<Proveedor> listarProveedores();

    /** A qué empresa solicitar más unidades cuando se terminen las existencias. */
    Proveedor obtenerProveedorDe(Producto p);

    /** El proveedor despacha la orden: EMITIDA -> EN_TRANSITO y emite su factura. */
    OrdenCompra despacharOrden(int idOrden);

    List<String> consultarHistorialCompras(Proveedor pr);
}
