package com.aislego.catalogue.service;

import com.aislego.catalogue.domain.Supermarket;
import com.aislego.catalogue.domain.SupermarketStatus;
import com.aislego.catalogue.dto.PendingSupermarketResponse;
import com.aislego.catalogue.repository.SupermarketRepository;
import com.aislego.common.exception.ConflictException;
import com.aislego.common.exception.NotFoundException;
import com.aislego.identity.domain.User;
import com.aislego.identity.repository.UserRepository;
import com.aislego.notifications.Notification;
import com.aislego.notifications.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Admin-side review workflow for self-registered supermarkets: a supermarket starts life
 * {@code PENDING} (see {@code AuthService#registerSupermarketOwner}) and stays invisible to
 * customer-facing discovery (see {@code StoreDiscoveryService}, {@code BranchRepository}) until
 * an admin moves it to {@code VERIFIED} or {@code REJECTED}. A decided application is never
 * re-reviewed.
 */
@Service
@Transactional
public class SupermarketVerificationService {

    private final SupermarketRepository supermarketRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public SupermarketVerificationService(SupermarketRepository supermarketRepository,
                                           UserRepository userRepository,
                                           NotificationService notificationService) {
        this.supermarketRepository = supermarketRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Returns DTOs, not entities: {@code Supermarket.owner} is a lazy association
     * (open-in-view is disabled - see application.yml) and {@link PendingSupermarketResponse}
     * needs to read through it for the owner's display name/email, so the mapping has to
     * happen while this method's transaction is still open.
     */
    @Transactional(readOnly = true)
    public List<PendingSupermarketResponse> listByStatus(SupermarketStatus status) {
        return supermarketRepository.findByStatus(status).stream()
                .map(PendingSupermarketResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Supermarket getMine(Long ownerId) {
        return supermarketRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new NotFoundException("No supermarket is registered to this account"));
    }

    public void verify(Long supermarketId, Long adminUserId) {
        Supermarket supermarket = findPending(supermarketId);
        supermarket.setStatus(SupermarketStatus.VERIFIED);
        supermarket.setReviewedAt(Instant.now());
        supermarket.setReviewedBy(userRepository.getReferenceById(adminUserId));
        supermarketRepository.save(supermarket);
        notifyOwner(supermarket, "Store approved",
                "Your store \"" + supermarket.getName() + "\" has been verified and is now visible to customers.");
    }

    public void reject(Long supermarketId, Long adminUserId, String reason) {
        Supermarket supermarket = findPending(supermarketId);
        supermarket.setStatus(SupermarketStatus.REJECTED);
        supermarket.setRejectionReason(reason);
        supermarket.setReviewedAt(Instant.now());
        supermarket.setReviewedBy(userRepository.getReferenceById(adminUserId));
        supermarketRepository.save(supermarket);
        notifyOwner(supermarket, "Store application rejected",
                "Your store \"" + supermarket.getName() + "\" application was rejected: " + reason);
    }

    /**
     * Pre-existing (V2 seed) supermarkets have no owner account, so there's nobody to notify.
     */
    private void notifyOwner(Supermarket supermarket, String subject, String message) {
        User owner = supermarket.getOwner();
        if (owner == null) {
            return;
        }
        notificationService.send(new Notification(owner.getFullName(), owner.getEmail(), owner.getPhone(),
                subject, message));
    }

    private Supermarket findPending(Long supermarketId) {
        Supermarket supermarket = supermarketRepository.findById(supermarketId)
                .orElseThrow(() -> new NotFoundException("Supermarket " + supermarketId + " was not found"));
        if (supermarket.getStatus() != SupermarketStatus.PENDING) {
            throw new ConflictException("NOT_PENDING", "This supermarket has already been reviewed");
        }
        return supermarket;
    }
}
