package polimarket.componentes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import polimarket.datos.Bitacora;
import polimarket.datos.RepositorioPoliMarket;
import polimarket.modelo.Bodega;
import polimarket.modelo.DetalleOrdenCompra;
import polimarket.modelo.DetalleVenta;
import polimarket.modelo.EstadoOrden;
import polimarket.modelo.OrdenCompra;
import polimarket.modelo.Producto;
import polimarket.modelo.Proveedor;
import polimarket.modelo.Ventas;

/**
 * Componente de negocio de Bodega (provee IBodega; requiere IProveedores).
 * RF3: Consultar la disponibilidad de productos.
 * RF4: Emitir órdenes de compra a proveedores cuando el stock cae bajo el mínimo y recibirlas.
 */
public class BodegaComponent implements IBodega {

    private final RepositorioPoliMarket repo = RepositorioPoliMarket.instancia();
    private final IProveedores proveedores;

    public BodegaComponent(IProveedores proveedores) {
        this.proveedores = proveedores;
    }

    @Override
    public Bodega getBodega() {
        return repo.getBodega();
    }

    @Override
    public List<Producto> listarInventario() {
        return getBodega().getProductos();
    }

    @Override
    public Producto buscarProducto(int idProducto) {
        for (Producto p : listarInventario()) {
            if (p.getIdProducto() == idProducto) return p;
        }
        throw new IllegalArgumentException("No existe el producto " + idProducto);
    }

    @Override
    public int consultarStock(Producto p) {
        return getBodega().verificarStock(p);
    }

    @Override
    public List<Producto> listarProductosBajoMinimo() {
        return getBodega().listarProductosBajoMinimo();
    }

    @Override
    public OrdenCompra solicitarReabastecimiento(Producto p) {
        OrdenCompra abierta = ordenAbiertaDe(p);
        if (abierta != null) {
            throw new IllegalStateException(p.getNombreProducto() + " ya tiene la orden " + abierta.getNumeroOrden()
                    + " en estado " + abierta.getEstadoOrden() + ".");
        }
        Proveedor pr = proveedores.obtenerProveedorDe(p);
        OrdenCompra orden = getBodega().solicitarReabastecimiento(pr, p);
        Bitacora.registrar("Bodega", String.format("Orden %s EMITIDA a %s: %d u. de %s ($%,.0f)",
                orden.getNumeroOrden(), pr.getNombreEmpresa(), orden.getDetalles().get(0).getCantidadSolicitada(),
                p.getNombreProducto(), orden.getTotalOrden()));
        return orden;
    }

    @Override
    public List<OrdenCompra> listarOrdenesCompra() {
        List<OrdenCompra> lista = new ArrayList<>(getBodega().getOrdenesCompra());
        Collections.reverse(lista); // más recientes primero
        return lista;
    }

    @Override
    public OrdenCompra registrarRecepcion(int idOrden) {
        OrdenCompra orden = buscarOrden(idOrden);
        orden.registrarRecepcion();
        StringBuilder detalle = new StringBuilder();
        for (DetalleOrdenCompra d : orden.getDetalles()) {
            int antes = d.getProducto().getStockActual();
            getBodega().registrarIngresoStock(d.getProducto(), d.getCantidadRecibida());
            detalle.append(' ').append(d.getProducto().getNombreProducto())
                    .append(" (").append(antes).append(" -> ").append(d.getProducto().getStockActual()).append(')');
        }
        Bitacora.registrar("Bodega", "Orden " + orden.getNumeroOrden() + " RECIBIDA. Ingreso a stock:" + detalle);
        return orden;
    }

    @Override
    public OrdenCompra cancelarOrden(int idOrden) {
        OrdenCompra orden = buscarOrden(idOrden);
        orden.setEstadoOrden(EstadoOrden.CANCELADA);
        Bitacora.registrar("Bodega", "Orden " + orden.getNumeroOrden() + " CANCELADA.");
        return orden;
    }

    @Override
    public void revisarStockMinimo(Ventas venta) {
        for (DetalleVenta d : venta.getDetalles()) {
            Producto p = d.getProducto();
            if (p.requiereReabastecimiento() && ordenAbiertaDe(p) == null) {
                Bitacora.registrar("Bodega", p.getNombreProducto() + " quedó bajo el stock mínimo ("
                        + p.getStockActual() + "/" + p.getStockMinimo() + ").");
                solicitarReabastecimiento(p);
            }
        }
    }

    private OrdenCompra ordenAbiertaDe(Producto p) {
        for (OrdenCompra o : getBodega().getOrdenesCompra()) {
            if (o.estaAbierta() && o.incluyeProducto(p)) return o;
        }
        return null;
    }

    private OrdenCompra buscarOrden(int idOrden) {
        for (OrdenCompra o : getBodega().getOrdenesCompra()) {
            if (o.getIdOrden() == idOrden) return o;
        }
        throw new IllegalArgumentException("No existe la orden de compra " + idOrden);
    }
}
