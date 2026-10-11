package rw.ac.rca.marketsphere.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rw.ac.rca.marketsphere.cart.Cart;
import rw.ac.rca.marketsphere.cart.CartItem;
import rw.ac.rca.marketsphere.cart.CartRepository;
import rw.ac.rca.marketsphere.cart.CartResponse;
import rw.ac.rca.marketsphere.cart.CartService;
import rw.ac.rca.marketsphere.catalog.Product;
import rw.ac.rca.marketsphere.catalog.ProductRepository;
import rw.ac.rca.marketsphere.catalog.ProductStatus;
import rw.ac.rca.marketsphere.common.exception.ApiException;
import rw.ac.rca.marketsphere.identity.User;
import rw.ac.rca.marketsphere.identity.UserRepository;
import rw.ac.rca.marketsphere.payment.PaymentGateway;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    @Mock
    private OrderRepository orders;
    @Mock
    private CartService cartService;
    @Mock
    private CartRepository carts;
    @Mock
    private ProductRepository products;
    @Mock
    private UserRepository users;
    @Mock
    private PaymentGateway payments;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCheckout_EmptyCart() {
        UUID userId = UUID.randomUUID();
        CheckoutRequest request = new CheckoutRequest(PaymentProvider.MTN_MOMO, "SUCCESS");

        User user = new User();
        user.setId(userId);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUserId(userId);
        when(cartService.requireOwnedCart(userId)).thenReturn(cart);

        ApiException exception = assertThrows(ApiException.class, () -> orderService.checkout(userId, request, null));
        assertEquals(400, exception.status().value());
    }

    @Test
    void testCheckout_Success() {
        UUID userId = UUID.randomUUID();
        CheckoutRequest request = new CheckoutRequest(PaymentProvider.MTN_MOMO, "SUCCESS");

        User user = new User();
        user.setId(userId);
        when(users.findById(userId)).thenReturn(Optional.of(user));

        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUserId(userId);
        UUID productId = UUID.randomUUID();
        cart.getItems().add(new CartItem(productId, 2, 1000));
        when(cartService.requireOwnedCart(userId)).thenReturn(cart);

        Product product = new Product();
        product.setId(productId);
        product.setStatus(ProductStatus.PUBLISHED);
        product.setStockQty(10);
        when(products.findById(productId)).thenReturn(Optional.of(product));

        CartResponse cartResponse = new CartResponse(UUID.randomUUID(), null, List.of(), null, 2000, 0, 0, 2000, rw.ac.rca.marketsphere.identity.LoyaltyTier.STANDARD);
        when(cartService.toResponse(cart, userId)).thenReturn(cartResponse);

        PaymentGateway.Result chargeResult = new PaymentGateway.Result(rw.ac.rca.marketsphere.order.PaymentStatus.SUCCEEDED, "REF123");
        when(payments.charge(any(), anyLong(), any(), any())).thenReturn(chargeResult);

        when(orders.save(any(ShopOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.checkout(userId, request, null);
        assertNotNull(response);
        assertEquals(OrderStatus.PAID, response.status());
        verify(orders, times(1)).save(any(ShopOrder.class));
        verify(carts, times(1)).save(cart);
        assertEquals(8, product.getStockQty());
    }
}
