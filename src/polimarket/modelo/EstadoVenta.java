package polimarket.modelo;

/** Ciclo de vida de una venta. */
public enum EstadoVenta {
    /** En construcción: se están agregando productos. */
    BORRADOR,
    /** Registrada por el vendedor; sus unidades quedan comprometidas en bodega. */
    CONFIRMADA,
    /** Facturada al registrar Entregas la salida de bodega. */
    FACTURADA,
    /** Cancelada antes de salir de bodega. */
    ANULADA
}
