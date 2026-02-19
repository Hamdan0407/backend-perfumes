package com.perfume.shop.repository;

import com.perfume.shop.entity.Coupon;
import com.perfume.shop.entity.CouponUsage;
import com.perfume.shop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    long countByCouponAndUser(Coupon coupon, User user);

    boolean existsByCouponAndUserAndOrder(Coupon coupon, User user, com.perfume.shop.entity.Order order);
}
