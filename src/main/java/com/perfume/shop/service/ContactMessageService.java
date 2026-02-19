package com.perfume.shop.service;

import com.perfume.shop.dto.ContactMessageRequest;
import com.perfume.shop.entity.ContactMessage;
import com.perfume.shop.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;
    private final JavaMailSender mailSender;

    @Value("${app.admin.email:muwas2021@gmail.com}")
    private String adminEmail;

    /**
     * Submit a new contact message
     */
    @Transactional
    public ContactMessage submitMessage(ContactMessageRequest request) {
        log.info("Received contact message from: {}", request.getEmail());

        // Save to database
        ContactMessage message = ContactMessage.builder()
                .name(request.getName())
                .email(request.getEmail())
                .subject(request.getSubject())
                .message(request.getMessage())
                .status(ContactMessage.MessageStatus.NEW)
                .build();

        ContactMessage saved = contactMessageRepository.save(message);
        log.info("Contact message saved with ID: {}", saved.getId());

        // Send email notification to admin (async)
        try {
            sendAdminNotification(saved);
        } catch (Exception e) {
            log.error("Failed to send admin notification email", e);
            // Don't fail the request if email fails
        }

        return saved;
    }

    /**
     * Send email notification to admin
     */
    private void sendAdminNotification(ContactMessage message) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo(adminEmail);
            email.setSubject("New Contact Form Submission: " + message.getSubject());
            email.setText(String.format(
                    "New contact message received:\n\n" +
                            "From: %s (%s)\n" +
                            "Subject: %s\n\n" +
                            "Message:\n%s\n\n" +
                            "---\n" +
                            "Submitted at: %s",
                    message.getName(),
                    message.getEmail(),
                    message.getSubject(),
                    message.getMessage(),
                    message.getCreatedAt()));
            email.setReplyTo(message.getEmail());

            mailSender.send(email);
            log.info("Admin notification sent for contact message ID: {}", message.getId());
        } catch (Exception e) {
            log.warn("Failed to send admin notification: {}", e.getMessage());
        }
    }

    /**
     * Get all messages (admin only)
     */
    public Page<ContactMessage> getAllMessages(Pageable pageable) {
        return contactMessageRepository.findAll(pageable);
    }

    /**
     * Get messages by status (admin only)
     */
    public Page<ContactMessage> getMessagesByStatus(ContactMessage.MessageStatus status, Pageable pageable) {
        return contactMessageRepository.findByStatus(status, pageable);
    }

    /**
     * Update message status (admin only)
     */
    @Transactional
    public ContactMessage updateStatus(Long id, ContactMessage.MessageStatus status, String adminNotes) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        message.setStatus(status);
        if (adminNotes != null) {
            message.setAdminNotes(adminNotes);
        }

        return contactMessageRepository.save(message);
    }

    /**
     * Count unread messages
     */
    public long countUnreadMessages() {
        return contactMessageRepository.countByStatus(ContactMessage.MessageStatus.NEW);
    }
}
