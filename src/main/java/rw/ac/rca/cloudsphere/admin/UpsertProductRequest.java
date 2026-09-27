package rw.ac.rca.cloudsphere.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import rw.ac.rca.cloudsphere.catalog.ProductStatus;

public record UpsertProductRequest(
        @NotBlank String sku,
        @NotBlank String name,
        String description,
        @Min(0) long priceRwf,
        @Min(0) int stockQty,
        @NotNull ProductStatus status,
        String category,
        String imageUrl
) {}
