package rw.ac.rca.cloudsphere.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddItemRequest(@NotNull UUID productId, @Min(1) int qty) {}
