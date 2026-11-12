package polimarket.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Venta realizada por un vendedor.
 * Relaciones del diagrama de clases:
 *   Ventas "0..*" --> "1" Cliente : atiende
 *   Ventas "1" *-- "1..*" DetalleVenta : contiene (composición)
 *   Ventas ..> Bodega : <<use>> verifica stock
 *   Ventas ..> EstadoVenta : <<use>>
 *
 * Ciclo de vida: BORRADOR -> CONFIRMADA (registrarVenta) -> FACTURADA (salida de bodega)
 *                BORRADOR / CONFIRMADA -> ANULADA (cancelarVenta)
 */
public class Ventas {

    public static final float TASA_IVA = 0.19f;

    private int idVenta;
    private String numeroFactura;
    private LocalDateTime fechaHora;
    private float subtotal;
    private float impuestos;
    private float totalVenta;
    private EstadoVenta estadoVenta;

    /** Asociación "atiende" (1). */
    private Cliente cliente;
    /** Composición "contiene" (1..*). */
    private final List<DetalleVenta> detalles = new ArrayList<>();

    public Ventas(int idVenta, Cliente cliente) {
        this.idVenta = idVenta;
        this.cliente = cliente;
        this.fechaHora = LocalDateTime.now();
        this.estadoVenta = EstadoVenta.BORRADOR;
    }

    /** Confirma la venta: BORRADOR -> CONFIRMADA. */
    public boolean registrarVenta() {
        if (estadoVenta != EstadoVenta.BORRADOR || cliente == null || detalles.isEmpty()) {
            return false;
        }
        this.fechaHora = LocalDateTime.now();
        this.estadoVenta = EstadoVenta.CONFIRMADA;
        return true;
    }

    /** Agrega una línea; si el producto ya está en la venta, suma la cantidad. */
    public void agregarDetalle(Producto p, int cantidad) {
        if (estadoVenta != EstadoVenta.BORRADOR) {
            throw new IllegalStateException("Solo se pueden agregar productos a una venta en BORRADOR.");
        }
        DetalleVenta existente = buscarDetalle(p);
        if (existente != null) {
            existente.sumarCantidad(cantidad);
        } else {
            detalles.add(new DetalleVenta(detalles.size() + 1, p, cantidad));
        }
        calcularTotales();
    }

    /** Dependencia con Bodega: consulta si la bodega tiene existencias del producto. */
    public boolean consultarDisponibilidadBodega(Bodega b, Producto p) {
        return b.verificarStock(p) > 0;
    }

    public void calcularTotales() {
        float suma = 0f;
        for (DetalleVenta d : detalles) {
            suma += d.calcularSubtotal();
        }
        this.subtotal = suma;
        this.impuestos = suma * TASA_IVA;
        this.totalVenta = subtotal + impuestos;
    }

    public void cancelarVenta() {
        if (estadoVenta != EstadoVenta.BORRADOR && estadoVenta != EstadoVenta.CONFIRMADA) {
            throw new IllegalStateException("No se puede anular una venta en estado " + estadoVenta + ".");
        }
        this.estadoVenta = EstadoVenta.ANULADA;
    }

    /** CONFIRMADA -> FACTURADA. Lo invoca Entregas al registrar la salida de bodega. */
    void facturar() {
        if (estadoVenta != EstadoVenta.CONFIRMADA) {
            throw new IllegalStateException("Solo se factura una venta CONFIRMADA (estado: " + estadoVenta + ").");
        }
        this.numeroFactura = String.format("FV-%06d", idVenta);
        this.estadoVenta = EstadoVenta.FACTURADA;
    }

    private DetalleVenta buscarDetalle(Producto p) {
        for (DetalleVenta d : detalles) {
            if (d.getProducto().equals(p)) return d;
        }
        return null;
    }

    public int getCantidadDe(Producto p) {
        DetalleVenta d = buscarDetalle(p);
        return d == null ? 0 : d.getCantidad();
    }

    public int getCantidadTotalItems() {
        int total = 0;
        for (DetalleVenta d : detalles) total += d.getCantidad();
        return total;
    }

    public List<DetalleVenta> getDetalles() { return Collections.unmodifiableList(detalles); }
    public int getIdVenta() { return idVenta; }
    public String getNumeroFactura() { return numeroFactura; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public float getSubtotal() { return subtotal; }
    public float getImpuestos() { return impuestos; }
    public float getTotalVenta() { return totalVenta; }
    public EstadoVenta getEstadoVenta() { return estadoVenta; }
    public Cliente getCliente() { return cliente; }
}
