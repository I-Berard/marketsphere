package rw.ac.rca.cloudsphere.cart;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rw.ac.rca.cloudsphere.catalog.Product;
import rw.ac.rca.cloudsphere.catalog.ProductRepository;
import rw.ac.rca.cloudsphere.catalog.ProductStatus;
import rw.ac.rca.cloudsphere.commerce.Pricing;
import rw.ac.rca.cloudsphere.common.exception.ApiException;
import rw.ac.rca.cloudsphere.config.CloudSphereProperties;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CartServiceTest {

    @Mock
    private CartRepository carts;
    @Mock
    private ProductRepository products;
    @Mock
    private Pricing pricing;
    @Mock
    private CloudSphereProperties props;

    @InjectMocks
    private CartService cartService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAddItem_ProductNotFound() {
        UUID userId = UUID.randomUUID();
        AddItemRequest request = new AddItemRequest(UUID.randomUUID(), 1);

        when(products.findById(request.productId())).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> cartService.addItem(userId, null, request));
        assertEquals(404, exception.getStatus().value());
    }

    @Test
    void testAddItem_ProductNotPublished() {
        UUID userId = UUID.randomUUID();
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setStatus(ProductStatus.DRAFT);
        AddItemRequest request = new AddItemRequest(product.getId(), 1);

        when(products.findById(request.productId())).thenReturn(Optional.of(product));

        ApiException exception = assertThrows(ApiException.class, () -> cartService.addItem(userId, null, request));
        assertEquals(404, exception.getStatus().value());
    }

    @Test
    void testAddItem_InsufficientStock() {
        UUID userId = UUID.randomUUID();
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setStatus(ProductStatus.PUBLISHED);
        product.setStockQty(0);
        AddItemRequest request = new AddItemRequest(product.getId(), 1);

        when(products.findById(request.productId())).thenReturn(Optional.of(product));

        ApiException exception = assertThrows(ApiException.class, () -> cartService.addItem(userId, null, request));
        assertEquals(409, exception.getStatus().value());
    }

    @Test
    void testAddItem_Success() {
        UUID userId = UUID.randomUUID();
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setStatus(ProductStatus.PUBLISHED);
        product.setStockQty(10);
        product.setPriceRwf(1000);
        AddItemRequest request = new AddItemRequest(product.getId(), 1);

        Cart cart = new Cart(userId);
        
        when(products.findById(request.productId())).thenReturn(Optional.of(product));
        when(carts.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(carts.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = cartService.addItem(userId, null, request);
        assertNotNull(response);
        verify(carts, times(1)).save(cart);
    }
}
