package rw.ac.rca.cloudsphere.order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        OrderStatus status,
        long subtotalRwf,
        long discountRwf,
        long shippingRwf,
        long totalRwf,
        Instant placedAt,
        PaymentView payment,
        ShipmentView shipment,
        List<LineView> lines
) {
    public record PaymentView(PaymentProvider provider, PaymentStatus status, String gatewayRef, long amountRwf) {}
    public record ShipmentView(String trackingNo, ShipmentState state) {}
    public record LineView(UUID productId, String sku, String name, int qty, long unitPriceRwf) {}

    public static OrderResponse from(ShopOrder order) {
        PaymentView pay = order.getPayment() == null ? null : new PaymentView(
                order.getPayment().getProvider(),
                order.getPayment().getStatus(),
                order.getPayment().getGatewayRef(),
                order.getPayment().getAmountRwf()
        );
        ShipmentView ship = order.getShipment() == null ? null : new ShipmentView(
                order.getShipment().getTrackingNo(),
                order.getShipment().getState()
        );
        var lines = order.getLines().stream()
                .map(l -> new LineView(l.getProductId(), l.getSku(), l.getName(), l.getQty(), l.getUnitPriceRwf()))
                .toList();
        return new OrderResponse(
                order.getId(), order.getOrderNumber(), order.getStatus(),
                order.getSubtotalRwf(), order.getDiscountRwf(), order.getShippingRwf(), order.getTotalRwf(),
                order.getPlacedAt(), pay, ship, lines
        );
    }
}
