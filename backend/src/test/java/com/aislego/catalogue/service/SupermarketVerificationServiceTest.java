package com.aislego.catalogue.service;

import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.domain.SupermarketStatus;
import com.aislego.catalogue.repository.SupermarketRepository;
import com.aislego.common.exception.ConflictException;
import com.aislego.common.exception.NotFoundException;
import com.aislego.identity.domain.User;
import com.aislego.identity.repository.UserRepository;
import com.aislego.notifications.Notification;
import com.aislego.notifications.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the admin review workflow's one hard rule: a supermarket application can only be
 * decided once - {@code verify}/{@code reject} on anything but {@code PENDING} is rejected.
 */
@ExtendWith(MockitoExtension.class)
class SupermarketVerificationServiceTest {

    @Mock
    private SupermarketRepository supermarketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SupermarketVerificationService verificationService;

    private static final Long SUPERMARKET_ID = 10L;
    private static final Long ADMIN_ID = 99L;

    private Supermarket pendingSupermarket;
    private User adminReference;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setId(1L);
        owner.setFullName("Store Owner");
        owner.setEmail("owner@example.com");
        owner.setPhone("+15551112222");

        pendingSupermarket = new Supermarket();
        pendingSupermarket.setId(SUPERMARKET_ID);
        pendingSupermarket.setName("New Store");
        pendingSupermarket.setStatus(SupermarketStatus.PENDING);
        pendingSupermarket.setOwner(owner);

        adminReference = new User();
        adminReference.setId(ADMIN_ID);
    }

    @Test
    void verifyMarksAPendingSupermarketVerifiedAndRecordsTheReviewer() {
        when(supermarketRepository.findById(SUPERMARKET_ID)).thenReturn(Optional.of(pendingSupermarket));
        when(userRepository.getReferenceById(ADMIN_ID)).thenReturn(adminReference);
        when(supermarketRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Instant before = Instant.now();
        verificationService.verify(SUPERMARKET_ID, ADMIN_ID);
        Instant after = Instant.now();

        assertThat(pendingSupermarket.getStatus()).isEqualTo(SupermarketStatus.VERIFIED);
        assertThat(pendingSupermarket.getReviewedBy()).isEqualTo(adminReference);
        assertThat(pendingSupermarket.getReviewedAt()).isBetween(before, after);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).send(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().recipientEmail()).isEqualTo("owner@example.com");
        assertThat(notificationCaptor.getValue().subject()).isEqualTo("Store approved");
    }

    @Test
    void verifyDoesNotNotifyWhenTheSupermarketHasNoOwner() {
        pendingSupermarket.setOwner(null);
        when(supermarketRepository.findById(SUPERMARKET_ID)).thenReturn(Optional.of(pendingSupermarket));
        when(userRepository.getReferenceById(ADMIN_ID)).thenReturn(adminReference);
        when(supermarketRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        verificationService.verify(SUPERMARKET_ID, ADMIN_ID);

        verify(notificationService, never()).send(any());
    }

    @Test
    void rejectMarksAPendingSupermarketRejectedWithTheReasonAndReviewer() {
        when(supermarketRepository.findById(SUPERMARKET_ID)).thenReturn(Optional.of(pendingSupermarket));
        when(userRepository.getReferenceById(ADMIN_ID)).thenReturn(adminReference);
        when(supermarketRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        verificationService.reject(SUPERMARKET_ID, ADMIN_ID, "Invalid business documents");

        assertThat(pendingSupermarket.getStatus()).isEqualTo(SupermarketStatus.REJECTED);
        assertThat(pendingSupermarket.getRejectionReason()).isEqualTo("Invalid business documents");
        assertThat(pendingSupermarket.getReviewedBy()).isEqualTo(adminReference);
        assertThat(pendingSupermarket.getReviewedAt()).isNotNull();

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).send(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().subject()).isEqualTo("Store application rejected");
        assertThat(notificationCaptor.getValue().message()).contains("Invalid business documents");
    }

    @Test
    void verifyThrowsConflictWhenTheSupermarketHasAlreadyBeenReviewed() {
        pendingSupermarket.setStatus(SupermarketStatus.VERIFIED);
        when(supermarketRepository.findById(SUPERMARKET_ID)).thenReturn(Optional.of(pendingSupermarket));

        assertThatThrownBy(() -> verificationService.verify(SUPERMARKET_ID, ADMIN_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been reviewed");
    }

    @Test
    void rejectThrowsConflictWhenTheSupermarketHasAlreadyBeenReviewed() {
        pendingSupermarket.setStatus(SupermarketStatus.REJECTED);
        when(supermarketRepository.findById(SUPERMARKET_ID)).thenReturn(Optional.of(pendingSupermarket));

        assertThatThrownBy(() -> verificationService.reject(SUPERMARKET_ID, ADMIN_ID, "Some reason"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been reviewed");
    }

    @Test
    void getMineThrowsNotFoundWhenTheOwnerHasNoSupermarket() {
        when(supermarketRepository.findByOwnerId(ADMIN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> verificationService.getMine(ADMIN_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getMineReturnsTheOwnersSupermarketWhenPresent() {
        when(supermarketRepository.findByOwnerId(ADMIN_ID)).thenReturn(Optional.of(pendingSupermarket));

        Supermarket result = verificationService.getMine(ADMIN_ID);

        assertThat(result).isEqualTo(pendingSupermarket);
    }

    @Test
    void listByStatusFiltersToOneStatusWhenGiven() {
        when(supermarketRepository.findByStatus(SupermarketStatus.PENDING)).thenReturn(java.util.List.of(pendingSupermarket));

        var result = verificationService.listByStatus(SupermarketStatus.PENDING);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(SupermarketStatus.PENDING);
        verify(supermarketRepository, never()).findAll();
    }

    @Test
    void listByStatusReturnsEverySupermarketWhenStatusIsNull() {
        Supermarket verified = new Supermarket();
        verified.setId(20L);
        verified.setName("Other Store");
        verified.setStatus(SupermarketStatus.VERIFIED);
        when(supermarketRepository.findAll()).thenReturn(java.util.List.of(pendingSupermarket, verified));

        var result = verificationService.listByStatus(null);

        assertThat(result).hasSize(2);
        verify(supermarketRepository, never()).findByStatus(any());
    }
}
