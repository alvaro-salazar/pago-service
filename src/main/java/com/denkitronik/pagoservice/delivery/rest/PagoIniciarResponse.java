package com.denkitronik.pagoservice.delivery.rest;

import com.denkitronik.pagoservice.domain.entities.EstadoPago;
import java.math.BigDecimal;

public record PagoIniciarResponse(
    Long pagoId,
    String checkoutUrl,
    EstadoPago estado,
    BigDecimal monto
) {}
