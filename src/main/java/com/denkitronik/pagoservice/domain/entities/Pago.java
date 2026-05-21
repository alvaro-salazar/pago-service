package com.denkitronik.pagoservice.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pedidoId;

    @Column(nullable = false)
    private Long clienteId;

    @Column(length = 64)
    private String mpPreferenciaId;

    private Long mpPaymentId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoPago estado = EstadoPago.CREADO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    private LocalDateTime fechaActualizacion;
}
