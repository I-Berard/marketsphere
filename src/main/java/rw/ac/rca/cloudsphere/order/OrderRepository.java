package rw.ac.rca.cloudsphere.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<ShopOrder, UUID> {

    @EntityGraph(attributePaths = {"lines", "payment", "shipment"})
    List<ShopOrder> findByUserIdOrderByPlacedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"lines", "payment", "shipment"})
    Optional<ShopOrder> findByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = {"lines", "payment", "shipment"})
    Optional<ShopOrder> findByOrderNumber(String orderNumber);

    @Override
    @EntityGraph(attributePaths = {"lines", "payment", "shipment"})
    Optional<ShopOrder> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = {"lines", "payment", "shipment"})
    List<ShopOrder> findAll();
}
