package rw.ac.rca.marketsphere.admin;

import jakarta.validation.constraints.NotNull;
import rw.ac.rca.marketsphere.order.OrderStatus;

public record StatusChangeRequest(@NotNull OrderStatus status) {}
