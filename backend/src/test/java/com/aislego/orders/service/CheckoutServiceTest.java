package com.aislego.orders.service;

import com.aislego.addresses.domain.Address;
import com.aislego.addresses.repository.AddressRepository;
import com.aislego.catalogue.domain.Branch;
import com.aislego.catalogue.domain.Product;
import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.repository.BranchRepository;
import com.aislego.common.exception.NotFoundException;
import com.aislego.common.money.Money;
import com.aislego.identity.domain.User;
import com.aislego.identity.repository.UserRepository;
import com.aislego.inventory.service.InventoryReservationService;
import com.aislego.inventory.service.StockReservationLine;
import com.aislego.notifications.Notification;
import com.aislego.notifications.NotificationService;
import com.aislego.orders.domain.Cart;
import com.aislego.orders.domain.CartItem;
import com.aislego.orders.domain.Order;
import com.aislego.orders.domain.OrderItem;
import com.aislego.orders.domain.OrderStatus;
import com.aislego.orders.dto.CheckoutRequest;
import com.aislego.orders.dto.OrderResponse;
import com.aislego.orders.repository.CartRepository;
import com.aislego.orders.repository.OrderRepository;
import com.aislego.payments.PaymentIntent;
import com.aislego.payments.PaymentService;
import com.aislego.payments.PaymentVerificationRequest;
import com.aislego.payments.PaymentVerificationResult;
import com.aislego.payments.domain.Payment;
import com.aislego.payments.domain.PaymentStatus;
import com.aislego.payments.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers {@code verifyPayment}'s idempotency guard and the failure path that cancels the
 * order and releases reserved inventory - see {@link CheckoutService#applyVerificationResult}.
 */
@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private InventoryReservationService inventoryReservationService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private NotificationService notificationService;

    private CheckoutService checkoutService;

    private static final Long USER_ID = 9L;
    private static final Long ORDER_ID = 100L;
    private static final Long BRANCH_ID = 20L;
    private static final Long PRODUCT_ID = 30L;

    @BeforeEach
    void setUp() {
        checkoutService = new CheckoutService(cartRepository, orderRepository, branchRepository, userRepository,
                addressRepository, inventoryReservationService, paymentService, paymentRepository,
                notificationService, "mock");
    }

    @Test
    void verifyPaymentIsIdempotentWhenOrderAlreadyConfirmed() {
        Order order = buildOrder(OrderStatus.PAYMENT_CONFIRMED);
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        OrderResponse response = checkoutService.verifyPayment(USER_ID, ORDER_ID,
                new PaymentVerificationRequest(null, null, null));

        assertThat(response.status()).isEqualTo(OrderStatus.PAYMENT_CONFIRMED);
        verify(paymentService, never()).verifyAndCapture(any(), any());
        verify(paymentRepository, never()).findByOrderId(anyLong());
        verify(inventoryReservationService, never()).release(any(), any());
    }

    @Test
    void verifyPaymentIsIdempotentWhenOrderAlreadyCancelled() {
        Order order = buildOrder(OrderStatus.CANCELLED);
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        OrderResponse response = checkoutService.verifyPayment(USER_ID, ORDER_ID,
                new PaymentVerificationRequest(null, null, null));

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(paymentService, never()).verifyAndCapture(any(), any());
    }

    @Test
    void failedVerificationCancelsOrderAndReleasesReservedInventory() {
        Order order = buildOrder(OrderStatus.PLACED);
        Payment payment = buildPendingPayment(order);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
        when(paymentService.verifyAndCapture(eq("MOCK-abc"), any()))
                .thenReturn(new PaymentVerificationResult(false, "Card declined"));

        OrderResponse response = checkoutService.verifyPayment(USER_ID, ORDER_ID,
                new PaymentVerificationRequest(null, "pay_1", "sig"));

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StockReservationLine>> linesCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryReservationService).release(eq(BRANCH_ID), linesCaptor.capture());
        assertThat(linesCaptor.getValue()).containsExactly(new StockReservationLine(PRODUCT_ID, 3));

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).send(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().subject()).isEqualTo("Order cancelled");
    }

    @Test
    void successfulVerificationConfirmsOrderAndDoesNotTouchInventory() {
        Order order = buildOrder(OrderStatus.PLACED);
        Payment payment = buildPendingPayment(order);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
        when(paymentService.verifyAndCapture(eq("MOCK-abc"), any()))
                .thenReturn(new PaymentVerificationResult(true, "Payment confirmed"));

        OrderResponse response = checkoutService.verifyPayment(USER_ID, ORDER_ID,
                new PaymentVerificationRequest(null, null, null));

        assertThat(response.status()).isEqualTo(OrderStatus.PAYMENT_CONFIRMED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(inventoryReservationService, never()).release(any(), any());

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).send(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().subject()).isEqualTo("Payment confirmed");
    }

    @Test
    void checkoutSnapshotsTheSelectedAddressOntoTheOrder() {
        Cart cart = buildCart();
        Branch branch = buildBranch();
        Address address = buildAddress(50L, USER_ID);

        when(orderRepository.findByUserIdAndIdempotencyKey(USER_ID, "key-2")).thenReturn(Optional.empty());
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(branchRepository.findById(BRANCH_ID)).thenReturn(Optional.of(branch));
        when(addressRepository.findByIdAndUserId(50L, USER_ID)).thenReturn(Optional.of(address));
        when(userRepository.getReferenceById(USER_ID)).thenReturn(buildCustomer());
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(ORDER_ID);
            return o;
        });
        when(paymentService.createIntent(any(), any()))
                .thenReturn(new PaymentIntent("MOCK-xyz", false, null, 6000, "INR"));

        checkoutService.checkout(USER_ID, "key-2", new CheckoutRequest(BRANCH_ID, 50L));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getDeliveryAddress()).isEqualTo("221B Baker St, Springfield, ST 12345");
    }

    @Test
    void checkoutRejectsAnAddressBelongingToAnotherCustomer() {
        Cart cart = buildCart();
        Branch branch = buildBranch();

        when(orderRepository.findByUserIdAndIdempotencyKey(USER_ID, "key-3")).thenReturn(Optional.empty());
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));
        when(branchRepository.findById(BRANCH_ID)).thenReturn(Optional.of(branch));
        when(addressRepository.findByIdAndUserId(50L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkoutService.checkout(USER_ID, "key-3", new CheckoutRequest(BRANCH_ID, 50L)))
                .isInstanceOf(NotFoundException.class);
        verify(orderRepository, never()).save(any());
    }

    private User buildCustomer() {
        User user = new User();
        user.setId(USER_ID);
        user.setFullName("Jane Customer");
        user.setEmail("jane@example.com");
        user.setPhone("+15559998888");
        return user;
    }

    private Cart buildCart() {
        Supermarket supermarket = new Supermarket();
        supermarket.setId(1L);

        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setSupermarket(supermarket);
        product.setName("Milk");
        product.setSku("SKU-" + PRODUCT_ID);
        product.setPrice(Money.of(BigDecimal.valueOf(20), "INR"));

        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(3);
        item.setUnitPrice(Money.of(BigDecimal.valueOf(20), "INR"));

        Cart cart = new Cart();
        cart.setSupermarketId(1L);
        cart.getItems().add(item);
        return cart;
    }

    private Branch buildBranch() {
        Supermarket supermarket = new Supermarket();
        supermarket.setId(1L);
        Branch branch = new Branch();
        branch.setId(BRANCH_ID);
        branch.setSupermarket(supermarket);
        return branch;
    }

    private Address buildAddress(Long id, Long userId) {
        User owner = new User();
        owner.setId(userId);
        Address address = new Address();
        address.setId(id);
        address.setUser(owner);
        address.setLabel("Home");
        address.setLine1("221B Baker St");
        address.setCity("Springfield");
        address.setState("ST");
        address.setPostalCode("12345");
        return address;
    }

    private Order buildOrder(OrderStatus status) {
        Supermarket supermarket = new Supermarket();
        supermarket.setId(1L);

        Branch branch = new Branch();
        branch.setId(BRANCH_ID);

        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setSupermarket(supermarket);
        product.setName("Milk");
        product.setSku("SKU-" + PRODUCT_ID);
        product.setPrice(Money.of(BigDecimal.valueOf(20), "INR"));

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setProductName("Milk");
        item.setQuantity(3);
        item.setUnitPrice(Money.of(BigDecimal.valueOf(20), "INR"));
        item.setLineTotal(Money.of(BigDecimal.valueOf(60), "INR"));

        User user = new User();
        user.setId(USER_ID);
        user.setFullName("Jane Customer");
        user.setEmail("jane@example.com");
        user.setPhone("+15559998888");

        Order order = new Order();
        order.setId(ORDER_ID);
        order.setUser(user);
        order.setSupermarket(supermarket);
        order.setBranch(branch);
        order.setStatus(status);
        order.setTotalAmount(Money.of(BigDecimal.valueOf(60), "INR"));
        order.setIdempotencyKey("key-1");
        order.getItems().add(item);
        return order;
    }

    private Payment buildPendingPayment(Order order) {
        Payment payment = new Payment();
        payment.setId(500L);
        payment.setOrderId(order.getId());
        payment.setGatewayReference("MOCK-abc");
        payment.setProvider("MOCK");
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        return payment;
    }
}
