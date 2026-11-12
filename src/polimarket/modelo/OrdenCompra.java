package polimarket.modelo;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Orden de compra que la bodega emite a un proveedor para reabastecerse.
 * Relaciones del diagrama de clases:
 *   Bodega "1" --> "0..*" OrdenCompra : solicita
 *   OrdenCompra "0..*" --> "1" Proveedor : se emite a
 *   OrdenCompra "1" *-- "1..*" DetalleOrdenCompra : contiene (composición)
 *   OrdenCompra ..> EstadoOrden : <<use>>
 *
 * Ciclo de vida: EMITIDA -> EN_TRANSITO (el proveedor despacha) -> RECIBIDA (ingresa a bodega)
 *                EMITIDA -> CANCELADA
 */
public class OrdenCompra {

    /** Días que tarda el proveedor en entregar. */
    public static final int DIAS_ENTREGA = 3;

    private int idOrden;
    private String numeroOrden;
    private Date fechaEmision;
    private Date fechaEntregaEsperada;
    private EstadoOrden estadoOrden;
    private float totalOrden;

    /** Asociación "se emite a" (1). */
    private final Proveedor proveedor;
    /** Composición "contiene" (1..*). */
    private final List<DetalleOrdenCompra> detalles = new ArrayList<>();

    public OrdenCompra(int idOrden, Proveedor proveedor) {
        this.idOrden = idOrden;
        this.proveedor = proveedor;
    }

    public void agregarDetalle(Producto p, int cantidad, float precioCompra) {
        if (estadoOrden != null) {
            throw new IllegalStateException("La orden ya fue generada; no admite más productos.");
        }
        if (!proveedor.suministraProducto(p)) {
            throw new IllegalArgumentException(proveedor.getNombreEmpresa() + " no suministra " + p.getNombreProducto());
        }
        detalles.add(new DetalleOrdenCompra(detalles.size() + 1, p, cantidad, precioCompra));
    }

    /** Asigna número y fechas, calcula el total y deja la orden EMITIDA. */
    public void generarOrden() {
        if (detalles.isEmpty()) {
            throw new IllegalStateException("La orden de compra no tiene productos.");
        }
        this.numeroOrden = String.format("OC-%06d", idOrden);
        this.fechaEmision = new Date();
        this.fechaEntregaEsperada = Date.from(LocalDate.now().plusDays(DIAS_ENTREGA)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());
        float total = 0f;
        for (DetalleOrdenCompra d : detalles) total += d.calcularSubtotal();
        this.totalOrden = total;
        this.estadoOrden = EstadoOrden.EMITIDA;
    }

    /** EN_TRANSITO -> RECIBIDA: registra lo recibido (el ingreso a stock lo hace la bodega). */
    public void registrarRecepcion() {
        if (estadoOrden != EstadoOrden.EN_TRANSITO) {
            throw new IllegalStateException("Solo se recibe una orden EN_TRANSITO (estado: " + estadoOrden + ").");
        }
        for (DetalleOrdenCompra d : detalles) {
            d.registrarCantidadRecibida(d.getCantidadSolicitada());
        }
        this.estadoOrden = EstadoOrden.RECIBIDA;
    }

    /** Cambia el estado validando las transiciones permitidas. */
    public void setEstadoOrden(EstadoOrden nuevo) {
        boolean valido = (estadoOrden == EstadoOrden.EMITIDA
                && (nuevo == EstadoOrden.EN_TRANSITO || nuevo == EstadoOrden.CANCELADA))
                || (estadoOrden == EstadoOrden.EN_TRANSITO && nuevo == EstadoOrden.RECIBIDA);
        if (!valido) {
            throw new IllegalStateException("No se puede pasar la orden " + numeroOrden + " de "
                    + estadoOrden + " a " + nuevo + ".");
        }
        if (nuevo == EstadoOrden.RECIBIDA) {
            registrarRecepcion();
        } else {
            this.estadoOrden = nuevo;
        }
    }

    /** Orden EMITIDA o EN_TRANSITO. */
    public boolean estaAbierta() {
        return estadoOrden == EstadoOrden.EMITIDA || estadoOrden == EstadoOrden.EN_TRANSITO;
    }

    public boolean incluyeProducto(Producto p) {
        for (DetalleOrdenCompra d : detalles) {
            if (d.getProducto().equals(p)) return true;
        }
        return false;
    }

    public List<DetalleOrdenCompra> getDetalles() { return Collections.unmodifiableList(detalles); }
    public int getIdOrden() { return idOrden; }
    public String getNumeroOrden() { return numeroOrden; }
    public Date getFechaEmision() { return fechaEmision; }
    public Date getFechaEntregaEsperada() { return fechaEntregaEsperada; }
    public EstadoOrden getEstadoOrden() { return estadoOrden; }
    public float getTotalOrden() { return totalOrden; }
    public Proveedor getProveedor() { return proveedor; }
}
