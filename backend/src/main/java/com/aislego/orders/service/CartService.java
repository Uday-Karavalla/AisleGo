package com.aislego.orders.service;

import com.aislego.catalogue.domain.Product;
import com.aislego.catalogue.repository.ProductRepository;
import com.aislego.common.exception.ConflictException;
import com.aislego.common.exception.NotFoundException;
import com.aislego.identity.repository.UserRepository;
import com.aislego.orders.domain.Cart;
import com.aislego.orders.domain.CartItem;
import com.aislego.orders.dto.AddCartItemRequest;
import com.aislego.orders.dto.CartResponse;
import com.aislego.orders.repository.CartItemRepository;
import com.aislego.orders.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                        ProductRepository productRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse viewCart(Long userId) {
        return CartResponse.from(getOrCreateCart(userId));
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
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long itemId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = findOwnedItem(cart, itemId);
        item.setQuantity(quantity);
        cartRepository.save(cart);
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = findOwnedItem(cart, itemId);
        cart.getItems().remove(item);
        if (cart.getItems().isEmpty()) {
            cart.setSupermarketId(null);
        }
        cartRepository.save(cart);
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cart.setSupermarketId(null);
        cartRepository.save(cart);
        return CartResponse.from(cart);
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
}
