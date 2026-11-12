package polimarket.clientes.escritorio;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import polimarket.componentes.IBodega;
import polimarket.componentes.IRecursosHumanos;
import polimarket.componentes.IVentas;
import polimarket.componentes.PoliMarketComponentes;
import polimarket.datos.Bitacora;
import polimarket.modelo.Cliente;
import polimarket.modelo.Producto;
import polimarket.modelo.Vendedor;
import polimarket.modelo.Ventas;

/**
 * CLIENTE 1 (plataforma de escritorio - Java Swing): ModuloVendedoresApp.
 * Consume, según el diagrama de componentes:
 *   - IRecursosHumanos: autorizar / revocar / validar la autorización vigente de los vendedores.
 *   - IVentas: clientes, productos disponibles, disponibilidad, registro y anulación de ventas.
 *   - IBodega: inventario y productos bajo el stock mínimo.
 */
public class ModuloVendedoresApp extends JFrame {

    private static final SimpleDateFormat FECHA = new SimpleDateFormat("yyyy-MM-dd");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final IRecursosHumanos rh = PoliMarketComponentes.instancia().recursosHumanos();
    private final IVentas ventas = PoliMarketComponentes.instancia().ventas();
    private final IBodega bodega = PoliMarketComponentes.instancia().bodega();
    private final String urlClienteWeb;

    private Vendedor vendedorEnSesion;
    private final Map<Producto, Integer> carrito = new LinkedHashMap<>();
    private List<Producto> productosMostrados = List.of();

    // Encabezado
    private final JComboBox<Vendedor> cbVendedor = new JComboBox<>();
    private final JLabel lblSesion = new JLabel();

    // Recursos Humanos
    private final DefaultTableModel mVendedores = modelo("ID", "Nombre", "Correo", "Ingreso", "Autorización", "Autorizado por", "Fecha autorización");
    private final JTable tVendedores = tabla(mVendedores);

    // Ventas
    private final JComboBox<Cliente> cbCliente = new JComboBox<>();
    private final DefaultTableModel mProductos = modelo("SKU", "Producto", "Precio", "Stock bodega", "Disponible");
    private final JTable tProductos = tabla(mProductos);
    private final JSpinner spCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    private final DefaultTableModel mCarrito = modelo("SKU", "Producto", "Cant.", "Precio", "Subtotal");
    private final JTable tCarrito = tabla(mCarrito);
    private final JLabel lblSubtotal = new JLabel();
    private final JLabel lblIva = new JLabel();
    private final JLabel lblTotal = new JLabel();
    private final JLabel lblAvisoVentas = new JLabel();
    private final JPanel panelVentas = new JPanel(new BorderLayout(8, 8));

    // Historial
    private final DefaultTableModel mHistorial = modelo("#", "Factura", "Fecha", "Vendedor", "Cliente", "Unid.", "Total", "Estado");
    private final JTable tHistorial = tabla(mHistorial);

    // Inventario
    private final DefaultTableModel mInventario = modelo("SKU", "Producto", "Stock", "Mínimo", "Disponible para venta", "Estado");
    private final JTable tInventario = tabla(mInventario);
    private final JLabel lblBajoMinimo = new JLabel();

    // Bitácora
    private final JTextArea txtBitacora = new JTextArea();

