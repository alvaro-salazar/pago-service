package com.denkitronik.pagoservice.delivery.rest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PagoIniciarRequest(
    @NotNull Long pedidoId,
    @NotNull @Positive BigDecimal monto,
    @NotNull @Size(min = 3, max = 200) String descripcion
) {}
