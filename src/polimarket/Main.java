package polimarket;

import polimarket.clientes.escritorio.ModuloVendedoresApp;
import polimarket.clientes.web.SistemaEntregasLogistica;
import polimarket.datos.Bitacora;

/**
 * Punto de entrada de la demostración PoliMarket.
 * Levanta los dos clientes sobre los mismos componentes:
 *   1. SistemaEntregasLogistica -> cliente web (navegador) en http://localhost:8080
 *   2. ModuloVendedoresApp      -> cliente de escritorio (Java Swing)
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String url = new SistemaEntregasLogistica().iniciar(8080);
        Bitacora.registrar("Sistema", "Cliente web de Entregas disponible en " + url);
        ModuloVendedoresApp.mostrar(url);
    }
}
