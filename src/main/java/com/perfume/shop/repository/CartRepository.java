package com.perfume.shop.repository;

import com.perfume.shop.entity.Cart;
import com.perfume.shop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUser(User user);

    Optional<Cart> findByUserId(Long userId);

    @Query("SELECT c FROM Cart c WHERE c.lastUpdated < :threshold " +
            "AND c.abandonedEmailSent = false " +
            "AND SIZE(c.items) > 0")
    List<Cart> findAbandonedCarts(@Param("threshold") LocalDateTime threshold);
}
