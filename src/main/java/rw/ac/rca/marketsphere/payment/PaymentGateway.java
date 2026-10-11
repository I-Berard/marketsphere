package rw.ac.rca.cloudsphere.payment;

import rw.ac.rca.cloudsphere.config.CloudSphereProperties;
import rw.ac.rca.cloudsphere.order.PaymentProvider;
import rw.ac.rca.cloudsphere.order.PaymentStatus;

import java.util.UUID;

public interface PaymentGateway {
    Result charge(PaymentProvider provider, long amountRwf, UUID orderId, CloudSphereProperties.StubMode override);

    record Result(PaymentStatus status, String gatewayRef) {}
}
