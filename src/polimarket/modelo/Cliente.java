package polimarket.modelo;

/**
 * Cliente atendido en las ventas de PoliMarket.
 */
public class Cliente {

    private int idCliente;
    private String tipoDocumento;
    private String numeroDocumento;
    private String nombres;
    private String apellidos;
    private String correo;
    private String telefono;
    private String direccionResidencia;
    private boolean registrado;

    public Cliente(int idCliente, String tipoDocumento, String numeroDocumento, String nombres,
                   String apellidos, String correo, String telefono, String direccionResidencia) {
        this.idCliente = idCliente;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correo = correo;
        this.telefono = telefono;
        this.direccionResidencia = direccionResidencia;
    }

    public void registrarCliente() {
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            throw new IllegalStateException("El cliente debe tener un número de documento.");
        }
        this.registrado = true;
    }

    public void actualizarDatosContacto(String telefono, String correo) {
        this.telefono = telefono;
        this.correo = correo;
    }

    public String getNombreCompleto() { return nombres + " " + apellidos; }
    public boolean isRegistrado() { return registrado; }
    public int getIdCliente() { return idCliente; }
    public String getTipoDocumento() { return tipoDocumento; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getCorreo() { return correo; }
    public String getTelefono() { return telefono; }
    public String getDireccionResidencia() { return direccionResidencia; }

    @Override
    public String toString() {
        return getNombreCompleto() + " (" + tipoDocumento + " " + numeroDocumento + ")";
    }
}
