package rw.ac.rca.cloudsphere.wishlist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rw.ac.rca.cloudsphere.cart.CartService;
import rw.ac.rca.cloudsphere.catalog.Product;
import rw.ac.rca.cloudsphere.catalog.ProductRepository;
import rw.ac.rca.cloudsphere.catalog.ProductStatus;
import rw.ac.rca.cloudsphere.common.exception.ApiException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlists;

    @Mock
    private ProductRepository products;

    @Mock
    private CartService carts;

    @InjectMocks
    private WishlistService wishlistService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAdd_Success() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setStatus(ProductStatus.PUBLISHED);
        product.setStockQty(10);

        when(products.findById(productId)).thenReturn(Optional.of(product));
        when(wishlists.existsByIdUserIdAndIdProductId(userId, productId)).thenReturn(false);

        wishlistService.add(userId, productId);
        verify(wishlists, times(1)).save(any(WishlistItem.class));
    }

    @Test
    void testAdd_OutOfStock() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setStatus(ProductStatus.PUBLISHED);
        product.setStockQty(0);

        when(products.findById(productId)).thenReturn(Optional.of(product));

        assertThrows(ApiException.class, () -> wishlistService.add(userId, productId));
        verify(wishlists, never()).save(any(WishlistItem.class));
    }
}
