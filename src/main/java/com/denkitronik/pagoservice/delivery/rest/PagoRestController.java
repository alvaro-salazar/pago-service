package com.denkitronik.pagoservice.delivery.rest;

import com.denkitronik.pagoservice.domain.services.IPagoService;
import com.denkitronik.pagoservice.infrastructure.config.WebhookSignatureValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class PagoRestController {

    private final IPagoService pagoService;
    private final WebhookSignatureValidator signatureValidator;

    @PostMapping
    public ResponseEntity<PagoIniciarResponse> iniciarPago(
        @Valid @RequestBody PagoIniciarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.iniciarPago(request));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
        @RequestHeader(value = "x-signature", required = false) String xSignature,
        @RequestHeader(value = "x-request-id", required = false) String xRequestId,
        @RequestBody Map<String, Object> body) {

        String type = (String) body.get("type");
        if (!"payment".equals(type)) {
            return ResponseEntity.ok().build();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        String dataId = data.get("id").toString();

        signatureValidator.validate(xSignature, xRequestId, dataId);

        pagoService.procesarWebhook(Long.parseLong(dataId));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reembolso")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reembolsar(
        @PathVariable Long id,
        @RequestBody(required = false) ReembolsoRequest request) {
        pagoService.reembolsar(id, request);
        return ResponseEntity.ok().build();
    }
}
