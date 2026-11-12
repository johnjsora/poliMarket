package polimarket.modelo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Área de Recursos Humanos de PoliMarket.
 * Relación del diagrama de clases: RecursosHumanos "1" --> "0..*" Vendedor : autoriza
 */
public class RecursosHumanos {

    /** Vigencia de una autorización, en años. */
    public static final int DURACION_AUTORIZACION_ANIOS = 1;

    private int idDepto;
    private String nombreDepartamento;
    private String responsable;
    private String telefonoContacto;

    /** Asociación "autoriza" (0..*). */
    private final List<Vendedor> vendedores = new ArrayList<>();

    public RecursosHumanos(int idDepto, String nombreDepartamento, String responsable, String telefonoContacto) {
        this.idDepto = idDepto;
        this.nombreDepartamento = nombreDepartamento;
        this.responsable = responsable;
        this.telefonoContacto = telefonoContacto;
    }

    /** Autoriza al vendedor por un año a partir de hoy, firmado por el responsable del departamento. */
    public boolean autorizarVendedor(Vendedor v) {
        if (v == null) {
            return false;
        }
        vincularVendedor(v);
        Date hoy = new Date();
        Calendar vence = Calendar.getInstance();
        vence.setTime(hoy);
        vence.add(Calendar.YEAR, DURACION_AUTORIZACION_ANIOS);
        v.registrarAutorizacion(true, hoy, vence.getTime(), responsable);
        return true;
    }

    public boolean revocarAutorizacion(Vendedor v) {
        if (v == null || !vendedores.contains(v)) {
            return false;
        }
        v.registrarAutorizacion(false, v.getFechaAutorizacion(), v.getFechaVencimientoAutorizacion(),
                v.getAutorizadoPor());
        return true;
    }

    public String consultarEstadoPersonal(int idVendedor) {
        Vendedor v = buscar(idVendedor);
        if (v == null) {
            return "Vendedor " + idVendedor + " no registrado en " + nombreDepartamento;
        }
        return v.getNombreCompleto() + " - " + v.getDescripcionAutorizacion();
    }

    public boolean validarAutorizacionVigente(int idVendedor) {
        Vendedor v = buscar(idVendedor);
        return v != null && v.tieneAutorizacionVigente();
    }

    /** Registra al vendedor en el departamento sin autorizarlo. */
    public void vincularVendedor(Vendedor v) {
        if (!vendedores.contains(v)) {
            vendedores.add(v);
        }
    }

    private Vendedor buscar(int idVendedor) {
        for (Vendedor v : vendedores) {
            if (v.getIdVendedor() == idVendedor) return v;
        }
        return null;
    }

    public List<Vendedor> getVendedores() { return vendedores; }
    public int getIdDepto() { return idDepto; }
    public String getNombreDepartamento() { return nombreDepartamento; }
    public String getResponsable() { return responsable; }
    public String getTelefonoContacto() { return telefonoContacto; }
}
