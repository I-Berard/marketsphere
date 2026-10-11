package rw.ac.rca.cloudsphere.cart;

import rw.ac.rca.cloudsphere.identity.LoyaltyTier;

import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID cartId,
        String guestToken,
        List<CartLineResponse> items,
        String discountCode,
        long subtotalRwf,
        long discountRwf,
        long shippingRwf,
        long totalRwf,
        LoyaltyTier pricedAsTier
) {
    public record CartLineResponse(UUID productId, String sku, String name, int qty, long unitPriceRwf, long lineTotalRwf, String imageUrl) {}
}
