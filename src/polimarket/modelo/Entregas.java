package polimarket.modelo;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * Entrega de los productos de una venta.
 * Relaciones del diagrama de clases:
 *   Entregas "1" --> "1" Ventas : se basa en
 *   Entregas "1" --> "1" Bodega : descarga stock
 *   Entregas ..> EstadoEntrega : <<use>>
 *
 * Ciclo de vida: PROGRAMADA -> EN_RUTA (salida de bodega) -> ENTREGADA | FALLIDA
 */
public class Entregas {

    private int idEntrega;
    private String numeroGuia;
    private Date fechaProgramada;
    private Date fechaEntregaReal;
    private String direccionDestino;
    private EstadoEntrega estadoEntrega;
    private String transportistaAsignado;

    /** Asociación "se basa en" (1). */
    private Ventas venta;
    /** Asociación "descarga stock" (1). */
    private Bodega bodega;

    public Entregas(int idEntrega, String transportistaAsignado) {
        this.idEntrega = idEntrega;
        this.transportistaAsignado = transportistaAsignado;
    }

    public void programarEntrega(Ventas v) {
        if (v.getEstadoVenta() != EstadoVenta.CONFIRMADA) {
            throw new IllegalStateException("La venta #" + v.getIdVenta()
                    + " no está disponible para entrega (estado " + v.getEstadoVenta() + ").");
        }
        this.venta = v;
        this.numeroGuia = String.format("GU-%06d", idEntrega);
        this.direccionDestino = v.getCliente().getDireccionResidencia();
        this.fechaProgramada = Date.from(LocalDate.now().plusDays(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());
        this.estadoEntrega = EstadoEntrega.PROGRAMADA;
    }

    /** Descarga de la bodega los productos de la venta, que en ese momento queda FACTURADA. */
    public void registrarSalidaBodega(Bodega b) {
        if (estadoEntrega != EstadoEntrega.PROGRAMADA) {
            throw new IllegalStateException("Solo se despachan entregas en estado PROGRAMADA.");
        }
        if (venta.getEstadoVenta() != EstadoVenta.CONFIRMADA) {
            throw new IllegalStateException("La venta #" + venta.getIdVenta() + " está " + venta.getEstadoVenta()
                    + "; no se puede despachar.");
        }
        for (DetalleVenta d : venta.getDetalles()) {
            if (!d.getProducto().verificarStockDisponible(d.getCantidad())) {
                throw new IllegalStateException("Stock insuficiente de " + d.getProducto().getNombreProducto()
                        + " (hay " + b.verificarStock(d.getProducto()) + ", se requieren " + d.getCantidad()
                        + "). Espere la recepción de la orden de compra.");
            }
        }
        for (DetalleVenta d : venta.getDetalles()) {
            b.registrarSalidaStock(d.getProducto(), d.getCantidad());
        }
        this.bodega = b;
        venta.facturar();
        this.estadoEntrega = EstadoEntrega.EN_RUTA;
    }

    public void actualizarEstadoEnvio(EstadoEntrega nuevoEstado) {
        if (estadoEntrega != EstadoEntrega.EN_RUTA) {
            throw new IllegalStateException("Solo se actualiza una entrega EN_RUTA (estado: " + estadoEntrega + ").");
        }
        if (nuevoEstado != EstadoEntrega.ENTREGADA && nuevoEstado != EstadoEntrega.FALLIDA) {
            throw new IllegalArgumentException("Una entrega en ruta solo puede quedar ENTREGADA o FALLIDA.");
        }
        this.estadoEntrega = nuevoEstado;
        if (nuevoEstado == EstadoEntrega.ENTREGADA) {
            this.fechaEntregaReal = new Date();
        }
    }

    public int getIdEntrega() { return idEntrega; }
    public String getNumeroGuia() { return numeroGuia; }
    public Date getFechaProgramada() { return fechaProgramada; }
    public Date getFechaEntregaReal() { return fechaEntregaReal; }
    public String getDireccionDestino() { return direccionDestino; }
    public EstadoEntrega getEstadoEntrega() { return estadoEntrega; }
    public String getTransportistaAsignado() { return transportistaAsignado; }
    public Ventas getVenta() { return venta; }
    public Bodega getBodega() { return bodega; }
}
