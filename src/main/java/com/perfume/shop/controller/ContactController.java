package com.perfume.shop.controller;

import com.perfume.shop.dto.ContactMessageRequest;
import com.perfume.shop.entity.ContactMessage;
import com.perfume.shop.service.ContactMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5173" })
public class ContactController {

    private final ContactMessageService contactMessageService;

    /**
     * Submit contact form (public endpoint)
     */
    @PostMapping
    public ResponseEntity<?> submitContactForm(@Valid @RequestBody ContactMessageRequest request) {
        log.info("Contact form submission from: {}", request.getEmail());

        try {
            ContactMessage message = contactMessageService.submitMessage(request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Thank you for contacting us! We'll get back to you soon.",
                    "id", message.getId()));
        } catch (Exception e) {
            log.error("Error submitting contact form", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to submit message. Please try again."));
        }
    }

    /**
     * Get all contact messages (admin only)
     */
    @GetMapping("/admin/messages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ContactMessage>> getAllMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ContactMessage> messages = contactMessageService.getAllMessages(
                PageRequest.of(page, size));

        return ResponseEntity.ok(messages);
    }

    /**
     * Get messages by status (admin only)
     */
    @GetMapping("/admin/messages/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ContactMessage>> getMessagesByStatus(
            @PathVariable ContactMessage.MessageStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ContactMessage> messages = contactMessageService.getMessagesByStatus(
                status, PageRequest.of(page, size));

        return ResponseEntity.ok(messages);
    }

    /**
     * Update message status (admin only)
     */
    @PutMapping("/admin/messages/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ContactMessage> updateMessageStatus(
            @PathVariable Long id,
            @RequestParam ContactMessage.MessageStatus status,
            @RequestParam(required = false) String adminNotes) {

        ContactMessage updated = contactMessageService.updateStatus(id, status, adminNotes);
        return ResponseEntity.ok(updated);
    }

    /**
     * Get unread message count (admin only)
     */
    @GetMapping("/admin/messages/unread/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        long count = contactMessageService.countUnreadMessages();
        return ResponseEntity.ok(Map.of("count", count));
    }
}
