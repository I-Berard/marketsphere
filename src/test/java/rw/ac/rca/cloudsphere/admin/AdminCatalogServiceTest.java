package rw.ac.rca.cloudsphere.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rw.ac.rca.cloudsphere.catalog.Category;
import rw.ac.rca.cloudsphere.catalog.CategoryRepository;
import rw.ac.rca.cloudsphere.catalog.Product;
import rw.ac.rca.cloudsphere.catalog.ProductRepository;
import rw.ac.rca.cloudsphere.catalog.ProductResponse;
import rw.ac.rca.cloudsphere.catalog.ProductStatus;
import rw.ac.rca.cloudsphere.common.exception.ApiException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminCatalogServiceTest {

    @Mock
    private ProductRepository products;

    @Mock
    private CategoryRepository categories;

    @InjectMocks
    private AdminCatalogService adminService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateProduct_Success() {
        UpsertProductRequest request = new UpsertProductRequest(
                "SKU-001", "Product", "Desc", 1000, 10, ProductStatus.PUBLISHED, "Electronics", null
        );

        when(products.findBySkuIgnoreCase("SKU-001")).thenReturn(Optional.empty());
        when(categories.findByNameIgnoreCase("Electronics")).thenReturn(Optional.empty());
        when(categories.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));
        when(products.save(any(Product.class))).thenAnswer(i -> {
            Product p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        ProductResponse response = adminService.createProduct(request);
        assertNotNull(response);
        assertEquals("SKU-001", response.sku());
        verify(products, times(1)).save(any(Product.class));
    }

    @Test
    void testUpdateProductStock() {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setStockQty(5);

        when(products.findById(productId)).thenReturn(Optional.of(product));
        when(products.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        ProductResponse response = adminService.updateStock(productId, new StockUpdateRequest(20));
        assertEquals(20, product.getStockQty());
    }
}
