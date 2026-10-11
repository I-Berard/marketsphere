package rw.ac.rca.cloudsphere.cart;

import jakarta.validation.constraints.NotBlank;

public record ApplyDiscountRequest(@NotBlank String code) {}
