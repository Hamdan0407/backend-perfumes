package com.perfume.shop.repository;

import com.perfume.shop.entity.ContactMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    /**
     * Find messages by status
     */
    Page<ContactMessage> findByStatus(ContactMessage.MessageStatus status, Pageable pageable);

    /**
     * Find messages by email
     */
    List<ContactMessage> findByEmailOrderByCreatedAtDesc(String email);

    /**
     * Count unread messages
     */
    long countByStatus(ContactMessage.MessageStatus status);
}
