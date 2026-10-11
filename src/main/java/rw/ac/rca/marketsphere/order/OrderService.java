package rw.ac.rca.cloudsphere.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.rca.cloudsphere.cart.Cart;
import rw.ac.rca.cloudsphere.cart.CartItem;
import rw.ac.rca.cloudsphere.cart.CartRepository;
import rw.ac.rca.cloudsphere.cart.CartResponse;
import rw.ac.rca.cloudsphere.cart.CartService;
import rw.ac.rca.cloudsphere.catalog.Product;
import rw.ac.rca.cloudsphere.catalog.ProductRepository;
import rw.ac.rca.cloudsphere.catalog.ProductStatus;
import rw.ac.rca.cloudsphere.common.exception.ApiException;
import rw.ac.rca.cloudsphere.config.CloudSphereProperties;
import rw.ac.rca.cloudsphere.identity.UserRepository;
import rw.ac.rca.cloudsphere.payment.PaymentGateway;

import java.time.Instant;
import java.time.Year;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final CartService cartService;
    private final CartRepository carts;
    private final ProductRepository products;
    private final UserRepository users;
    private final PaymentGateway payments;

    public OrderService(OrderRepository orders, CartService cartService, CartRepository carts,
                        ProductRepository products, UserRepository users, PaymentGateway payments) {
        this.orders = orders;
        this.cartService = cartService;
        this.carts = carts;
        this.products = products;
        this.users = users;
        this.payments = payments;
    }

    @Transactional
    public OrderResponse checkout(UUID userId, CheckoutRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = orders.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return OrderResponse.from(existing.get());
            }
        }
        users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Authentication is required."));
        Cart cart = cartService.requireOwnedCart(userId);
        if (cart.getItems().isEmpty()) {
            throw ApiException.badRequest("Cart is empty.");
        }
        for (CartItem item : cart.getItems()) {
            Product product = products.findById(item.getProductId())
                    .orElseThrow(() -> ApiException.conflict("A cart item is no longer available."));
            if (product.getStatus() != ProductStatus.PUBLISHED) {
                throw ApiException.conflict("A cart item is no longer available.");
            }
            if (item.getQty() > product.getStockQty()) {
                throw ApiException.conflict("Item " + product.getSku() + " went out of stock.");
            }
        }
        CartResponse priced = cartService.toResponse(cart, userId);

        ShopOrder order = new ShopOrder();
        order.setId(UUID.randomUUID());
        order.setUserId(userId);
        order.setOrderNumber(nextOrderNumber());
        order.setIdempotencyKey(blankToNull(idempotencyKey));
        Instant now = Instant.now();
        order.setPlacedAt(now);
        order.setUpdatedAt(now);
        order.setSubtotalRwf(priced.subtotalRwf());
        order.setDiscountRwf(priced.discountRwf());
        order.setShippingRwf(priced.shippingRwf());
        order.setTotalRwf(priced.totalRwf());
        for (CartItem item : cart.getItems()) {
            Product product = products.findById(item.getProductId()).orElseThrow();
            OrderLine line = new OrderLine();
            line.setProductId(product.getId());
            line.setSku(product.getSku());
            line.setName(product.getName());
            line.setQty(item.getQty());
            line.setUnitPriceRwf(item.getUnitPriceRwf());
            order.addLine(line);
        }

        CloudSphereProperties.StubMode override = parseStub(request.stubMode());
        var charge = payments.charge(request.paymentMethod(), priced.totalRwf(), order.getId(), override);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setProvider(request.paymentMethod());
        payment.setAmountRwf(priced.totalRwf());
        payment.setStatus(charge.status());
        payment.setGatewayRef(charge.gatewayRef());
        order.setPayment(payment);

        if (charge.status() == PaymentStatus.SUCCEEDED) {
            for (CartItem item : cart.getItems()) {
                decrement(item.getProductId(), item.getQty());
            }
            order.setStatus(OrderStatus.PAID);
            cart.getItems().clear();
            cart.setDiscountCode(null);
            carts.save(cart);
        } else if (charge.status() == PaymentStatus.TIMED_OUT) {
            order.setStatus(OrderStatus.PAYMENT_TIMED_OUT);
        } else {
            order.setStatus(OrderStatus.PLACED);
        }
        orders.save(order);
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getForUser(UUID userId, UUID orderId) {
        ShopOrder order = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw ApiException.notFound("Order not found");
        }
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public java.util.List<OrderResponse> listForUser(UUID userId) {
        return orders.findByUserIdOrderByPlacedAtDesc(userId).stream().map(OrderResponse::from).toList();
    }

    @Transactional
    public OrderResponse retryPayment(UUID userId, UUID orderId, CheckoutRequest request) {
        ShopOrder order = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw ApiException.notFound("Order not found");
        }
        if (order.getStatus() != OrderStatus.PAYMENT_TIMED_OUT && order.getStatus() != OrderStatus.PLACED) {
            throw ApiException.conflict("Order is not awaiting payment.");
        }
        CloudSphereProperties.StubMode override = parseStub(request.stubMode());
        var charge = payments.charge(request.paymentMethod(), order.getTotalRwf(), order.getId(), override);
        if (order.getPayment() != null) {
            order.getPayment().setStatus(charge.status());
            order.getPayment().setProvider(request.paymentMethod());
            order.getPayment().setGatewayRef(charge.gatewayRef());
        }
        if (charge.status() == PaymentStatus.SUCCEEDED) {
            for (OrderLine line : order.getLines()) {
                decrement(line.getProductId(), line.getQty());
            }
            OrderStateMachine.assertTransition(order.getStatus(), OrderStatus.PAID);
            order.setStatus(OrderStatus.PAID);
        } else if (charge.status() == PaymentStatus.TIMED_OUT) {
            order.setStatus(OrderStatus.PAYMENT_TIMED_OUT);
        }
        return OrderResponse.from(orders.save(order));
    }

    @Transactional
    public OrderResponse ship(UUID orderId, String trackingNo) {
        if (trackingNo == null || trackingNo.isBlank()) {
            throw ApiException.badRequest("Tracking number is required.");
        }
        ShopOrder order = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order not found"));
        OrderStateMachine.assertTransition(order.getStatus(), OrderStatus.SHIPPED);
        order.setStatus(OrderStatus.SHIPPED);
        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setTrackingNo(trackingNo.trim());
        shipment.setState(ShipmentState.SHIPPED);
        shipment.setShippedAt(Instant.now());
        order.setShipment(shipment);
        return OrderResponse.from(orders.save(order));
    }

    @Transactional
    public OrderResponse deliver(UUID orderId) {
        ShopOrder order = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order not found"));
        OrderStateMachine.assertTransition(order.getStatus(), OrderStatus.DELIVERED);
        order.setStatus(OrderStatus.DELIVERED);
        if (order.getShipment() != null) {
            order.getShipment().setState(ShipmentState.DELIVERED);
            order.getShipment().setDeliveredAt(Instant.now());
        }
        return OrderResponse.from(orders.save(order));
    }

    @Transactional
    public OrderResponse forceTransition(UUID orderId, OrderStatus to) {
        ShopOrder order = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order not found"));
        OrderStateMachine.assertTransition(order.getStatus(), to);
        order.setStatus(to);
        return OrderResponse.from(orders.save(order));
    }

    private void decrement(UUID productId, int qty) {
        Product product = products.findById(productId)
                .orElseThrow(() -> ApiException.conflict("Item went out of stock during checkout."));
        if (product.getStockQty() < qty) {
            throw ApiException.conflict("Item " + product.getSku() + " went out of stock during checkout.");
        }
        product.setStockQty(product.getStockQty() - qty);
    }

    private static String nextOrderNumber() {
        int n = ThreadLocalRandom.current().nextInt(100_000, 999_999);
        return "CS-" + Year.now() + "-" + n;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static CloudSphereProperties.StubMode parseStub(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return CloudSphereProperties.StubMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Unknown stubMode. Use SUCCESS, DECLINE or TIMEOUT.");
        }
    }
}
