package com.aislego.orders.service;

import com.aislego.addresses.domain.Address;
import com.aislego.addresses.repository.AddressRepository;
import com.aislego.catalogue.domain.Branch;
import com.aislego.catalogue.repository.BranchRepository;
import com.aislego.common.exception.BadRequestException;
import com.aislego.common.exception.ConflictException;
import com.aislego.common.exception.ForbiddenException;
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
import com.aislego.orders.dto.CheckoutResponse;
import com.aislego.orders.dto.OrderResponse;
import com.aislego.orders.dto.PaymentIntentResponse;
import com.aislego.orders.repository.CartRepository;
import com.aislego.orders.repository.OrderRepository;
import com.aislego.payments.PaymentIntent;
import com.aislego.payments.PaymentService;
import com.aislego.payments.PaymentVerificationRequest;
import com.aislego.payments.PaymentVerificationResult;
import com.aislego.payments.domain.Payment;
import com.aislego.payments.domain.PaymentStatus;
import com.aislego.payments.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Turns a cart into a placed order and drives it through the two-phase payment flow (see
 * {@link com.aislego.payments.PaymentService}). Requires an {@code Idempotency-Key} (see
 * {@link com.aislego.orders.web.CheckoutController}) so retries after a dropped connection
 * are safe: a repeat call with the same key returns the order already created instead of
 * double-reserving stock or creating a second gateway order.
 *
 * <p>{@link #checkout} only gets as far as reserving stock, creating the {@code Order}
 * (still {@code PLACED}) and a {@code Payment} (still {@code PENDING}), and asking the
 * gateway for an intent - it never marks a payment succeeded/failed itself. That happens in
 * {@link #verifyPayment} (client-driven) or {@link #applyVerificationResult} (also used by
 * {@link com.aislego.payments.web.RazorpayWebhookController} as defense-in-depth), which is
 * the single place that applies a verification outcome to an order+payment: on success the
 * order is confirmed, on failure the order is cancelled and reserved stock is released.
 *
 * <p>Note on idempotency under true concurrency: the pre-check below covers the common
 * "client retried after a timeout" case. Two genuinely simultaneous requests with the same
 * key racing each other rely on the DB's unique {@code (user_id, idempotency_key)}
 * constraint (V1 migration) to reject the loser with a 409 rather than create two orders -
 * that response can then be retried by the client and would return the sole winning order.
 */
@Service
public class CheckoutService {

    private static final BigDecimal MINOR_UNITS_PER_MAJOR_UNIT = BigDecimal.valueOf(100);

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final InventoryReservationService inventoryReservationService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final String provider;

    public CheckoutService(CartRepository cartRepository, OrderRepository orderRepository,
                            BranchRepository branchRepository, UserRepository userRepository,
                            AddressRepository addressRepository,
                            InventoryReservationService inventoryReservationService,
                            PaymentService paymentService, PaymentRepository paymentRepository,
                            NotificationService notificationService,
                            @Value("${aislego.payments.provider}") String provider) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.inventoryReservationService = inventoryReservationService;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
        this.provider = provider.toUpperCase();
    }

    @Transactional
    public CheckoutResponse checkout(Long userId, String idempotencyKey, CheckoutRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Account no longer exists"));
        if (!user.isEmailVerified()) {
            throw new ForbiddenException("EMAIL_NOT_VERIFIED", "Please verify your email before placing an order");
        }

        Optional<Order> existing = orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existing.isPresent()) {
            Order order = existing.get();
            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
            return new CheckoutResponse(OrderResponse.from(order),
                    payment == null ? null : replayPaymentIntent(order, payment));
        }

        Cart cart = cartRepository.findByUserId(userId)
                .filter(c -> !c.isEmpty())
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));

        Branch branch = branchRepository.findById(request.branchId())
                .orElseThrow(() -> new NotFoundException("Branch " + request.branchId() + " was not found"));

        if (!branch.getSupermarket().getId().equals(cart.getSupermarketId())) {
            throw new ConflictException("BRANCH_SUPERMARKET_MISMATCH",
                    "The selected branch does not belong to the supermarket your cart's items are from");
        }

        inventoryReservationService.reserveForOrder(branch.getId(), toReservationLines(cart));

        String deliveryAddress = resolveDeliveryAddress(userId, request.addressId());

        Order order = buildOrder(userId, idempotencyKey, cart, branch, deliveryAddress);
        order = orderRepository.save(order);

        PaymentIntentResponse paymentResponse = createPayment(order);

        cart.getItems().clear();
        cart.setSupermarketId(null);
        cartRepository.save(cart);

        notifyOrder(order, "Order placed",
                "Your order #" + order.getId() + " has been placed and is awaiting payment confirmation.");

        return new CheckoutResponse(OrderResponse.from(order), paymentResponse);
    }

    /**
     * Verifies a client-reported payment outcome and applies it. Idempotent: if the order is
     * already in a terminal state (payment already confirmed or cancelled), returns it as-is
     * without re-invoking the gateway or re-mutating inventory.
     */
    @Transactional
    public OrderResponse verifyPayment(Long userId, Long orderId, PaymentVerificationRequest request) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order " + orderId + " was not found"));

        if (isTerminal(order.getStatus())) {
            return OrderResponse.from(order);
        }

        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new NotFoundException("Payment for order " + orderId + " was not found"));

        PaymentVerificationResult result = paymentService.verifyAndCapture(payment.getGatewayReference(), request);
        return applyVerificationResult(order, payment, result);
    }

    /**
     * The single place a verification outcome is applied to an order+payment - used by both
     * {@link #verifyPayment} and {@link com.aislego.payments.web.RazorpayWebhookController}'s
     * defense-in-depth path, so there is exactly one implementation of "what happens on
     * success/failure," not two copies that could drift apart. Idempotent for the same reason
     * as {@link #verifyPayment}.
     */
    @Transactional
    public OrderResponse applyVerificationResult(Order order, Payment payment, PaymentVerificationResult result) {
        if (isTerminal(order.getStatus())) {
            return OrderResponse.from(order);
        }

        if (result.success()) {
            payment.setStatus(PaymentStatus.SUCCEEDED);
            order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
            notifyOrder(order, "Payment confirmed",
                    "Payment for your order #" + order.getId() + " has been confirmed.");
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            order.setStatus(OrderStatus.CANCELLED);
            inventoryReservationService.release(order.getBranch().getId(), toReservationLines(order));
            notifyOrder(order, "Order cancelled",
                    "Your order #" + order.getId() + " was cancelled because payment failed: " + result.message());
        }
        paymentRepository.save(payment);
        orderRepository.save(order);

        return OrderResponse.from(order);
    }

    private void notifyOrder(Order order, String subject, String message) {
        User user = order.getUser();
        notificationService.send(new Notification(user.getFullName(), user.getEmail(), user.getPhone(),
                subject, message));
    }

    private boolean isTerminal(OrderStatus status) {
        return status == OrderStatus.PAYMENT_CONFIRMED || status == OrderStatus.CANCELLED;
    }

    private PaymentIntentResponse createPayment(Order order) {
        PaymentIntent intent = paymentService.createIntent(order.getId(), order.getTotalAmount());

        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setGatewayReference(intent.gatewayReference());
        payment.setProvider(provider);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        return new PaymentIntentResponse(provider, intent.requiresClientAction(), intent.gatewayReference(),
                intent.providerKeyId(), intent.amountMinorUnits(), intent.currency());
    }

    /**
     * Reconstructs a {@code PaymentIntentResponse} for an idempotent checkout retry without
     * calling the gateway again (which would create a second, orphaned gateway order). The
     * provider key id is static app config the client already has cached from the original
     * response, so it's left {@code null} here rather than plumbed through just for this
     * retry-window edge case.
     */
    private PaymentIntentResponse replayPaymentIntent(Order order, Payment payment) {
        boolean requiresClientAction = order.getStatus() == OrderStatus.PLACED
                && !"MOCK".equals(payment.getProvider());
        long amountMinorUnits = payment.getAmount().getAmount().multiply(MINOR_UNITS_PER_MAJOR_UNIT).longValueExact();
        return new PaymentIntentResponse(payment.getProvider(), requiresClientAction, payment.getGatewayReference(),
                null, amountMinorUnits, payment.getAmount().getCurrencyCode());
    }

    private List<StockReservationLine> toReservationLines(Cart cart) {
        return cart.getItems().stream()
                .map(item -> new StockReservationLine(item.getProduct().getId(), item.getQuantity()))
                .toList();
    }

    private List<StockReservationLine> toReservationLines(Order order) {
        return order.getItems().stream()
                .map(item -> new StockReservationLine(item.getProduct().getId(), item.getQuantity()))
                .toList();
    }

    /** Resolves and formats the customer's chosen address into the snapshot stored on the
     *  order - null for a pickup order or when no address was selected. Re-validates the
     *  address belongs to this customer rather than trusting the id, same pattern as every
     *  other owner-scoped lookup in this codebase. */
    private String resolveDeliveryAddress(Long userId, Long addressId) {
        if (addressId == null) {
            return null;
        }
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NotFoundException("Address " + addressId + " was not found"));

        StringBuilder formatted = new StringBuilder(address.getLine1());
        if (address.getLine2() != null && !address.getLine2().isBlank()) {
            formatted.append(", ").append(address.getLine2());
        }
        formatted.append(", ").append(address.getCity())
                .append(", ").append(address.getState())
                .append(" ").append(address.getPostalCode());
        return formatted.toString();
    }

    private Order buildOrder(Long userId, String idempotencyKey, Cart cart, Branch branch, String deliveryAddress) {
        Order order = new Order();
        order.setUser(userRepository.getReferenceById(userId));
        order.setSupermarket(branch.getSupermarket());
        order.setBranch(branch);
        order.setIdempotencyKey(idempotencyKey);
        order.setDeliveryAddress(deliveryAddress);
        order.setStatus(OrderStatus.PLACED);

        String currency = cart.getItems().get(0).getUnitPrice().getCurrencyCode();
        Money total = Money.zero(currency);

        for (CartItem cartItem : cart.getItems()) {
            Money lineTotal = cartItem.getUnitPrice().multiply(cartItem.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setProductName(cartItem.getProduct().getName());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setLineTotal(lineTotal);
            order.getItems().add(orderItem);

            total = total.add(lineTotal);
        }
        order.setTotalAmount(total);
        return order;
    }
}
