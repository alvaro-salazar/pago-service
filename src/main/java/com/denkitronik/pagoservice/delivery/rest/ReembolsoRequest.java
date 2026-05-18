package com.denkitronik.pagoservice.delivery.rest;

import java.math.BigDecimal;

public record ReembolsoRequest(
    BigDecimal monto   // null = reembolso total
) {}
