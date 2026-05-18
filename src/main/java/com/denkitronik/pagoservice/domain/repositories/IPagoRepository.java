package com.denkitronik.pagoservice.domain.repositories;

import com.denkitronik.pagoservice.domain.entities.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IPagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByPedidoId(Long pedidoId);
    boolean existsByMpPaymentId(Long mpPaymentId);
}
