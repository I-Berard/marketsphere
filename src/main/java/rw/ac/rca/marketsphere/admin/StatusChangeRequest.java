package rw.ac.rca.cloudsphere.admin;

import jakarta.validation.constraints.NotNull;
import rw.ac.rca.cloudsphere.order.OrderStatus;

public record StatusChangeRequest(@NotNull OrderStatus status) {}
