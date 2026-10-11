package rw.ac.rca.cloudsphere.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rw.ac.rca.cloudsphere.cart.Cart;
import rw.ac.rca.cloudsphere.cart.CartItem;
import rw.ac.rca.cloudsphere.cart.CartRepository;
import rw.ac.rca.cloudsphere.cart.CartResponse;
import rw.ac.rca.cloudsphere.cart.CartService;
import rw.ac.rca.cloudsphere.catalog.Product;
import rw.ac.rca.cloudsphere.catalog.ProductRepository;
import rw.ac.rca.cloudsphere.catalog.ProductStatus;
import rw.ac.rca.cloudsphere.common.exception.ApiException;
import rw.ac.rca.cloudsphere.identity.User;
import rw.ac.rca.cloudsphere.identity.UserRepository;
import rw.ac.rca.cloudsphere.payment.ChargeResult;
import rw.ac.rca.cloudsphere.payment.PaymentGateway;

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
        CheckoutRequest request = new CheckoutRequest("MTN_MOMO", "SUCCESS");

        when(users.findById(userId)).thenReturn(Optional.of(new User()));
        Cart cart = new Cart(userId);
        when(cartService.requireOwnedCart(userId)).thenReturn(cart);

        ApiException exception = assertThrows(ApiException.class, () -> orderService.checkout(userId, request, null));
        assertEquals(400, exception.getStatus().value());
    }

    @Test
    void testCheckout_Success() {
        UUID userId = UUID.randomUUID();
        CheckoutRequest request = new CheckoutRequest("MTN_MOMO", "SUCCESS");

        User user = new User();
        user.setId(userId);
        when(users.findById(userId)).thenReturn(Optional.of(user));

        Cart cart = new Cart(userId);
        UUID productId = UUID.randomUUID();
        cart.getItems().add(new CartItem(productId, 2, 1000));
        when(cartService.requireOwnedCart(userId)).thenReturn(cart);

        Product product = new Product();
        product.setId(productId);
        product.setStatus(ProductStatus.PUBLISHED);
        product.setStockQty(10);
        when(products.findById(productId)).thenReturn(Optional.of(product));

        CartResponse cartResponse = new CartResponse(List.of(), 2000, 0, 0, 2000);
        when(cartService.toResponse(cart, userId)).thenReturn(cartResponse);

        ChargeResult chargeResult = new ChargeResult(rw.ac.rca.cloudsphere.order.PaymentStatus.SUCCEEDED, "REF123");
        when(payments.charge(anyString(), anyInt(), any(), any())).thenReturn(chargeResult);

        when(orders.save(any(ShopOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.checkout(userId, request, null);
        assertNotNull(response);
        assertEquals(OrderStatus.PAID, response.status());
        verify(orders, times(1)).save(any(ShopOrder.class));
        verify(carts, times(1)).save(cart);
        assertEquals(8, product.getStockQty());
    }
}
