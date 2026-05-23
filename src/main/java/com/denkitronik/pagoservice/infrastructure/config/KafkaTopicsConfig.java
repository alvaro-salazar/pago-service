package com.denkitronik.pagoservice.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic topicPagosConfirmados() {
        return TopicBuilder.name("pagos.confirmados").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic topicPagosRechazados() {
        return TopicBuilder.name("pagos.rechazados").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic topicPagosReembolsados() {
        return TopicBuilder.name("pagos.reembolsados").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic topicPagosReembolsosSolicitados() {
        return TopicBuilder.name("pagos.reembolsos.solicitados").partitions(3).replicas(1).build();
    }
}
