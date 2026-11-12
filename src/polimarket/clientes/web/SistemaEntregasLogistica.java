package polimarket.clientes.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import polimarket.componentes.IBodega;
import polimarket.componentes.IEntregas;
import polimarket.componentes.IProveedores;
import polimarket.componentes.IRecursosHumanos;
import polimarket.componentes.PoliMarketComponentes;
import polimarket.modelo.DetalleOrdenCompra;
import polimarket.modelo.DetalleVenta;
import polimarket.modelo.EstadoEntrega;
import polimarket.modelo.Entregas;
import polimarket.modelo.OrdenCompra;
import polimarket.modelo.Producto;
import polimarket.modelo.Proveedor;
import polimarket.modelo.Vendedor;
import polimarket.modelo.Ventas;

/**
 * CLIENTE 2 (plataforma web - HTML/JavaScript en el navegador): SistemaEntregasLogistica.
 * Esta clase publica como API HTTP/JSON las interfaces que el cliente requiere según el
 * diagrama de componentes y sirve la página index.html.
 *
 * IEntregas
 *   GET  /api/ventas-por-entregar                  consultarVentasPorEntregar()
 *   GET  /api/entregas                             listarEntregas()
 *   POST /api/entregas?idVenta=&transportista=     programarEntrega()
 *   POST /api/entregas/despachar?id=               despacharEntrega()   (salida de bodega + factura)
 *   POST /api/entregas/estado?id=&valor=           actualizarEstado()   (ENTREGADA | FALLIDA)
 * IBodega
 *   GET  /api/inventario                           listarInventario()
 *   GET  /api/ordenes                              listarOrdenesCompra()
 *   POST /api/ordenes?idProducto=                  solicitarReabastecimiento()
 *   POST /api/ordenes/recibir?id=                  registrarRecepcion()
 *   POST /api/ordenes/cancelar?id=                 cancelarOrden()
 * IProveedores
 *   POST /api/ordenes/despachar?id=                despacharOrden()
 *   (también obtenerProveedorDe() para mostrar el proveedor de cada producto)
 * IRecursosHumanos
 *   GET  /api/personal                             listarVendedores()  (solo lectura)
 *   (también validarAutorizacionVigente() del vendedor de cada venta)
 */
public class SistemaEntregasLogistica {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PoliMarketComponentes componentes = PoliMarketComponentes.instancia();
    private final IEntregas entregas = componentes.entregas();
    private final IBodega bodega = componentes.bodega();
    private final IProveedores proveedores = componentes.proveedores();
    private final IRecursosHumanos recursosHumanos = componentes.recursosHumanos();
    private HttpServer servidor;

    /** Inicia el servidor en el primer puerto libre a partir de puertoInicial. Devuelve la URL. */
    public String iniciar(int puertoInicial) throws IOException {
        for (int puerto = puertoInicial; puerto < puertoInicial + 20; puerto++) {
            try {
                servidor = HttpServer.create(new InetSocketAddress("localhost", puerto), 0);
                break;
            } catch (BindException ocupado) {
                // probar el siguiente puerto
            }
        }
        if (servidor == null) throw new IOException("No hay puertos libres desde " + puertoInicial);

        servidor.createContext("/", this::paginaPrincipal);
        servidor.createContext("/api/", this::api);
        servidor.start();
        return "http://localhost:" + servidor.getAddress().getPort();
    }

    private void api(HttpExchange ex) throws IOException {
        String ruta = ex.getRequestMethod() + " " + ex.getRequestURI().getPath();
        Map<String, String> q = parametros(ex);
        Supplier<String> operacion = switch (ruta) {
            // IEntregas
            case "GET /api/ventas-por-entregar" -> () -> lista(entregas.consultarVentasPorEntregar(), this::ventaJson);
            case "GET /api/entregas" -> () -> lista(entregas.listarEntregas(), this::entregaJson);
            case "POST /api/entregas" -> () -> entregaJson(
                    entregas.programarEntrega(entero(q, "idVenta"), q.get("transportista")));
            case "POST /api/entregas/despachar" -> () -> entregaJson(entregas.despacharEntrega(entero(q, "id")));
            case "POST /api/entregas/estado" -> () -> entregaJson(
                    entregas.actualizarEstado(entero(q, "id"), estadoEntrega(q.get("valor"))));
            // IBodega + IProveedores
            case "GET /api/inventario" -> () -> lista(bodega.listarInventario(), this::productoJson);
            case "GET /api/ordenes" -> () -> lista(bodega.listarOrdenesCompra(), SistemaEntregasLogistica::ordenJson);
            case "POST /api/ordenes" -> () -> ordenJson(
                    bodega.solicitarReabastecimiento(bodega.buscarProducto(entero(q, "idProducto"))));
            case "POST /api/ordenes/despachar" -> () -> ordenJson(proveedores.despacharOrden(entero(q, "id")));
            case "POST /api/ordenes/recibir" -> () -> ordenJson(bodega.registrarRecepcion(entero(q, "id")));
            case "POST /api/ordenes/cancelar" -> () -> ordenJson(bodega.cancelarOrden(entero(q, "id")));
            // IRecursosHumanos
            case "GET /api/personal" -> () -> lista(recursosHumanos.listarVendedores(), SistemaEntregasLogistica::vendedorJson);
            default -> null;
        };
        if (operacion == null) {
            enviar(ex, 404, "{\"error\":\"Ruta no encontrada: " + ruta + "\"}", "application/json");
        } else {
            responder(ex, operacion);
        }
    }

