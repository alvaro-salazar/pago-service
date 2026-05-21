package com.denkitronik.pagoservice.infrastructure.messaging;

import java.math.BigDecimal;

public record PagoConfirmadoEvent(Long pagoId, Long pedidoId, Long clienteId, BigDecimal monto, String metodoPago) {}
