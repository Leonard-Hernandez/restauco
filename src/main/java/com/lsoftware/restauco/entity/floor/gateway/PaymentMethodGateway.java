package com.lsoftware.restauco.entity.floor.gateway;

import com.lsoftware.restauco.entity.floor.model.PaymentMethod;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodGateway {
    PaymentMethod save(PaymentMethod method);
    Optional<PaymentMethod> findById(Long id);
    List<PaymentMethod> findAll();
    boolean existsByName(String name, Long exceptId);
}
