package polimarket.modelo;

/**
 * Línea de detalle de una venta.
 * Relaciones del diagrama de clases:
 *   Ventas "1" *-- "1..*" DetalleVenta : contiene (composición)
 *   DetalleVenta "0..*" --> "1" Producto : refiere
 */
public class DetalleVenta {

    private int idDetalle;
    private int cantidad;
    private float precioUnitario;
    /** Porcentaje de descuento entre 0 y 1. Por defecto 0. */
    private float descuento;
    private float subtotalLinea;

    /** Asociación "refiere" (1). */
    private final Producto producto;

    /** El precio se toma del producto en el momento de la venta. */
    public DetalleVenta(int idDetalle, Producto producto, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }
        this.idDetalle = idDetalle;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = producto.getPrecioUnitario();
        this.descuento = 0f;
        calcularSubtotal();
    }

    public float calcularSubtotal() {
        subtotalLinea = cantidad * precioUnitario * (1f - descuento);
        return subtotalLinea;
    }

    void sumarCantidad(int adicional) {
        this.cantidad += adicional;
        calcularSubtotal();
    }

    public void setDescuento(float descuento) {
        if (descuento < 0f || descuento > 1f) {
            throw new IllegalArgumentException("El descuento debe estar entre 0 y 1.");
        }
        this.descuento = descuento;
        calcularSubtotal();
    }

    public int getIdDetalle() { return idDetalle; }
    public int getCantidad() { return cantidad; }
    public float getPrecioUnitario() { return precioUnitario; }
    public float getDescuento() { return descuento; }
    public float getSubtotalLinea() { return subtotalLinea; }
    public Producto getProducto() { return producto; }
}
