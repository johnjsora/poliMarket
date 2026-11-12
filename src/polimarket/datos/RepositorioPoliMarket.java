package polimarket.datos;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import polimarket.modelo.Bodega;
import polimarket.modelo.Cliente;
import polimarket.modelo.Entregas;
import polimarket.modelo.OrdenCompra;
import polimarket.modelo.Producto;
import polimarket.modelo.Proveedor;
import polimarket.modelo.RecursosHumanos;
import polimarket.modelo.Vendedor;
import polimarket.modelo.Ventas;

/**
 * Capa de datos (componente RepositorioPoliMarket del diagrama de componentes).
 * Almacenamiento en memoria con datos de ejemplo que simula las bases de datos
 * de cada área de negocio de PoliMarket.
 */
public final class RepositorioPoliMarket {

    private static final RepositorioPoliMarket INSTANCIA = new RepositorioPoliMarket();

    private final RecursosHumanos recursosHumanos;
    private final Bodega bodega;
    private final List<Cliente> clientes = new ArrayList<>();
    private final List<Proveedor> proveedores = new ArrayList<>();
    private final List<Entregas> entregas = new ArrayList<>();

    private int secuenciaVenta = 1;
    private int secuenciaEntrega = 1;
    private int secuenciaOrden = 1;

    public static RepositorioPoliMarket instancia() {
        return INSTANCIA;
    }

    private RepositorioPoliMarket() {
        recursosHumanos = new RecursosHumanos(1, "Recursos Humanos", "Laura Gómez", "601 555 0101");
        bodega = new Bodega(1, "Bodega Central", "Calle 13 # 68-20, Bogotá", 5000);

        // Vendedores: Carlos queda autorizado hoy, Ana sin autorización y Jorge con autorización vencida.
        Vendedor carlos = new Vendedor(101, "Carlos", "Pérez", "carlos.perez@polimarket.co", "3001112233", fecha(2023, 2, 1));
        Vendedor ana = new Vendedor(102, "Ana", "Martínez", "ana.martinez@polimarket.co", "3104445566", fecha(2024, 6, 15));
        Vendedor jorge = new Vendedor(103, "Jorge", "Ramírez", "jorge.ramirez@polimarket.co", "3207778899",
                fecha(2024, 9, 3), fecha(2024, 9, 10), fecha(2025, 9, 10), "Laura Gómez");
        recursosHumanos.vincularVendedor(carlos);
        recursosHumanos.vincularVendedor(ana);
        recursosHumanos.vincularVendedor(jorge);
        recursosHumanos.autorizarVendedor(carlos);

        agregarCliente(new Cliente(1, "CC", "1012345678", "María", "López", "maria.lopez@mail.com", "3015550001", "Cra 7 # 45-10, Bogotá"));
        agregarCliente(new Cliente(2, "CC", "80123456", "Pedro", "Suárez", "pedro.suarez@mail.com", "3025550002", "Av. 68 # 22-31, Bogotá"));
        agregarCliente(new Cliente(3, "NIT", "900123456-7", "Tiendas", "El Ahorro SAS", "compras@elahorro.co", "6015550003", "Calle 80 # 15-40, Bogotá"));
        agregarCliente(new Cliente(4, "CE", "E-445566", "Lucía", "Fernández", "lucia.f@mail.com", "3165550004", "Cl 100 # 19-61, Bogotá"));

        Proveedor alimentos = new Proveedor(1, "860001234-1", "Alimentos del Valle SA", "Rosa Díaz", "6025551000", "ventas@alimvalle.co");
        Proveedor aseo = new Proveedor(2, "830005678-2", "Distribuidora Aseo Total", "Mario Ruiz", "6015552000", "pedidos@aseototal.co");
        Proveedor bebidas = new Proveedor(3, "900765432-3", "Bebidas Andinas SAS", "Clara Mejía", "6045553000", "comercial@bebandinas.co");
        proveedores.add(alimentos);
        proveedores.add(aseo);
        proveedores.add(bebidas);

        agregarProducto(new Producto(1, "ALI-001", "Arroz 1 kg", "Arroz blanco premium", 4800f, 10, 60), alimentos);
        agregarProducto(new Producto(2, "ALI-002", "Aceite 1 L", "Aceite vegetal de girasol", 12500f, 8, 25), alimentos);
        agregarProducto(new Producto(3, "ALI-003", "Café 500 g", "Café molido tostión media", 18900f, 5, 12), alimentos);
        agregarProducto(new Producto(4, "ASE-001", "Detergente 2 kg", "Detergente en polvo multiusos", 21900f, 6, 15), aseo);
        agregarProducto(new Producto(5, "ASE-002", "Jabón x3", "Jabón de tocador paquete x3", 7600f, 10, 40), aseo);
        agregarProducto(new Producto(6, "BEB-001", "Gaseosa 1.5 L", "Bebida gaseosa sabor cola", 5200f, 12, 30), bebidas);
        agregarProducto(new Producto(7, "BEB-002", "Agua 600 ml", "Agua sin gas", 1800f, 20, 22), bebidas);
    }

    private void agregarCliente(Cliente c) {
        c.registrarCliente();
        clientes.add(c);
    }

    private void agregarProducto(Producto p, Proveedor proveedor) {
        bodega.agregarProducto(p);
        proveedor.agregarProductoSuministrado(p);
    }

    private static Date fecha(int anio, int mes, int dia) {
        return Date.from(LocalDate.of(anio, mes, dia).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public synchronized int siguienteIdVenta() { return secuenciaVenta++; }
    public synchronized int siguienteIdEntrega() { return secuenciaEntrega++; }
    public synchronized int siguienteIdOrden() { return secuenciaOrden++; }

    public RecursosHumanos getRecursosHumanos() { return recursosHumanos; }
    public Bodega getBodega() { return bodega; }
    public List<Vendedor> getVendedores() { return recursosHumanos.getVendedores(); }
    public List<Cliente> getClientes() { return clientes; }
    public List<Producto> getProductos() { return bodega.getProductos(); }
    public List<Proveedor> getProveedores() { return proveedores; }
    public List<Entregas> getEntregas() { return entregas; }
    public List<OrdenCompra> getOrdenesCompra() { return bodega.getOrdenesCompra(); }

    /** Primer proveedor que suministra el producto, o null. */
    public Proveedor getProveedorDe(Producto p) {
        for (Proveedor pr : proveedores) {
            if (pr.suministraProducto(p)) return pr;
        }
        return null;
    }

    /** Entrega asociada a la venta, o null si aún no se ha programado. */
    public Entregas getEntregaDe(Ventas v) {
        for (Entregas e : entregas) {
            if (e.getVenta() == v) return e;
        }
        return null;
    }
}
