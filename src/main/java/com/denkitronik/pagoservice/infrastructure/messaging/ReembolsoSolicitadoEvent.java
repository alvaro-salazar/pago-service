package com.denkitronik.pagoservice.infrastructure.messaging;

public record ReembolsoSolicitadoEvent(Long pedidoId, Long clienteId) {}
