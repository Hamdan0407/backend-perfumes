package com.perfume.shop.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts", indexes = {
        @Index(name = "idx_cart_user", columnList = "user_id"),
        @Index(name = "idx_cart_abandoned", columnList = "last_updated, abandoned_email_sent")
})
@Data
@EqualsAndHashCode(callSuper = true, exclude = { "items" })
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "abandoned_email_sent")
    @Builder.Default
    private Boolean abandonedEmailSent = false;

    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
        this.lastUpdated = LocalDateTime.now();
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
        this.lastUpdated = LocalDateTime.now();
    }
}
