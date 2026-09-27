package rw.ac.rca.cloudsphere.order;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull PaymentProvider paymentMethod,
        String stubMode
) {}
