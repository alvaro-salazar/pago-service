package com.denkitronik.pagoservice.infrastructure.messaging;

import java.math.BigDecimal;

public record PagoReembolsadoEvent(Long pagoId, Long pedidoId, BigDecimal montoReembolsado) {}
