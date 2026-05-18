package com.denkitronik.pagoservice.infrastructure.messaging;

public record PagoRechazadoEvent(Long pagoId, Long pedidoId) {}
