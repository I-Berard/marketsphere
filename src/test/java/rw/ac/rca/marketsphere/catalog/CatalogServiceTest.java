package rw.ac.rca.marketsphere.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rw.ac.rca.marketsphere.common.exception.ApiException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogServiceTest {

    @Mock
    private ProductRepository products;

    @Mock
    private CategoryRepository categories;

    @InjectMocks
    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSearch_NoQuery() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setStatus(ProductStatus.PUBLISHED);

        Page<Product> page = new PageImpl<>(List.of(product));
        when(products.findByStatus(eq(ProductStatus.PUBLISHED), any())).thenReturn(page);

        Page<ProductResponse> result = catalogService.search(null, 0, 10);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetPublished_Success() {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setStatus(ProductStatus.PUBLISHED);

        when(products.findByIdAndStatus(productId, ProductStatus.PUBLISHED)).thenReturn(Optional.of(product));

        ProductResponse response = catalogService.getPublished(productId);
        assertNotNull(response);
    }

    @Test
    void testGetPublished_NotFoundOrDraft() {
        UUID productId = UUID.randomUUID();

        when(products.findByIdAndStatus(productId, ProductStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> catalogService.getPublished(productId));
    }
}
