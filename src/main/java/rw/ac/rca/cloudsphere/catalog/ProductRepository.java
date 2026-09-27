package rw.ac.rca.cloudsphere.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCase(String sku);

    Page<Product> findByStatusAndNameContainingIgnoreCase(ProductStatus status, String name, Pageable pageable);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Optional<Product> findByIdAndStatus(UUID id, ProductStatus status);

    @Modifying
    @Query("""
            update Product p set p.stockQty = p.stockQty - :qty
            where p.id = :id and p.stockQty >= :qty
            """)
    int decrementStock(@Param("id") UUID id, @Param("qty") int qty);

    @Modifying
    @Query("update Product p set p.stockQty = p.stockQty + :qty where p.id = :id")
    int incrementStock(@Param("id") UUID id, @Param("qty") int qty);
}
