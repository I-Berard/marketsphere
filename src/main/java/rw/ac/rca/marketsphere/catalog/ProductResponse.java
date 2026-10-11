package rw.ac.rca.cloudsphere.catalog;

import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String description,
        long priceRwf,
        int stockQty,
        ProductStatus status,
        String stockBadge,
        UUID categoryId,
        String categoryName,
        String imageUrl
) {
    public static ProductResponse publicView(Product p) {
        return new ProductResponse(
                p.getId(), p.getSku(), p.getName(), p.getDescription(),
                p.getPriceRwf(), p.getStockQty(), p.getStatus(),
                p.isInStock() ? "IN_STOCK" : "OUT_OF_STOCK",
                p.getCategory() == null ? null : p.getCategory().getId(),
                p.getCategory() == null ? null : p.getCategory().getName(),
                p.getImageUrl()
        );
    }
}
