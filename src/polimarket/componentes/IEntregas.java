package polimarket.componentes;

import java.util.List;

import polimarket.modelo.EstadoEntrega;
import polimarket.modelo.Entregas;
import polimarket.modelo.Ventas;

/** Interfaz provista por EntregasComponent. */
public interface IEntregas {

    List<Ventas> consultarVentasPorEntregar();

    List<Entregas> listarEntregas();

    Entregas buscarEntrega(int idEntrega);

    Entregas programarEntrega(int idVenta, String transportista);

    /** Registra la salida de stock en bodega y factura la venta. */
    Entregas despacharEntrega(int idEntrega);

    Entregas actualizarEstado(int idEntrega, EstadoEntrega nuevoEstado);
}
