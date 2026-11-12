package polimarket.datos;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Registro de eventos del sistema. Los clientes se suscriben para mostrar
 * en pantalla lo que hacen los componentes (ventas, despachos, reabastecimientos...).
 */
public final class Bitacora {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final List<String> EVENTOS = new CopyOnWriteArrayList<>();
    private static final List<Consumer<String>> OYENTES = new CopyOnWriteArrayList<>();

    private Bitacora() { }

    public static void registrar(String componente, String mensaje) {
        String linea = "[" + LocalTime.now().format(HORA) + "] [" + componente + "] " + mensaje;
        EVENTOS.add(linea);
        System.out.println(linea);
        for (Consumer<String> o : OYENTES) {
            o.accept(linea);
        }
    }

    public static void suscribir(Consumer<String> oyente) { OYENTES.add(oyente); }
    public static List<String> getEventos() { return EVENTOS; }
}
