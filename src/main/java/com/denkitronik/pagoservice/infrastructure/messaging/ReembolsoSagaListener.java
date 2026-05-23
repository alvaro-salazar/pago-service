package com.denkitronik.pagoservice.infrastructure.messaging;

import com.denkitronik.pagoservice.domain.services.PagoServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReembolsoSagaListener {

    private final PagoServiceImpl pagoService;

    @KafkaListener(
        topics = "pagos.reembolsos.solicitados",
        groupId = "pago-reembolso-group",
        properties = {"spring.json.value.default.type=com.denkitronik.pagoservice.infrastructure.messaging.ReembolsoSolicitadoEvent"}
    )
    public void onReembolsoSolicitado(ReembolsoSolicitadoEvent evento) {
        log.info("Reembolso solicitado para pedido {} (cliente {})",
                 evento.pedidoId(), evento.clienteId());
        pagoService.reembolsarPorPedido(evento.pedidoId());
    }
}
