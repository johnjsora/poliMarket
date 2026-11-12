package polimarket.componentes;

import java.util.List;

import polimarket.modelo.Vendedor;
import polimarket.modelo.Ventas;

/** Interfaz provista por RecursosHumanosFacade. */
public interface IRecursosHumanos {

    List<Vendedor> listarVendedores();

    Vendedor buscarVendedor(int idVendedor);

    /** Vendedor que realizó la venta, o null. */
    Vendedor buscarVendedorDeVenta(Ventas venta);

    boolean autorizarVendedor(int idVendedor);

    boolean revocarAutorizacion(int idVendedor);

    boolean validarAutorizacionVigente(int idVendedor);

    String consultarEstadoPersonal(int idVendedor);
}
