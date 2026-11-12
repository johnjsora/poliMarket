package polimarket.modelo;

import java.util.ArrayList;
import java.util.List;

import polimarket.datos.RepositorioPoliMarket;

/**
 * Bodega de PoliMarket.
 * Relaciones del diagrama de clases:
 *   Bodega "1" --> "0..*" Producto : almacena
 *   Bodega "1" --> "0..*" OrdenCompra : solicita
 */
public class Bodega {

    /** Costo de compra estimado respecto al precio de venta. */
    public static final float FACTOR_COSTO_COMPRA = 0.6f;

    private int idBodega;
    private String nombreBodega;
    private String ubicacionFisica;
    private int capacidadMaxima;

    /** Asociación "almacena" (0..*). */
    private final List<Producto> productos = new ArrayList<>();
    /** Asociación "solicita" (0..*). */
    private final List<OrdenCompra> ordenesCompra = new ArrayList<>();

    public Bodega(int idBodega, String nombreBodega, String ubicacionFisica, int capacidadMaxima) {
        this.idBodega = idBodega;
        this.nombreBodega = nombreBodega;
        this.ubicacionFisica = ubicacionFisica;
        this.capacidadMaxima = capacidadMaxima;
    }

    public int verificarStock(Producto p) {
        return p.getStockActual();
    }

    public void registrarSalidaStock(Producto p, int cantidad) {
        p.actualizarStock(-cantidad);
    }

    public void registrarIngresoStock(Producto p, int cantidad) {
        p.actualizarStock(cantidad);
    }

    public List<Producto> listarProductosBajoMinimo() {
        List<Producto> bajos = new ArrayList<>();
        for (Producto p : productos) {
            if (p.requiereReabastecimiento()) bajos.add(p);
        }
        return bajos;
    }

    /**
     * Emite una orden de compra al proveedor por las unidades necesarias para dejar
     * el producto en el triple de su stock mínimo. El stock ingresa cuando la orden se recibe.
     */
    public OrdenCompra solicitarReabastecimiento(Proveedor pr, Producto p) {
        if (!pr.suministraProducto(p)) {
            throw new IllegalArgumentException(pr.getNombreEmpresa() + " no suministra " + p.getNombreProducto());
        }
        int cantidad = Math.max(p.getStockMinimo() * 3 - p.getStockActual(), p.getStockMinimo());
        OrdenCompra orden = new OrdenCompra(RepositorioPoliMarket.instancia().siguienteIdOrden(), pr);
        orden.agregarDetalle(p, cantidad, p.getPrecioUnitario() * FACTOR_COSTO_COMPRA);
        orden.generarOrden();
        ordenesCompra.add(orden);
        return orden;
    }

    public void agregarProducto(Producto p) {
        if (!productos.contains(p)) {
            productos.add(p);
        }
    }

    public List<Producto> getProductos() { return productos; }
    public List<OrdenCompra> getOrdenesCompra() { return ordenesCompra; }
    public int getIdBodega() { return idBodega; }
    public String getNombreBodega() { return nombreBodega; }
    public String getUbicacionFisica() { return ubicacionFisica; }
    public int getCapacidadMaxima() { return capacidadMaxima; }
}
