package polimarket.componentes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import polimarket.datos.Bitacora;
import polimarket.datos.RepositorioPoliMarket;
import polimarket.modelo.Cliente;
import polimarket.modelo.EstadoVenta;
import polimarket.modelo.Producto;
import polimarket.modelo.Vendedor;
import polimarket.modelo.Ventas;

/**
 * Componente de negocio de Ventas (provee IVentas; requiere IRecursosHumanos e IBodega).
 * RF2: Registrar ventas (listado de clientes y productos disponibles).
 */
public class VentasFacade implements IVentas {

    private final RepositorioPoliMarket repo = RepositorioPoliMarket.instancia();
    private final IRecursosHumanos recursosHumanos;
    private final IBodega bodega;

    public VentasFacade(IRecursosHumanos recursosHumanos, IBodega bodega) {
        this.recursosHumanos = recursosHumanos;
        this.bodega = bodega;
    }

    @Override
    public List<Cliente> listarClientes() {
        return repo.getClientes();
    }

    @Override
    public List<Producto> listarProductosDisponibles(Vendedor v) {
        return v.consultarProductosDisponibles();
    }

    /** Unidades de ventas CONFIRMADAS: vendidas pero aún no han salido de bodega. */
    private int cantidadComprometida(Producto p) {
        int total = 0;
        for (Ventas venta : listarVentas()) {
            if (venta.getEstadoVenta() == EstadoVenta.CONFIRMADA) {
                total += venta.getCantidadDe(p);
            }
        }
        return total;
    }

    @Override
    public int consultarDisponibilidad(Producto p) {
        return Math.max(0, bodega.consultarStock(p) - cantidadComprometida(p));
    }

    @Override
    public Ventas registrarVenta(Vendedor vendedor, Cliente cliente, Map<Producto, Integer> carrito) {
        if (!recursosHumanos.validarAutorizacionVigente(vendedor.getIdVendedor())) {
            throw new IllegalStateException("Recursos Humanos reporta que " + vendedor.getNombreCompleto()
                    + " no tiene autorización vigente (" + vendedor.getDescripcionAutorizacion() + ").");
        }
        if (carrito.isEmpty()) {
            throw new IllegalArgumentException("Agregue al menos un producto a la venta.");
        }
        // Se valida la disponibilidad antes de crear la venta para no dejar ventas incompletas.
        for (Map.Entry<Producto, Integer> item : carrito.entrySet()) {
            if (consultarDisponibilidad(item.getKey()) < item.getValue()) {
                throw new IllegalStateException("No hay disponibilidad suficiente de "
                        + item.getKey().getNombreProducto() + " (disponible: "
                        + consultarDisponibilidad(item.getKey()) + ", solicitado: " + item.getValue() + ").");
            }
        }
        Ventas venta = vendedor.gestionarVenta(cliente);
        for (Map.Entry<Producto, Integer> item : carrito.entrySet()) {
            if (!venta.consultarDisponibilidadBodega(bodega.getBodega(), item.getKey())) {
                venta.cancelarVenta();
                throw new IllegalStateException("La bodega no tiene existencias de " + item.getKey().getNombreProducto());
            }
            venta.agregarDetalle(item.getKey(), item.getValue());
        }
        venta.calcularTotales();
        venta.registrarVenta();
        Bitacora.registrar("Ventas", String.format("Venta #%d CONFIRMADA por %s para %s. Total $%,.0f",
                venta.getIdVenta(), vendedor.getNombreCompleto(), cliente.getNombreCompleto(), venta.getTotalVenta()));
        return venta;
    }

    @Override
    public void cancelarVenta(int idVenta) {
        Ventas v = buscarVenta(idVenta);
        if (repo.getEntregaDe(v) != null) {
            throw new IllegalStateException("La venta #" + idVenta + " ya tiene una entrega programada; no se puede anular.");
        }
        v.cancelarVenta();
        Bitacora.registrar("Ventas", "Venta #" + idVenta + " ANULADA.");
    }

    @Override
    public List<Ventas> listarVentas() {
        List<Ventas> todas = new ArrayList<>();
        for (Vendedor v : repo.getVendedores()) {
            todas.addAll(v.getVentas());
        }
        todas.sort((a, b) -> Integer.compare(a.getIdVenta(), b.getIdVenta()));
        return todas;
    }

    @Override
    public List<Ventas> obtenerVentasPendientesDeEntrega() {
        List<Ventas> pendientes = new ArrayList<>();
        for (Ventas v : listarVentas()) {
            if (v.getEstadoVenta() == EstadoVenta.CONFIRMADA && repo.getEntregaDe(v) == null) {
                pendientes.add(v);
            }
        }
        return pendientes;
    }

    @Override
    public Ventas buscarVenta(int idVenta) {
        for (Ventas v : listarVentas()) {
            if (v.getIdVenta() == idVenta) return v;
        }
        throw new IllegalArgumentException("No existe la venta #" + idVenta);
    }
}
