package com.denkitronik.pagoservice.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class PagoEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publicarPagoConfirmado(Long pagoId, Long pedidoId, Long clienteId, BigDecimal monto) {
        var payload = new PagoConfirmadoEvent(pagoId, pedidoId, clienteId, monto, "MercadoPago");
        enviar("pagos.confirmados", pedidoId.toString(), EventoBase.of("PagoConfirmado", payload));
    }

    public void publicarPagoRechazado(Long pagoId, Long pedidoId, Long clienteId) {
        var payload = new PagoRechazadoEvent(pagoId, pedidoId, clienteId,
            "El pago fue rechazado por el procesador. Verifica tus datos e intenta nuevamente.");
        enviar("pagos.rechazados", pedidoId.toString(), EventoBase.of("PagoRechazado", payload));
    }

    public void publicarPagoReembolsado(Long pagoId, Long pedidoId, BigDecimal monto) {
        var payload = new PagoReembolsadoEvent(pagoId, pedidoId, monto);
        enviar("pagos.reembolsados", pedidoId.toString(), EventoBase.of("PagoReembolsado", payload));
    }

    private void enviar(String topic, String key, Object mensaje) {
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(topic, key, mensaje);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Evento publicado en {}: key={}, offset={}",
                    topic, key, result.getRecordMetadata().offset());
            } else {
                log.error("Error al publicar en {}: {}", topic, ex.getMessage());
            }
        });
    }
}
