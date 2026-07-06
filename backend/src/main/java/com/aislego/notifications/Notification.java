package com.aislego.notifications;

/**
 * A single message to deliver to one person. Callers (Orders, Admin, ...) compose
 * {@code subject}/{@code message} themselves - this module has no knowledge of domain types
 * like {@code Order} or {@code Supermarket}, only contact details and content, so it stays a
 * leaf module the way {@code payments} does.
 */
public record Notification(String recipientName, String recipientEmail, String recipientPhone, String subject,
                            String message) {
}
