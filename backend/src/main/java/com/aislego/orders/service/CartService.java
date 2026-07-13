package com.aislego.orders.service;

import com.aislego.catalogue.domain.Coupon;
import com.aislego.catalogue.domain.Product;
import com.aislego.catalogue.dto.AvailableCouponResponse;
import com.aislego.catalogue.repository.ProductRepository;
import com.aislego.catalogue.service.CouponService;
import com.aislego.common.exception.ConflictException;
import com.aislego.common.exception.NotFoundException;
import com.aislego.common.money.Money;
import com.aislego.identity.repository.UserRepository;
import com.aislego.orders.domain.Cart;
import com.aislego.orders.domain.CartItem;
import com.aislego.orders.dto.AddCartItemRequest;
import com.aislego.orders.dto.CartResponse;
import com.aislego.orders.repository.CartItemRepository;
import com.aislego.orders.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CouponService couponService;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                        ProductRepository productRepository, UserRepository userRepository,
                        CouponService couponService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.couponService = couponService;
    }

    @Transactional
    public CartResponse viewCart(Long userId) {
        return toResponse(getOrCreateCart(userId), userId);
    }

    /**
     * Adds a product to the current user's cart. Enforces the platform's one hard business
     * rule: a cart can only contain products from a single supermarket. The first item
     * added pins {@code cart.supermarketId}; any later item from a different supermarket
     * is rejected with a 409, never silently allowed or silently replacing the cart.
     */
    @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("Product " + request.productId() + " was not found"));

        Cart cart = getOrCreateCart(userId);
        Long productSupermarketId = product.getSupermarket().getId();

        if (cart.getSupermarketId() == null) {
            cart.setSupermarketId(productSupermarketId);
        } else if (!cart.getSupermarketId().equals(productSupermarketId)) {
            throw new ConflictException("CROSS_STORE_CART",
                    "Your cart already contains items from another supermarket. "
                            + "An order can only contain products from a single supermarket - "
                            + "clear your cart or check out before shopping at a different store.");
        }

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setProduct(product);
                    newItem.setQuantity(0);
                    newItem.setUnitPrice(product.getPrice());
                    cart.getItems().add(newItem);
                    return newItem;
                });
        item.setQuantity(item.getQuantity() + request.quantity());

        cartRepository.save(cart);
        return toResponse(cart, userId);
    }

    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long itemId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = findOwnedItem(cart, itemId);
        item.setQuantity(quantity);
        cartRepository.save(cart);
        return toResponse(cart, userId);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = findOwnedItem(cart, itemId);
        cart.getItems().remove(item);
        if (cart.getItems().isEmpty()) {
            cart.setSupermarketId(null);
            cart.setCouponCode(null);
        }
        cartRepository.save(cart);
        return toResponse(cart, userId);
    }

    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cart.setSupermarketId(null);
        cart.setCouponCode(null);
        cartRepository.save(cart);
        return toResponse(cart, userId);
    }

    /** Validates the code (exists, active, not expired, applies to this cart's store) before
     *  committing it - the shopper gets a specific reason if it's rejected, rather than it
     *  silently doing nothing. */
    @Transactional
    public CartResponse applyCoupon(Long userId, String code) {
        Cart cart = getOrCreateCart(userId);
        couponService.resolveApplicableCoupon(code, cart.getSupermarketId(), userId);
        cart.setCouponCode(code.trim().toUpperCase());
        cartRepository.save(cart);
        return toResponse(cart, userId);
    }

    @Transactional
    public CartResponse removeCoupon(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.setCouponCode(null);
        cartRepository.save(cart);
        return toResponse(cart, userId);
    }

    @Transactional(readOnly = true)
    public List<AvailableCouponResponse> listAvailableCoupons(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null || cart.isEmpty()) {
            return List.of();
        }
        return couponService.listApplicableCoupons(cart.getSupermarketId(), computeSubtotal(cart), userId);
    }

    private CartItem findOwnedItem(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Cart item " + itemId + " was not found in your cart"));
    }

    Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(userRepository.getReferenceById(userId));
            return cartRepository.save(cart);
        });
    }

    /**
     * Re-resolves the applied coupon (if any) fresh on every read, rather than trusting a
     * stored discount amount: a coupon can be deactivated, deleted, or expire while a cart just
     * sits there, and the discount also needs to track the current subtotal as items change.
     * A no-longer-valid coupon is silently cleared from the cart rather than surfacing an error
     * on an unrelated cart view - the shopper only sees an error when they actively try to
     * apply one (see {@link #applyCoupon}).
     */
    private CartResponse toResponse(Cart cart, Long userId) {
        Money subtotal = computeSubtotal(cart);
        Coupon coupon = null;
        if (cart.getCouponCode() != null) {
            coupon = couponService.tryResolveApplicableCoupon(cart.getCouponCode(), cart.getSupermarketId(), userId)
                    .orElse(null);
            if (coupon == null) {
                cart.setCouponCode(null);
                cartRepository.save(cart);
            }
        }
        Money discount = couponService.calculateDiscount(coupon, subtotal);
        return CartResponse.from(cart, discount);
    }

    private Money computeSubtotal(Cart cart) {
        if (cart.getItems().isEmpty()) {
            return Money.zero("INR");
        }
        String currency = cart.getItems().get(0).getUnitPrice().getCurrencyCode();
        Money subtotal = Money.zero(currency);
        for (CartItem item : cart.getItems()) {
            subtotal = subtotal.add(item.getUnitPrice().multiply(item.getQuantity()));
        }
        return subtotal;
    }
}
