package com.perfume.shop.repository;

import com.perfume.shop.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    
    Optional<Coupon> findByCodeIgnoreCase(String code);
    
    boolean existsByCodeIgnoreCase(String code);
    
    @Query("SELECT c FROM Coupon c WHERE c.active = true AND c.validFrom <= :now AND c.validUntil >= :now AND c.usedCount < c.usageLimit")
    List<Coupon> findAllActiveCoupons(LocalDateTime now);
    
    List<Coupon> findByActiveTrue();
    
    List<Coupon> findByActiveFalse();
}
