package com.perfume.shop.repository;

import com.perfume.shop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    Boolean existsByEmail(String email);
    
    // Admin methods
    Long countByActive(Boolean active);
    
    Long countByCreatedAtAfter(LocalDateTime date);
    java.util.Optional<User> findByResetToken(String resetToken);
}
