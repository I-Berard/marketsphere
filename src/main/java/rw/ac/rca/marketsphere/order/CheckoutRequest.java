package rw.ac.rca.marketsphere.order;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull PaymentProvider paymentMethod,
        String stubMode
) {}
