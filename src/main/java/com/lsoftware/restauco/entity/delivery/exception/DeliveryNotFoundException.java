package com.lsoftware.restauco.entity.delivery.exception;

import com.lsoftware.restauco.entity.exception.NotFoundException;

public class DeliveryNotFoundException extends NotFoundException {
    public DeliveryNotFoundException() {
        super("DELIVERY_NOT_FOUND", "Delivery not found");
    }
}
