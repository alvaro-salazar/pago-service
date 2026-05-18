package com.denkitronik.pagoservice.infrastructure.config;

import com.mercadopago.MercadoPagoConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MercadoPagoConfiguration {

    @Value("${mp.access-token}")
    private String accessToken;

    @PostConstruct
    public void configure() {
        MercadoPagoConfig.setAccessToken(accessToken);
        log.info("MercadoPago SDK configurado para ambiente: {}",
            accessToken.startsWith("TEST-") || accessToken.startsWith("APP_USR-")
                ? "SANDBOX" : "PRODUCCION");
    }
}
