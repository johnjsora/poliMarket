package polimarket.componentes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import polimarket.datos.Bitacora;
import polimarket.datos.RepositorioPoliMarket;
import polimarket.modelo.EstadoEntrega;
import polimarket.modelo.Entregas;
import polimarket.modelo.Ventas;

/**
 * Componente de negocio de Entregas (provee IEntregas; requiere IVentas e IBodega).
 * RF5: Programar entregas (qué productos entregar y a quién) y registrar la salida de bodega.
 */
public class EntregasComponent implements IEntregas {

    private final RepositorioPoliMarket repo = RepositorioPoliMarket.instancia();
    private final IVentas ventas;
    private final IBodega bodega;

    public EntregasComponent(IVentas ventas, IBodega bodega) {
        this.ventas = ventas;
        this.bodega = bodega;
    }

    @Override
    public List<Ventas> consultarVentasPorEntregar() {
        return ventas.obtenerVentasPendientesDeEntrega();
    }

    @Override
    public List<Entregas> listarEntregas() {
        List<Entregas> lista = new ArrayList<>(repo.getEntregas());
        Collections.reverse(lista); // más recientes primero
        return lista;
    }

    @Override
    public Entregas buscarEntrega(int idEntrega) {
        for (Entregas e : repo.getEntregas()) {
            if (e.getIdEntrega() == idEntrega) return e;
        }
        throw new IllegalArgumentException("No existe la entrega #" + idEntrega);
    }

    @Override
    public Entregas programarEntrega(int idVenta, String transportista) {
        if (transportista == null || transportista.isBlank()) {
            throw new IllegalArgumentException("Indique el transportista asignado.");
        }
        Ventas venta = ventas.buscarVenta(idVenta);
        if (repo.getEntregaDe(venta) != null) {
            throw new IllegalStateException("La venta #" + idVenta + " ya tiene una entrega programada.");
        }
        Entregas entrega = new Entregas(repo.siguienteIdEntrega(), transportista.trim());
        entrega.programarEntrega(venta);
        repo.getEntregas().add(entrega);
        Bitacora.registrar("Entregas", "Entrega " + entrega.getNumeroGuia() + " programada para la venta #"
                + idVenta + " -> " + entrega.getDireccionDestino() + " (" + transportista.trim() + ")");
        return entrega;
    }

    @Override
    public Entregas despacharEntrega(int idEntrega) {
        Entregas entrega = buscarEntrega(idEntrega);
        entrega.registrarSalidaBodega(bodega.getBodega());
        Ventas venta = entrega.getVenta();
        Bitacora.registrar("Entregas", "Entrega " + entrega.getNumeroGuia() + " EN_RUTA: salida de "
                + venta.getCantidadTotalItems() + " unidades de bodega. Venta #" + venta.getIdVenta()
                + " FACTURADA con " + venta.getNumeroFactura() + ".");
        bodega.revisarStockMinimo(venta);
        return entrega;
    }

    @Override
    public Entregas actualizarEstado(int idEntrega, EstadoEntrega nuevoEstado) {
        Entregas entrega = buscarEntrega(idEntrega);
        entrega.actualizarEstadoEnvio(nuevoEstado);
        Bitacora.registrar("Entregas", "Entrega " + entrega.getNumeroGuia() + " ahora está " + nuevoEstado);
        return entrega;
    }
}
