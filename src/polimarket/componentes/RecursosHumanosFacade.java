package polimarket.componentes;

import java.util.List;

import polimarket.datos.Bitacora;
import polimarket.datos.RepositorioPoliMarket;
import polimarket.modelo.RecursosHumanos;
import polimarket.modelo.Vendedor;
import polimarket.modelo.Ventas;

/**
 * Componente de negocio de Recursos Humanos (provee IRecursosHumanos).
 * RF1: Autorizar / revocar vendedores y validar que su autorización esté vigente.
 */
public class RecursosHumanosFacade implements IRecursosHumanos {

    private final RepositorioPoliMarket repo = RepositorioPoliMarket.instancia();
    private final RecursosHumanos rh = repo.getRecursosHumanos();

    @Override
    public List<Vendedor> listarVendedores() {
        return rh.getVendedores();
    }

    @Override
    public Vendedor buscarVendedor(int idVendedor) {
        for (Vendedor v : rh.getVendedores()) {
            if (v.getIdVendedor() == idVendedor) return v;
        }
        throw new IllegalArgumentException("No existe el vendedor " + idVendedor);
    }

    @Override
    public Vendedor buscarVendedorDeVenta(Ventas venta) {
        for (Vendedor v : rh.getVendedores()) {
            if (v.getVentas().contains(venta)) return v;
        }
        return null;
    }

    @Override
    public boolean autorizarVendedor(int idVendedor) {
        Vendedor v = buscarVendedor(idVendedor);
        v.solicitarAutorizacion(rh);
        Bitacora.registrar("RecursosHumanos", "Vendedor autorizado: " + v.getNombreCompleto()
                + " (" + v.getDescripcionAutorizacion() + ", por " + v.getAutorizadoPor() + ")");
        return v.tieneAutorizacionVigente();
    }

    @Override
    public boolean revocarAutorizacion(int idVendedor) {
        Vendedor v = buscarVendedor(idVendedor);
        boolean ok = rh.revocarAutorizacion(v);
        Bitacora.registrar("RecursosHumanos", "Autorización revocada: " + v.getNombreCompleto());
        return ok;
    }

    @Override
    public boolean validarAutorizacionVigente(int idVendedor) {
        return rh.validarAutorizacionVigente(idVendedor);
    }

    @Override
    public String consultarEstadoPersonal(int idVendedor) {
        return rh.consultarEstadoPersonal(idVendedor);
    }
}
