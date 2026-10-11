package rw.ac.rca.marketsphere.cart;

import jakarta.validation.constraints.NotBlank;

public record ApplyDiscountRequest(@NotBlank String code) {}
