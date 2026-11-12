package polimarket.componentes;

/**
 * Ensambla los componentes respetando las interfaces requeridas del diagrama de componentes:
 *   VentasFacade      ..> IRecursosHumanos, IBodega
 *   BodegaComponent   ..> IProveedores
 *   EntregasComponent ..> IVentas, IBodega
 * Los clientes solo reciben interfaces; ambos comparten estas mismas instancias.
 */
public final class PoliMarketComponentes {

    private static final PoliMarketComponentes INSTANCIA = new PoliMarketComponentes();

    private final IRecursosHumanos recursosHumanos = new RecursosHumanosFacade();
    private final IProveedores proveedores = new ProveedoresComponent();
    private final IBodega bodega = new BodegaComponent(proveedores);
    private final IVentas ventas = new VentasFacade(recursosHumanos, bodega);
    private final IEntregas entregas = new EntregasComponent(ventas, bodega);

    private PoliMarketComponentes() { }

    public static PoliMarketComponentes instancia() { return INSTANCIA; }

    public IRecursosHumanos recursosHumanos() { return recursosHumanos; }
    public IVentas ventas() { return ventas; }
    public IBodega bodega() { return bodega; }
    public IProveedores proveedores() { return proveedores; }
    public IEntregas entregas() { return entregas; }
}
