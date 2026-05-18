package com.denkitronik.pagoservice.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Component
public class WebhookSignatureValidator {

    @Value("${mp.webhook-secret}")
    private String webhookSecret;

    public void validate(String xSignature, String xRequestId, String dataId) {
        if (xSignature == null || xSignature.isBlank()) {
            log.warn("Webhook sin x-signature — rechazado");
            throw new SecurityException("Firma ausente");
        }

        String ts = null, v1 = null;
        for (String part : xSignature.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2) {
                if ("ts".equals(kv[0])) ts = kv[1];
                if ("v1".equals(kv[0])) v1 = kv[1];
            }
        }

        if (ts == null || v1 == null) {
            throw new SecurityException("Formato de x-signature invalido");
        }

        String manifest = "id:" + dataId + ";" +
                          "request-id:" + (xRequestId != null ? xRequestId : "") + ";" +
                          "ts:" + ts + ";";

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = HexFormat.of().formatHex(digest);

            if (!calculatedSignature.equals(v1)) {
                log.warn("Firma invalida. Esperada: {} Recibida: {}", calculatedSignature, v1);
                throw new SecurityException("Firma invalida");
            }
            log.debug("Firma del webhook verificada correctamente");

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error al calcular firma", e);
        }
    }
}
