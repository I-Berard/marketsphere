package rw.ac.rca.cloudsphere.cart;

import jakarta.validation.constraints.Min;

public record UpdateQtyRequest(@Min(0) int qty) {}
