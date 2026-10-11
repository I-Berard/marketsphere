package rw.ac.rca.marketsphere.admin;

import jakarta.validation.constraints.Min;

public record StockUpdateRequest(@Min(0) int stockQty) {}
