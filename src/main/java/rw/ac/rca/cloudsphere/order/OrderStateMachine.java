package rw.ac.rca.cloudsphere.order;

import rw.ac.rca.cloudsphere.common.exception.ApiException;

public final class OrderStateMachine {

    private OrderStateMachine() {}

    public static void assertTransition(OrderStatus from, OrderStatus to) {
        boolean legal = switch (from) {
            case PLACED -> to == OrderStatus.PAID
                    || to == OrderStatus.CANCELLED
                    || to == OrderStatus.PAYMENT_TIMED_OUT;
            case PAID -> to == OrderStatus.SHIPPED || to == OrderStatus.CANCELLED;
            case SHIPPED -> to == OrderStatus.DELIVERED;
            case PAYMENT_TIMED_OUT -> to == OrderStatus.PAID || to == OrderStatus.CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };
        if (!legal) {
            throw ApiException.conflict("Illegal order transition: " + from + " → " + to);
        }
    }
}
