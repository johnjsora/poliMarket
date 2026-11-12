package polimarket.modelo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Proveedor que abastece a PoliMarket.
 * Relación del diagrama de clases: Proveedor "0..*" --> "0..*" Producto : suministra
 */
public class Proveedor {

    private int idProveedor;
    private String nitEmpresa;
    private String nombreEmpresa;
    private String personaContacto;
    private String telefonoContacto;
    private String correoCorporativo;

    /** Asociación "suministra" (0..*). */
    private final List<Producto> productos = new ArrayList<>();
    /** Registro de los productos adquiridos por la empresa a este proveedor. */
    private final List<String> historialSuministros = new ArrayList<>();
    private Producto ultimoProducto;
    private int ultimaCantidad;
    private int consecutivoFactura = 1000;

    public Proveedor(int idProveedor, String nitEmpresa, String nombreEmpresa, String personaContacto,
                     String telefonoContacto, String correoCorporativo) {
        this.idProveedor = idProveedor;
        this.nitEmpresa = nitEmpresa;
        this.nombreEmpresa = nombreEmpresa;
        this.personaContacto = personaContacto;
        this.telefonoContacto = telefonoContacto;
        this.correoCorporativo = correoCorporativo;
    }

    /** El proveedor despacha unidades de un producto que suministra y lo factura. */
    public void suministrarProductos(Producto p, int cantidad) {
        if (!suministraProducto(p)) {
            throw new IllegalArgumentException(nombreEmpresa + " no suministra " + p.getNombreProducto());
        }
        this.ultimoProducto = p;
        this.ultimaCantidad = cantidad;
        this.consecutivoFactura++;
        historialSuministros.add(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                + " | " + p.getCodigoSku() + " x" + cantidad + " | " + emitirFacturaProveedor());
    }

    public String emitirFacturaProveedor() {
        if (ultimoProducto == null) {
            return "Sin suministros facturados";
        }
        float total = ultimoProducto.getPrecioUnitario() * Bodega.FACTOR_COSTO_COMPRA * ultimaCantidad;
        return String.format("FAC-%s-%d: %d u. de %s por $%,.0f",
                nitEmpresa, consecutivoFactura, ultimaCantidad, ultimoProducto.getNombreProducto(), total);
    }

    public boolean suministraProducto(Producto p) {
        return productos.contains(p);
    }

    public void agregarProductoSuministrado(Producto p) {
        if (!productos.contains(p)) {
            productos.add(p);
        }
    }

    public List<Producto> getProductos() { return productos; }
    public List<String> getHistorialSuministros() { return historialSuministros; }
    public int getIdProveedor() { return idProveedor; }
    public String getNitEmpresa() { return nitEmpresa; }
    public String getNombreEmpresa() { return nombreEmpresa; }
    public String getPersonaContacto() { return personaContacto; }
    public String getTelefonoContacto() { return telefonoContacto; }
    public String getCorreoCorporativo() { return correoCorporativo; }

    @Override
    public String toString() { return nombreEmpresa + " (NIT " + nitEmpresa + ")"; }
}
