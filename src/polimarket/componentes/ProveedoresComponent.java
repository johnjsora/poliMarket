package polimarket.componentes;

import java.util.List;

import polimarket.datos.Bitacora;
import polimarket.datos.RepositorioPoliMarket;
import polimarket.modelo.DetalleOrdenCompra;
import polimarket.modelo.EstadoOrden;
import polimarket.modelo.OrdenCompra;
import polimarket.modelo.Producto;
import polimarket.modelo.Proveedor;

/**
 * Componente de negocio de Proveedores (provee IProveedores).
 * Registra qué empresa surte cada producto y despacha las órdenes de compra.
 */
public class ProveedoresComponent implements IProveedores {

    private final RepositorioPoliMarket repo = RepositorioPoliMarket.instancia();

    @Override
    public List<Proveedor> listarProveedores() {
        return repo.getProveedores();
    }

    @Override
    public Proveedor obtenerProveedorDe(Producto p) {
        Proveedor pr = repo.getProveedorDe(p);
        if (pr == null) {
            throw new IllegalStateException("El producto " + p.getNombreProducto() + " no tiene proveedor asignado.");
        }
        return pr;
    }

    @Override
    public OrdenCompra despacharOrden(int idOrden) {
        OrdenCompra orden = buscarOrden(idOrden);
        if (orden.getEstadoOrden() != EstadoOrden.EMITIDA) {
            throw new IllegalStateException("Solo se despacha una orden EMITIDA (" + orden.getNumeroOrden()
                    + " está " + orden.getEstadoOrden() + ").");
        }
        Proveedor pr = orden.getProveedor();
        for (DetalleOrdenCompra d : orden.getDetalles()) {
            pr.suministrarProductos(d.getProducto(), d.getCantidadSolicitada());
        }
        orden.setEstadoOrden(EstadoOrden.EN_TRANSITO);
        Bitacora.registrar("Proveedores", pr.getNombreEmpresa() + " despachó la orden " + orden.getNumeroOrden()
                + ". " + pr.emitirFacturaProveedor());
        return orden;
    }

    @Override
    public List<String> consultarHistorialCompras(Proveedor pr) {
        return pr.getHistorialSuministros();
    }

    private OrdenCompra buscarOrden(int idOrden) {
        for (OrdenCompra o : repo.getOrdenesCompra()) {
            if (o.getIdOrden() == idOrden) return o;
        }
        throw new IllegalArgumentException("No existe la orden de compra " + idOrden);
    }
}
