package polimarket.modelo;

/**
 * Línea de una orden de compra.
 * Relaciones del diagrama de clases:
 *   OrdenCompra "1" *-- "1..*" DetalleOrdenCompra : contiene (composición)
 *   DetalleOrdenCompra "0..*" --> "1" Producto : solicita
 */
public class DetalleOrdenCompra {

    private int idDetalleOrden;
    private int cantidadSolicitada;
    private int cantidadRecibida;
    private float precioCompra;
    private float subtotalLinea;

    /** Asociación "solicita" (1). */
    private final Producto producto;

    public DetalleOrdenCompra(int idDetalleOrden, Producto producto, int cantidadSolicitada, float precioCompra) {
        if (cantidadSolicitada <= 0) {
            throw new IllegalArgumentException("La cantidad solicitada debe ser mayor a cero.");
        }
        this.idDetalleOrden = idDetalleOrden;
        this.producto = producto;
        this.cantidadSolicitada = cantidadSolicitada;
        this.precioCompra = precioCompra;
        calcularSubtotal();
    }

    public float calcularSubtotal() {
        subtotalLinea = cantidadSolicitada * precioCompra;
        return subtotalLinea;
    }

    void registrarCantidadRecibida(int cantidad) {
        this.cantidadRecibida = cantidad;
    }

    public int getIdDetalleOrden() { return idDetalleOrden; }
    public int getCantidadSolicitada() { return cantidadSolicitada; }
    public int getCantidadRecibida() { return cantidadRecibida; }
    public float getPrecioCompra() { return precioCompra; }
    public float getSubtotalLinea() { return subtotalLinea; }
    public Producto getProducto() { return producto; }
}
