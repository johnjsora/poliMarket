package polimarket.modelo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import polimarket.datos.RepositorioPoliMarket;

/**
 * Vendedor de PoliMarket.
 * Relación del diagrama de clases: Vendedor "1" --> "0..*" Ventas : realiza
 */
public class Vendedor {

    private int idVendedor;
    private String nombres;
    private String apellidos;
    private String correoElectronico;
    private String telefono;
    private boolean estadoAutorizacion;
    private Date fechaAutorizacion;
    private Date fechaVencimientoAutorizacion;
    private String autorizadoPor;
    private Date fechaIngreso;

    /** Asociación "realiza" (0..*). */
    private final List<Ventas> ventas = new ArrayList<>();

    /** Vendedor nuevo, sin autorización. */
    public Vendedor(int idVendedor, String nombres, String apellidos, String correoElectronico,
                    String telefono, Date fechaIngreso) {
        this.idVendedor = idVendedor;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correoElectronico = correoElectronico;
        this.telefono = telefono;
        this.fechaIngreso = fechaIngreso;
        this.estadoAutorizacion = false;
    }

    /** Vendedor cargado desde el sistema de RH con una autorización ya registrada. */
    public Vendedor(int idVendedor, String nombres, String apellidos, String correoElectronico,
                    String telefono, Date fechaIngreso, Date fechaAutorizacion,
                    Date fechaVencimientoAutorizacion, String autorizadoPor) {
        this(idVendedor, nombres, apellidos, correoElectronico, telefono, fechaIngreso);
        registrarAutorizacion(true, fechaAutorizacion, fechaVencimientoAutorizacion, autorizadoPor);
    }

    public void solicitarAutorizacion(RecursosHumanos rh) {
        rh.autorizarVendedor(this);
    }

    /** Crea una nueva venta en BORRADOR. Solo un vendedor con autorización vigente puede vender. */
    public Ventas gestionarVenta(Cliente c) {
        if (!tieneAutorizacionVigente()) {
            throw new IllegalStateException("El vendedor " + getNombreCompleto()
                    + " no tiene autorización vigente (" + getDescripcionAutorizacion() + ").");
        }
        Ventas venta = new Ventas(RepositorioPoliMarket.instancia().siguienteIdVenta(), c);
        ventas.add(venta);
        return venta;
    }

    public List<Producto> consultarProductosDisponibles() {
        List<Producto> disponibles = new ArrayList<>();
        for (Producto p : RepositorioPoliMarket.instancia().getProductos()) {
            if (p.verificarStockDisponible(1)) {
                disponibles.add(p);
            }
        }
        return disponibles;
    }

    /** Autorizado y con la fecha de vencimiento aún no cumplida. */
    public boolean tieneAutorizacionVigente() {
        return estadoAutorizacion && fechaVencimientoAutorizacion != null
                && new Date().before(fechaVencimientoAutorizacion);
    }

    /** "VIGENTE hasta ...", "VENCIDA desde ..." o "NO AUTORIZADO". */
    public String getDescripcionAutorizacion() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        if (!estadoAutorizacion) return "NO AUTORIZADO";
        if (tieneAutorizacionVigente()) return "VIGENTE hasta " + f.format(fechaVencimientoAutorizacion);
        return "VENCIDA desde " + f.format(fechaVencimientoAutorizacion);
    }

    void registrarAutorizacion(boolean estado, Date desde, Date hasta, String por) {
        this.estadoAutorizacion = estado;
        this.fechaAutorizacion = desde;
        this.fechaVencimientoAutorizacion = hasta;
        this.autorizadoPor = por;
    }

    public String getNombreCompleto() { return nombres + " " + apellidos; }
    public List<Ventas> getVentas() { return ventas; }
    public int getIdVendedor() { return idVendedor; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getCorreoElectronico() { return correoElectronico; }
    public String getTelefono() { return telefono; }
    public boolean isEstadoAutorizacion() { return estadoAutorizacion; }
    public Date getFechaAutorizacion() { return fechaAutorizacion; }
    public Date getFechaVencimientoAutorizacion() { return fechaVencimientoAutorizacion; }
    public String getAutorizadoPor() { return autorizadoPor; }
    public Date getFechaIngreso() { return fechaIngreso; }

    @Override
    public String toString() { return idVendedor + " - " + getNombreCompleto(); }
}
