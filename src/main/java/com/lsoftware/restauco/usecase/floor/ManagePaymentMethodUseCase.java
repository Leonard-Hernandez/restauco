package com.lsoftware.restauco.usecase.floor;

import com.lsoftware.restauco.entity.floor.exception.DuplicatePaymentMethodException;
import com.lsoftware.restauco.entity.floor.exception.PaymentMethodNotFoundException;
import com.lsoftware.restauco.entity.floor.gateway.PaymentMethodGateway;
import com.lsoftware.restauco.entity.floor.model.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** The ways a restaurant accepts payment, defined by its owner (Efectivo, Nequi...). */
@Component
@RequiredArgsConstructor
public class ManagePaymentMethodUseCase {

    private final PaymentMethodGateway gateway;

    @Transactional(readOnly = true)
    public List<PaymentMethod> list() {
        return gateway.findAll();
    }

    @Transactional(readOnly = true)
    public List<PaymentMethod> listActive() {
        return gateway.findAll().stream().filter(PaymentMethod::active).toList();
    }

    @Transactional
    public PaymentMethod create(String name, int position, boolean active) {
        String clean = name.strip();
        if (gateway.existsByName(clean, null)) throw new DuplicatePaymentMethodException();
        return gateway.save(new PaymentMethod(null, clean, position, active));
    }

    @Transactional
    public PaymentMethod update(Long id, String name, int position, boolean active) {
        gateway.findById(id).orElseThrow(PaymentMethodNotFoundException::new);
        String clean = name.strip();
        if (gateway.existsByName(clean, id)) throw new DuplicatePaymentMethodException();
        return gateway.save(new PaymentMethod(id, clean, position, active));
    }
}