    // ------------------------------------------------------------------ JSON

    private static <T> String lista(List<T> elementos, Function<T, String> aJson) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < elementos.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(aJson.apply(elementos.get(i)));
        }
        return sb.append(']').toString();
    }

    private String ventaJson(Ventas v) {
        StringBuilder productos = new StringBuilder("[");
        for (DetalleVenta d : v.getDetalles()) {
            if (productos.length() > 1) productos.append(',');
            productos.append("{\"sku\":").append(txt(d.getProducto().getCodigoSku()))
                    .append(",\"nombre\":").append(txt(d.getProducto().getNombreProducto()))
                    .append(",\"cantidad\":").append(d.getCantidad()).append('}');
        }
        productos.append(']');
        Vendedor autor = recursosHumanos.buscarVendedorDeVenta(v);
        String vendedor = autor == null ? "null"
                : "{\"nombre\":" + txt(autor.getNombreCompleto())
                + ",\"vigente\":" + recursosHumanos.validarAutorizacionVigente(autor.getIdVendedor())
                + ",\"autorizacion\":" + txt(autor.getDescripcionAutorizacion()) + "}";
        return "{\"idVenta\":" + v.getIdVenta()
                + ",\"numeroFactura\":" + txt(v.getNumeroFactura())
                + ",\"fecha\":" + txt(v.getFechaHora().format(FECHA_HORA))
                + ",\"cliente\":" + txt(v.getCliente().getNombreCompleto())
                + ",\"telefono\":" + txt(v.getCliente().getTelefono())
                + ",\"direccion\":" + txt(v.getCliente().getDireccionResidencia())
                + ",\"total\":" + Math.round(v.getTotalVenta())
                + ",\"estado\":" + txt(v.getEstadoVenta().name())
                + ",\"vendedor\":" + vendedor
                + ",\"productos\":" + productos + "}";
    }

    private String entregaJson(Entregas e) {
        return "{\"idEntrega\":" + e.getIdEntrega()
                + ",\"numeroGuia\":" + txt(e.getNumeroGuia())
                + ",\"estado\":" + txt(e.getEstadoEntrega().name())
                + ",\"transportista\":" + txt(e.getTransportistaAsignado())
                + ",\"direccion\":" + txt(e.getDireccionDestino())
                + ",\"fechaProgramada\":" + txt(fecha(e.getFechaProgramada(), "yyyy-MM-dd"))
                + ",\"fechaEntregaReal\":" + txt(fecha(e.getFechaEntregaReal(), "yyyy-MM-dd HH:mm"))
                + ",\"venta\":" + ventaJson(e.getVenta()) + "}";
    }

    private String productoJson(Producto p) {
        Proveedor pr = proveedores.obtenerProveedorDe(p);
        return "{\"idProducto\":" + p.getIdProducto()
                + ",\"sku\":" + txt(p.getCodigoSku())
                + ",\"nombre\":" + txt(p.getNombreProducto())
                + ",\"stock\":" + bodega.consultarStock(p)
                + ",\"minimo\":" + p.getStockMinimo()
                + ",\"bajoMinimo\":" + p.requiereReabastecimiento()
                + ",\"proveedor\":" + txt(pr.getNombreEmpresa()) + "}";
    }

    private static String ordenJson(OrdenCompra o) {
        StringBuilder productos = new StringBuilder("[");
        for (DetalleOrdenCompra d : o.getDetalles()) {
            if (productos.length() > 1) productos.append(',');
            productos.append("{\"nombre\":").append(txt(d.getProducto().getNombreProducto()))
                    .append(",\"solicitada\":").append(d.getCantidadSolicitada())
                    .append(",\"recibida\":").append(d.getCantidadRecibida()).append('}');
        }
        productos.append(']');
        return "{\"idOrden\":" + o.getIdOrden()
                + ",\"numeroOrden\":" + txt(o.getNumeroOrden())
                + ",\"estado\":" + txt(o.getEstadoOrden().name())
                + ",\"proveedor\":" + txt(o.getProveedor().getNombreEmpresa())
                + ",\"fechaEmision\":" + txt(fecha(o.getFechaEmision(), "yyyy-MM-dd HH:mm"))
                + ",\"fechaEntregaEsperada\":" + txt(fecha(o.getFechaEntregaEsperada(), "yyyy-MM-dd"))
                + ",\"total\":" + Math.round(o.getTotalOrden())
                + ",\"productos\":" + productos + "}";
    }

    private static String vendedorJson(Vendedor v) {
        return "{\"id\":" + v.getIdVendedor()
                + ",\"nombre\":" + txt(v.getNombreCompleto())
                + ",\"correo\":" + txt(v.getCorreoElectronico())
                + ",\"telefono\":" + txt(v.getTelefono())
                + ",\"vigente\":" + v.tieneAutorizacionVigente()
                + ",\"autorizacion\":" + txt(v.getDescripcionAutorizacion())
                + ",\"autorizadoPor\":" + txt(v.getAutorizadoPor())
                + ",\"ventas\":" + v.getVentas().size() + "}";
    }

    private static EstadoEntrega estadoEntrega(String valor) {
        try {
            return EstadoEntrega.valueOf(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Estado de entrega inválido: " + valor);
        }
    }

    private static String fecha(Date d, String patron) {
        return d == null ? "" : new SimpleDateFormat(patron).format(d);
    }

    private static String txt(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }

    // ------------------------------------------------------------------ HTTP

    /**
     * Ejecuta la operación en el hilo de eventos de Swing para que el cliente web
     * y el de escritorio nunca modifiquen los datos compartidos al mismo tiempo.
     */
    private void responder(HttpExchange ex, Supplier<String> operacion) throws IOException {
        AtomicReference<String> resultado = new AtomicReference<>();
        AtomicReference<RuntimeException> fallo = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    resultado.set(operacion.get());
                } catch (RuntimeException e) {
                    fallo.set(e);
                }
            });
        } catch (Exception e) {
            fallo.set(new IllegalStateException("Error interno: " + e.getMessage()));
        }
        if (fallo.get() != null) {
            enviar(ex, 400, "{\"error\":" + txt(fallo.get().getMessage()) + "}", "application/json");
        } else {
            enviar(ex, 200, resultado.get(), "application/json");
        }
    }

    private void paginaPrincipal(HttpExchange ex) throws IOException {
        if (!"/".equals(ex.getRequestURI().getPath()) && !"/index.html".equals(ex.getRequestURI().getPath())) {
            enviar(ex, 404, "No encontrado", "text/plain");
            return;
        }
        enviar(ex, 200, leerIndexHtml(), "text/html");
    }

    private String leerIndexHtml() throws IOException {
        try (InputStream in = SistemaEntregasLogistica.class.getResourceAsStream("index.html")) {
            if (in != null) return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Path enFuentes = Path.of("src", "polimarket", "clientes", "web", "index.html");
        if (Files.exists(enFuentes)) return Files.readString(enFuentes);
        return "<h1>No se encontró index.html</h1>";
    }

    private static void enviar(HttpExchange ex, int codigo, String cuerpo, String tipo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", tipo + "; charset=utf-8");
        ex.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parametros(HttpExchange ex) {
        Map<String, String> mapa = new HashMap<>();
        String query = ex.getRequestURI().getRawQuery();
        if (query == null || query.isEmpty()) return mapa;
        for (String par : query.split("&")) {
            String[] kv = par.split("=", 2);
            mapa.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return mapa;
    }

    private static int entero(Map<String, String> q, String nombre) {
        try {
            return Integer.parseInt(q.get(nombre));
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Parámetro inválido: " + nombre);
        }
    }

    /** Permite ejecutar solo el servidor del cliente web (sin la app de escritorio). */
    public static void main(String[] args) throws IOException {
        String url = new SistemaEntregasLogistica().iniciar(8080);
        System.out.println("Cliente web de Entregas disponible en " + url);
    }
}
