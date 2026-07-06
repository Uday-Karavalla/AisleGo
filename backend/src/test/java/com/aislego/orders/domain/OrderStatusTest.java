package com.aislego.orders.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the declared order of {@link OrderStatus} against accidental reordering: several
 * places in the platform (and any future "is this status before/after that one" check)
 * would silently misbehave if the enum's ordinal order stopped matching the workflow from
 * the product spec.
 */
class OrderStatusTest {

    @Test
    void declaresValuesInWorkflowOrder() {
        OrderStatus[] expectedOrder = {
                OrderStatus.PLACED,
                OrderStatus.PAYMENT_CONFIRMED,
                OrderStatus.ACCEPTED_BY_STORE,
                OrderStatus.PICKING,
                OrderStatus.SUBSTITUTION_APPROVAL,
                OrderStatus.PACKING,
                OrderStatus.READY_FOR_PICKUP,
                OrderStatus.DELIVERY_PARTNER_ASSIGNED,
                OrderStatus.PICKED_UP,
                OrderStatus.OUT_FOR_DELIVERY,
                OrderStatus.DELIVERED,
                OrderStatus.CANCELLED
        };

        assertThat(OrderStatus.values()).containsExactly(expectedOrder);
    }

    @Test
    void placedComesBeforePaymentConfirmed() {
        assertThat(OrderStatus.PLACED.ordinal()).isLessThan(OrderStatus.PAYMENT_CONFIRMED.ordinal());
    }

    @Test
    void cancelledIsTheLastDeclaredStatus() {
        OrderStatus[] values = OrderStatus.values();
        assertThat(values[values.length - 1]).isEqualTo(OrderStatus.CANCELLED);
    }
}
