package polimarket.componentes;

import java.util.List;
import java.util.Map;

import polimarket.modelo.Cliente;
import polimarket.modelo.Producto;
import polimarket.modelo.Vendedor;
import polimarket.modelo.Ventas;

/** Interfaz provista por VentasFacade. */
public interface IVentas {

    List<Cliente> listarClientes();

    List<Producto> listarProductosDisponibles(Vendedor v);

    /** Stock en bodega menos las unidades de ventas CONFIRMADAS que aún no salen. */
    int consultarDisponibilidad(Producto p);

    Ventas registrarVenta(Vendedor vendedor, Cliente cliente, Map<Producto, Integer> carrito);

    void cancelarVenta(int idVenta);

    List<Ventas> listarVentas();

    /** Ventas CONFIRMADAS que todavía no tienen entrega programada. */
    List<Ventas> obtenerVentasPendientesDeEntrega();

    Ventas buscarVenta(int idVenta);
}
