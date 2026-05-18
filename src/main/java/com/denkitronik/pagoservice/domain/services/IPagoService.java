package com.denkitronik.pagoservice.domain.services;

import com.denkitronik.pagoservice.delivery.rest.PagoIniciarRequest;
import com.denkitronik.pagoservice.delivery.rest.PagoIniciarResponse;
import com.denkitronik.pagoservice.delivery.rest.ReembolsoRequest;

public interface IPagoService {
    PagoIniciarResponse iniciarPago(PagoIniciarRequest request);
    void procesarWebhook(Long mpPaymentId);
    void reembolsar(Long pagoId, ReembolsoRequest request);
}
