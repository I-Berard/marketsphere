package rw.ac.rca.marketsphere.payment;

import rw.ac.rca.marketsphere.config.MarketSphereProperties;
import rw.ac.rca.marketsphere.order.PaymentProvider;
import rw.ac.rca.marketsphere.order.PaymentStatus;

import java.util.UUID;

public interface PaymentGateway {
    Result charge(PaymentProvider provider, long amountRwf, UUID orderId, MarketSphereProperties.StubMode override);

    record Result(PaymentStatus status, String gatewayRef) {}
}
