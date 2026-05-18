package com.denkitronik.pagoservice.infrastructure.messaging;

import java.math.BigDecimal;

public record PagoConfirmadoEvent(Long pagoId, Long pedidoId, BigDecimal monto) {}
