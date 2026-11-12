package polimarket.modelo;

/**
 * Producto comercializado por PoliMarket. Su stock se administra desde la Bodega.
 */
public class Producto {

    private int idProducto;
    private String codigoSku;
    private String nombreProducto;
    private String descripcion;
    private float precioUnitario;
    private int stockMinimo;
    private int stockActual;

    public Producto(int idProducto, String codigoSku, String nombreProducto, String descripcion,
                    float precioUnitario, int stockMinimo, int stockActual) {
        this.idProducto = idProducto;
        this.codigoSku = codigoSku;
        this.nombreProducto = nombreProducto;
        this.descripcion = descripcion;
        this.precioUnitario = precioUnitario;
        this.stockMinimo = stockMinimo;
        this.stockActual = stockActual;
    }

    /** Suma (cantidad positiva) o resta (cantidad negativa) unidades al stock. */
    public void actualizarStock(int cantidad) {
        if (stockActual + cantidad < 0) {
            throw new IllegalStateException("Stock insuficiente de " + nombreProducto
                    + ": hay " + stockActual + " y se requieren " + (-cantidad));
        }
        stockActual += cantidad;
    }

    public boolean verificarStockDisponible(int cantidadRequerida) {
        return stockActual >= cantidadRequerida;
    }

    public boolean requiereReabastecimiento() {
        return stockActual < stockMinimo;
    }

    public int getIdProducto() { return idProducto; }
    public String getCodigoSku() { return codigoSku; }
    public String getNombreProducto() { return nombreProducto; }
    public String getDescripcion() { return descripcion; }
    public float getPrecioUnitario() { return precioUnitario; }
    public int getStockMinimo() { return stockMinimo; }
    public int getStockActual() { return stockActual; }

    @Override
    public String toString() { return codigoSku + " - " + nombreProducto; }
}
