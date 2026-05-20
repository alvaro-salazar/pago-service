package com.denkitronik.pagoservice.domain.services;

import com.denkitronik.pagoservice.delivery.rest.PagoIniciarRequest;
import com.denkitronik.pagoservice.delivery.rest.PagoIniciarResponse;
import com.denkitronik.pagoservice.delivery.rest.ReembolsoRequest;
import com.denkitronik.pagoservice.domain.entities.EstadoPago;
import com.denkitronik.pagoservice.domain.entities.Pago;
import com.denkitronik.pagoservice.domain.repositories.IPagoRepository;
import com.denkitronik.pagoservice.infrastructure.messaging.PagoEventPublisher;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentRefundClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.client.preference.PreferenceTaxRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.payment.PaymentRefund;
import com.mercadopago.resources.preference.Preference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements IPagoService {

    private final IPagoRepository pagoRepository;
    private final PagoEventPublisher eventPublisher;

    @Value("${mp.webhook-url}")
    private String webhookBaseUrl;

    @Override
    @Transactional
    public PagoIniciarResponse iniciarPago(PagoIniciarRequest request) {
        PreferenceItemRequest item = PreferenceItemRequest.builder()
            .title(request.descripcion())
            .quantity(1)
            .unitPrice(request.monto())
            .build();

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
            .success(webhookBaseUrl + "/pago/exitoso")
            .failure(webhookBaseUrl + "/pago/fallido")
            .pending(webhookBaseUrl + "/pago/pendiente")
            .build();

        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
            .items(List.of(item))
            .backUrls(backUrls)
            .notificationUrl(webhookBaseUrl + "/pagos/webhook")
            .externalReference(request.pedidoId().toString())
            .taxes(List.of(PreferenceTaxRequest.builder()
                .type("IVA")
                .value(java.math.BigDecimal.ZERO)
                .build()))
            .build();

        try {
            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);
            log.info("Preferencia MP creada: {} para pedido {}", preference.getId(), request.pedidoId());

            Pago pago = Pago.builder()
                .pedidoId(request.pedidoId())
                .mpPreferenciaId(preference.getId())
                .estado(EstadoPago.CREADO)
                .monto(request.monto())
                .build();
            pago = pagoRepository.save(pago);

            return new PagoIniciarResponse(
                pago.getId(),
                preference.getSandboxInitPoint(),
                EstadoPago.CREADO,
                pago.getMonto()
            );

        } catch (com.mercadopago.exceptions.MPApiException e) {
            log.error("Error MP API para pedido {}: status={}, body={}",
                request.pedidoId(), e.getStatusCode(),
                e.getApiResponse() != null ? e.getApiResponse().getContent() : "null");
            throw new RuntimeException("Error al comunicarse con MercadoPago: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error al crear preferencia MP para pedido {}: {}", request.pedidoId(), e.getMessage());
            throw new RuntimeException("Error al comunicarse con MercadoPago: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void procesarWebhook(Long mpPaymentId) {

        if (pagoRepository.existsByMpPaymentId(mpPaymentId)) {
            log.info("Webhook duplicado para mpPaymentId={}. Ignorando.", mpPaymentId);
            return;
        }

        Payment payment;
        try {
            PaymentClient mpClient = new PaymentClient();
            payment = mpClient.get(mpPaymentId);
        } catch (Exception e) {
            log.error("Error al consultar pago {} en MP: {}", mpPaymentId, e.getMessage());
            throw new RuntimeException("Error al consultar MercadoPago", e);
        }

        Long pedidoId = Long.parseLong(payment.getExternalReference());
        Pago pago = pagoRepository.findByPedidoId(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado para pedido: " + pedidoId));

        EstadoPago nuevoEstado = switch (payment.getStatus()) {
            case "approved" -> EstadoPago.APROBADO;
            case "rejected" -> EstadoPago.RECHAZADO;
            case "pending"  -> EstadoPago.PENDIENTE_MP;
            default -> {
                log.warn("Estado MP desconocido: {}", payment.getStatus());
                yield EstadoPago.PENDIENTE_MP;
            }
        };

        pago.setMpPaymentId(mpPaymentId);
        pago.setEstado(nuevoEstado);
        pago.setFechaActualizacion(LocalDateTime.now());
        pagoRepository.save(pago);
        log.info("Pago {} actualizado a {} para pedido {}", mpPaymentId, nuevoEstado, pedidoId);

        switch (nuevoEstado) {
            case APROBADO  -> eventPublisher.publicarPagoConfirmado(pago.getId(), pedidoId, pago.getMonto());
            case RECHAZADO -> eventPublisher.publicarPagoRechazado(pago.getId(), pedidoId);
            default -> log.info("Estado {} no genera evento Kafka", nuevoEstado);
        }
    }

    @Override
    @Transactional
    public void reembolsar(Long pagoId, ReembolsoRequest request) {
        Pago pago = pagoRepository.findById(pagoId)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado: " + pagoId));

        if (pago.getEstado() != EstadoPago.APROBADO) {
            throw new IllegalStateException(
                "Solo se puede reembolsar un pago en estado APROBADO. Estado actual: " + pago.getEstado());
        }

        if (pago.getMpPaymentId() == null) {
            throw new IllegalStateException("El pago no tiene ID de MercadoPago");
        }

        try {
            PaymentRefundClient refundClient = new PaymentRefundClient();
            BigDecimal montoReembolso = (request != null) ? request.monto() : null;

            PaymentRefund refund = (montoReembolso != null)
                ? refundClient.refund(pago.getMpPaymentId(), montoReembolso)
                : refundClient.refund(pago.getMpPaymentId());
            log.info("Reembolso creado en MP: id={}, status={}", refund.getId(), refund.getStatus());

        } catch (Exception e) {
            log.error("Error al crear reembolso en MP para pago {}: {}", pagoId, e.getMessage());
            throw new RuntimeException("Error al procesar reembolso en MercadoPago: " + e.getMessage(), e);
        }

        BigDecimal montoFinal = (request != null && request.monto() != null)
            ? request.monto()
            : pago.getMonto();

        pago.setEstado(EstadoPago.REEMBOLSADO);
        pago.setFechaActualizacion(LocalDateTime.now());
        pagoRepository.save(pago);

        eventPublisher.publicarPagoReembolsado(pagoId, pago.getPedidoId(), montoFinal);
        log.info("Reembolso de {} procesado para pago {}, pedido {}", montoFinal, pagoId, pago.getPedidoId());
    }
}