    public ModuloVendedoresApp(String urlClienteWeb) {
        super("PoliMarket - Módulo de Vendedores (Cliente de escritorio)");
        this.urlClienteWeb = urlClienteWeb;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 640));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("1. Recursos Humanos", construirPanelRecursosHumanos());
        tabs.addTab("2. Registrar venta", construirPanelVentas());
        tabs.addTab("3. Historial de ventas", construirPanelHistorial());
        tabs.addTab("4. Inventario", construirPanelInventario());
        tabs.addTab("5. Bitácora del sistema", construirPanelBitacora());
        tabs.addChangeListener(e -> refrescarTodo());

        add(construirEncabezado(), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        for (String linea : Bitacora.getEventos()) txtBitacora.append(linea + "\n");
        Bitacora.suscribir(linea -> SwingUtilities.invokeLater(() -> {
            txtBitacora.append(linea + "\n");
            refrescarTodo();
        }));

        refrescarTodo();
        actualizarSesion();
        pack();
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------ Encabezado

    private JPanel construirEncabezado() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));

        JLabel titulo = new JLabel("PoliMarket · Módulo de Vendedores");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        p.add(titulo, BorderLayout.WEST);

        JPanel sesion = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        for (Vendedor v : rh.listarVendedores()) cbVendedor.addItem(v);
        JButton btnIngresar = new JButton("Iniciar sesión");
        btnIngresar.addActionListener(e -> iniciarSesion());
        JButton btnSalir = new JButton("Cerrar sesión");
        btnSalir.addActionListener(e -> { vendedorEnSesion = null; carrito.clear(); actualizarSesion(); });
        JButton btnWeb = new JButton("Abrir cliente web de Entregas");
        btnWeb.addActionListener(e -> abrirClienteWeb());

        sesion.add(new JLabel("Vendedor:"));
        sesion.add(cbVendedor);
        sesion.add(btnIngresar);
        sesion.add(btnSalir);
        sesion.add(btnWeb);
        p.add(sesion, BorderLayout.EAST);

        lblSesion.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        p.add(lblSesion, BorderLayout.SOUTH);
        return p;
    }

    private void iniciarSesion() {
        Vendedor v = (Vendedor) cbVendedor.getSelectedItem();
        if (v == null) return;
        if (!rh.validarAutorizacionVigente(v.getIdVendedor())) {
            error(v.getNombreCompleto() + " no tiene autorización vigente ("
                    + v.getDescripcionAutorizacion() + ").\n"
                    + "Autorícelo en la pestaña \"1. Recursos Humanos\".");
            return;
        }
        vendedorEnSesion = v;
        carrito.clear();
        actualizarSesion();
    }

    private void actualizarSesion() {
        if (vendedorEnSesion != null && !rh.validarAutorizacionVigente(vendedorEnSesion.getIdVendedor())) {
            vendedorEnSesion = null; // RH revocó la autorización o venció
            carrito.clear();
        }
        boolean activa = vendedorEnSesion != null;
        lblSesion.setText(activa
                ? "Sesión activa: " + vendedorEnSesion.getNombreCompleto() + " · autorización "
                        + vendedorEnSesion.getDescripcionAutorizacion()
                : "Sin sesión. Seleccione un vendedor autorizado e inicie sesión para vender.");
        lblSesion.setForeground(activa ? new Color(0, 120, 60) : new Color(170, 60, 0));
        lblAvisoVentas.setText(activa ? " " : "Debe iniciar sesión con un vendedor autorizado para registrar ventas.");
        habilitar(panelVentas, activa);
        refrescarTodo();
    }

    private void abrirClienteWeb() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(urlClienteWeb));
                return;
            }
        } catch (Exception ignorada) {
            // se muestra la URL abajo
        }
        JOptionPane.showMessageDialog(this, "Abra en su navegador: " + urlClienteWeb);
    }

    // ------------------------------------------------------------------ RF1: Recursos Humanos

    private JPanel construirPanelRecursosHumanos() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(new JLabel("RF1 · Recursos Humanos autoriza a los vendedores por "
                + polimarket.modelo.RecursosHumanos.DURACION_AUTORIZACION_ANIOS
                + " año para interactuar con los demás sistemas. Solo venden quienes tienen autorización VIGENTE."),
                BorderLayout.NORTH);
        p.add(new JScrollPane(tVendedores), BorderLayout.CENTER);

        JButton autorizar = new JButton("Autorizar / renovar");
        autorizar.addActionListener(e -> {
            Integer id = idVendedorSeleccionado();
            if (id != null) { rh.autorizarVendedor(id); actualizarSesion(); }
        });
        JButton revocar = new JButton("Revocar autorización");
        revocar.addActionListener(e -> {
            Integer id = idVendedorSeleccionado();
            if (id != null) { rh.revocarAutorizacion(id); actualizarSesion(); }
        });
        JButton consultar = new JButton("Consultar estado");
        consultar.addActionListener(e -> {
            Integer id = idVendedorSeleccionado();
            if (id != null) JOptionPane.showMessageDialog(this, rh.consultarEstadoPersonal(id));
        });
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.add(autorizar);
        botones.add(revocar);
        botones.add(consultar);
        p.add(botones, BorderLayout.SOUTH);
        return p;
    }

    private Integer idVendedorSeleccionado() {
        int fila = tVendedores.getSelectedRow();
        if (fila < 0) { error("Seleccione un vendedor de la tabla."); return null; }
        return (Integer) mVendedores.getValueAt(fila, 0);
    }

    // ------------------------------------------------------------------ RF2 / RF3: Ventas

    private JPanel construirPanelVentas() {
        panelVentas.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel norte = new JPanel(new BorderLayout());
        JPanel selCliente = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (Cliente c : ventas.listarClientes()) cbCliente.addItem(c);
        selCliente.add(new JLabel("Cliente:"));
        selCliente.add(cbCliente);
        norte.add(selCliente, BorderLayout.WEST);
        lblAvisoVentas.setForeground(new Color(170, 60, 0));
        norte.add(lblAvisoVentas, BorderLayout.SOUTH);

        // Catálogo
        JPanel catalogo = new JPanel(new BorderLayout(4, 4));
        catalogo.setBorder(BorderFactory.createTitledBorder("Productos disponibles (RF3: disponibilidad en bodega)"));
        catalogo.add(new JScrollPane(tProductos), BorderLayout.CENTER);
        JPanel agregar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAgregar = new JButton("Agregar a la venta");
        btnAgregar.addActionListener(e -> agregarAlCarrito());
        agregar.add(new JLabel("Cantidad:"));
        agregar.add(spCantidad);
        agregar.add(btnAgregar);
        catalogo.add(agregar, BorderLayout.SOUTH);

        // Carrito
        JPanel detalle = new JPanel(new BorderLayout(4, 4));
        detalle.setBorder(BorderFactory.createTitledBorder("Detalle de la venta (RF2)"));
        detalle.add(new JScrollPane(tCarrito), BorderLayout.CENTER);
        JPanel totales = new JPanel(new GridLayout(0, 1));
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 15f));
        totales.add(lblSubtotal);
        totales.add(lblIva);
        totales.add(lblTotal);
        JButton quitar = new JButton("Quitar producto");
        quitar.addActionListener(e -> quitarDelCarrito());
        JButton vaciar = new JButton("Vaciar");
        vaciar.addActionListener(e -> { carrito.clear(); refrescarCarrito(); });
        JButton registrar = new JButton("Registrar venta");
        registrar.setFont(registrar.getFont().deriveFont(Font.BOLD));
        registrar.addActionListener(e -> registrarVenta());
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acciones.add(quitar);
        acciones.add(vaciar);
        acciones.add(registrar);
        JPanel sur = new JPanel(new BorderLayout());
        sur.add(totales, BorderLayout.WEST);
        sur.add(acciones, BorderLayout.SOUTH);
        detalle.add(sur, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, catalogo, detalle);
        split.setResizeWeight(0.55);

        panelVentas.add(norte, BorderLayout.NORTH);
        panelVentas.add(split, BorderLayout.CENTER);
        return panelVentas;
    }

    private void agregarAlCarrito() {
        int fila = tProductos.getSelectedRow();
        if (fila < 0) { error("Seleccione un producto del catálogo."); return; }
        Producto p = productosMostrados.get(tProductos.convertRowIndexToModel(fila));
        int cantidad = (Integer) spCantidad.getValue();
        int yaEnCarrito = carrito.getOrDefault(p, 0);
        int disponible = ventas.consultarDisponibilidad(p);
        if (yaEnCarrito + cantidad > disponible) {
            error("Disponibilidad insuficiente de " + p.getNombreProducto()
                    + ".\nDisponible: " + disponible + " · En la venta: " + yaEnCarrito);
            return;
        }
        carrito.merge(p, cantidad, Integer::sum);
        refrescarCarrito();
    }

    private void quitarDelCarrito() {
        int fila = tCarrito.getSelectedRow();
        if (fila < 0) { error("Seleccione un producto del detalle."); return; }
        carrito.remove(carrito.keySet().toArray(new Producto[0])[fila]);
        refrescarCarrito();
    }

    private void registrarVenta() {
        Cliente cliente = (Cliente) cbCliente.getSelectedItem();
        try {
            Ventas v = ventas.registrarVenta(vendedorEnSesion, cliente, carrito);
            carrito.clear();
            refrescarTodo();
            JOptionPane.showMessageDialog(this, String.format(
                    "Venta #%d CONFIRMADA.%nCliente: %s%nTotal: $%,.0f%n%n"
                    + "Ya aparece en el cliente web de Entregas. Se facturará al salir de bodega.",
                    v.getIdVenta(), cliente.getNombreCompleto(), v.getTotalVenta()),
                    "Venta registrada", JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException ex) {
            error(ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ Historial

    private JPanel construirPanelHistorial() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(new JLabel("CONFIRMADA: vendida, pendiente de salir de bodega · FACTURADA: despachada por Entregas · ANULADA: cancelada."),
                BorderLayout.NORTH);
        p.add(new JScrollPane(tHistorial), BorderLayout.CENTER);
        JButton cancelar = new JButton("Anular venta seleccionada");
        cancelar.addActionListener(e -> {
            int fila = tHistorial.getSelectedRow();
            if (fila < 0) { error("Seleccione una venta."); return; }
            try {
                ventas.cancelarVenta((Integer) mHistorial.getValueAt(fila, 0));
            } catch (RuntimeException ex) {
                error(ex.getMessage());
            }
        });
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sur.add(cancelar);
        p.add(sur, BorderLayout.SOUTH);
        return p;
    }

    // ------------------------------------------------------------------ Inventario (IBodega)

    private JPanel construirPanelInventario() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(new JLabel("RF3 · Existencias de la bodega. \"Disponible para venta\" descuenta las ventas CONFIRMADAS que aún no salen."),
                BorderLayout.NORTH);
        p.add(new JScrollPane(tInventario), BorderLayout.CENTER);
        lblBajoMinimo.setForeground(new Color(170, 60, 0));
        p.add(lblBajoMinimo, BorderLayout.SOUTH);
        return p;
    }

    private JPanel construirPanelBitacora() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        txtBitacora.setEditable(false);
        txtBitacora.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        p.add(new JLabel("Eventos de todos los componentes (incluye órdenes de compra de Bodega a Proveedores)."),
                BorderLayout.NORTH);
        p.add(new JScrollPane(txtBitacora), BorderLayout.CENTER);
        return p;
    }

    // ------------------------------------------------------------------ Refresco de datos

    private void refrescarTodo() {
        refrescarVendedores();
        refrescarProductos();
        refrescarCarrito();
        refrescarHistorial();
        refrescarInventario();
    }

    private void refrescarVendedores() {
        int sel = tVendedores.getSelectedRow();
        mVendedores.setRowCount(0);
        for (Vendedor v : rh.listarVendedores()) {
            mVendedores.addRow(new Object[]{v.getIdVendedor(), v.getNombreCompleto(), v.getCorreoElectronico(),
                    FECHA.format(v.getFechaIngreso()), v.getDescripcionAutorizacion(),
                    v.getAutorizadoPor() == null ? "-" : v.getAutorizadoPor(),
                    v.getFechaAutorizacion() == null ? "-" : FECHA.format(v.getFechaAutorizacion())});
        }
        reseleccionar(tVendedores, sel);
    }

    private void refrescarProductos() {
        int sel = tProductos.getSelectedRow();
        mProductos.setRowCount(0);
        productosMostrados = vendedorEnSesion == null
                ? List.of()
                : ventas.listarProductosDisponibles(vendedorEnSesion);
        for (Producto p : productosMostrados) {
            mProductos.addRow(new Object[]{p.getCodigoSku(), p.getNombreProducto(), pesos(p.getPrecioUnitario()),
                    p.getStockActual(), ventas.consultarDisponibilidad(p)});
        }
        reseleccionar(tProductos, sel);
    }

    private void refrescarCarrito() {
        mCarrito.setRowCount(0);
        float subtotal = 0f;
        for (Map.Entry<Producto, Integer> d : carrito.entrySet()) {
            float linea = d.getKey().getPrecioUnitario() * d.getValue();
            subtotal += linea;
            mCarrito.addRow(new Object[]{d.getKey().getCodigoSku(), d.getKey().getNombreProducto(), d.getValue(),
                    pesos(d.getKey().getPrecioUnitario()), pesos(linea)});
        }
        float iva = subtotal * Ventas.TASA_IVA;
        lblSubtotal.setText("Subtotal: " + pesos(subtotal));
        lblIva.setText("IVA (19%): " + pesos(iva));
        lblTotal.setText("TOTAL: " + pesos(subtotal + iva));
    }

    private void refrescarHistorial() {
        int sel = tHistorial.getSelectedRow();
        mHistorial.setRowCount(0);
        for (Ventas v : ventas.listarVentas()) {
            Vendedor autor = rh.buscarVendedorDeVenta(v);
            mHistorial.addRow(new Object[]{v.getIdVenta(), v.getNumeroFactura() == null ? "-" : v.getNumeroFactura(),
                    v.getFechaHora().format(FECHA_HORA), autor == null ? "-" : autor.getNombreCompleto(),
                    v.getCliente().getNombreCompleto(), v.getCantidadTotalItems(),
                    pesos(v.getTotalVenta()), v.getEstadoVenta()});
        }
        reseleccionar(tHistorial, sel);
    }

    private void refrescarInventario() {
        int sel = tInventario.getSelectedRow();
        mInventario.setRowCount(0);
        for (Producto p : bodega.listarInventario()) {
            mInventario.addRow(new Object[]{p.getCodigoSku(), p.getNombreProducto(), bodega.consultarStock(p),
                    p.getStockMinimo(), ventas.consultarDisponibilidad(p),
                    p.requiereReabastecimiento() ? "BAJO MÍNIMO" : "OK"});
        }
        reseleccionar(tInventario, sel);
        List<Producto> bajos = bodega.listarProductosBajoMinimo();
        StringBuilder sb = new StringBuilder();
        for (Producto p : bajos) sb.append(sb.length() == 0 ? "" : ", ").append(p.getNombreProducto());
        lblBajoMinimo.setText(bajos.isEmpty()
                ? "Ningún producto bajo el stock mínimo."
                : "Bajo el stock mínimo (" + bajos.size() + "): " + sb + ". Las órdenes de compra se gestionan en el cliente web.");
    }

    // ------------------------------------------------------------------ Utilidades

    private static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private static JTable tabla(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setRowHeight(22);
        t.getTableHeader().setReorderingAllowed(false);
        return t;
    }

    private static void reseleccionar(JTable t, int fila) {
        if (fila >= 0 && fila < t.getRowCount()) t.setRowSelectionInterval(fila, fila);
    }

    private static void habilitar(java.awt.Container c, boolean activo) {
        for (java.awt.Component hijo : c.getComponents()) {
            if (hijo == null) continue;
            if (!(hijo instanceof JLabel)) hijo.setEnabled(activo);
            if (hijo instanceof java.awt.Container cont) habilitar(cont, activo);
        }
    }

    private static String pesos(float valor) {
        return String.format("$%,.0f", valor);
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "PoliMarket", JOptionPane.WARNING_MESSAGE);
    }

    public static void mostrar(String urlClienteWeb) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignorada) {
                // se usa el look and feel por defecto
            }
            new ModuloVendedoresApp(urlClienteWeb).setVisible(true);
        });
    }

    /** Permite abrir solo el cliente de escritorio. */
    public static void main(String[] args) {
        mostrar("http://localhost:8080");
    }
}
