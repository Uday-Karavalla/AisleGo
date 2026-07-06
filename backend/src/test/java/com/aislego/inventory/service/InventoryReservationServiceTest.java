package com.aislego.inventory.service;

import com.aislego.inventory.domain.Inventory;
import com.aislego.inventory.exception.InsufficientStockException;
import com.aislego.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryReservationServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    private InventoryReservationService reservationService;

    private static final Long BRANCH_ID = 1L;

    @Test
    void throwsInsufficientStockAndDoesNotDecrementWhenRequestedQuantityExceedsStock() {
        reservationService = new InventoryReservationService(inventoryRepository);

        Inventory inventory = new Inventory();
        inventory.setId(10L);
        inventory.setQuantityOnHand(2);
        when(inventoryRepository.findByBranchIdAndProductId(BRANCH_ID, 99L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() ->
                reservationService.reserveForOrder(BRANCH_ID, List.of(new StockReservationLine(99L, 5))))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");

        // stock must be left untouched - no save should have been attempted
        verify(inventoryRepository, never()).saveAndFlush(any());
        assertThat(inventory.getQuantityOnHand()).isEqualTo(2);
    }

    @Test
    void throwsInsufficientStockWhenNoInventoryRecordExistsForTheBranchAndProduct() {
        reservationService = new InventoryReservationService(inventoryRepository);

        when(inventoryRepository.findByBranchIdAndProductId(BRANCH_ID, 123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reservationService.reserveForOrder(BRANCH_ID, List.of(new StockReservationLine(123L, 1))))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void decrementsStockWhenEnoughIsAvailable() {
        reservationService = new InventoryReservationService(inventoryRepository);

        Inventory inventory = new Inventory();
        inventory.setId(11L);
        inventory.setQuantityOnHand(10);
        when(inventoryRepository.findByBranchIdAndProductId(BRANCH_ID, 55L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.reserveForOrder(BRANCH_ID, List.of(new StockReservationLine(55L, 4)));

        assertThat(inventory.getQuantityOnHand()).isEqualTo(6);
        verify(inventoryRepository).saveAndFlush(inventory);
    }

    @Test
    void releaseIncrementsQuantityOnHand() {
        reservationService = new InventoryReservationService(inventoryRepository);

        Inventory inventory = new Inventory();
        inventory.setId(12L);
        inventory.setQuantityOnHand(3);
        when(inventoryRepository.findByBranchIdAndProductId(BRANCH_ID, 77L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.release(BRANCH_ID, List.of(new StockReservationLine(77L, 5)));

        assertThat(inventory.getQuantityOnHand()).isEqualTo(8);
        verify(inventoryRepository).saveAndFlush(inventory);
    }
}
