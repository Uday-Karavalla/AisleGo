package com.aislego.inventory.service;

import com.aislego.inventory.domain.Inventory;
import com.aislego.inventory.exception.InsufficientStockException;
import com.aislego.inventory.repository.InventoryRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Atomically reserves stock for an order at checkout time.
 *
 * <p>The whole reservation runs inside a single transaction: every line is checked and
 * decremented in turn, and if any line fails (not enough stock, or a concurrent optimistic
 * lock conflict) an exception is thrown and Spring rolls the transaction back - which
 * automatically "releases" every decrement already applied in this call. There is no
 * separate reserved/committed bucket to manage for this first working flow; a real
 * saga-based reservation-with-manual-release would only be needed once reservations need
 * to survive/span multiple transactions (e.g. hold-then-pay-later flows).
 */
@Service
public class InventoryReservationService {

    private final InventoryRepository inventoryRepository;

    public InventoryReservationService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public void reserveForOrder(Long branchId, List<StockReservationLine> lines) {
        for (StockReservationLine line : lines) {
            reserveOne(branchId, line.productId(), line.quantity());
        }
    }

    private void reserveOne(Long branchId, Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByBranchIdAndProductId(branchId, productId)
                .orElseThrow(() -> new InsufficientStockException(
                        "Product " + productId + " is not stocked at this branch"));

        if (inventory.getQuantityOnHand() < quantity) {
            throw new InsufficientStockException(
                    "Insufficient stock for product " + productId + ": requested " + quantity
                            + " but only " + inventory.getQuantityOnHand() + " available");
        }

        inventory.setQuantityOnHand(inventory.getQuantityOnHand() - quantity);
        try {
            inventoryRepository.saveAndFlush(inventory);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new InsufficientStockException(
                    "Stock for product " + productId + " changed concurrently, please retry checkout");
        }
    }

    /**
     * Restocks previously-reserved lines - used when a payment fails after inventory was
     * already decremented at checkout time (see {@link com.aislego.orders.service.CheckoutService}),
     * so a declined/abandoned payment doesn't permanently strand stock.
     */
    @Transactional
    public void release(Long branchId, List<StockReservationLine> lines) {
        for (StockReservationLine line : lines) {
            releaseOne(branchId, line.productId(), line.quantity());
        }
    }

    private void releaseOne(Long branchId, Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByBranchIdAndProductId(branchId, productId)
                .orElseThrow(() -> new InsufficientStockException(
                        "Product " + productId + " is not stocked at this branch"));

        inventory.setQuantityOnHand(inventory.getQuantityOnHand() + quantity);
        try {
            inventoryRepository.saveAndFlush(inventory);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new InsufficientStockException(
                    "Stock for product " + productId + " changed concurrently, please retry");
        }
    }
}
