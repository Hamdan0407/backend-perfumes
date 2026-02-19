package com.perfume.shop.service;

import com.perfume.shop.dto.AddToCartRequest;
import com.perfume.shop.dto.CartItemResponse;
import com.perfume.shop.dto.CartResponse;
import com.perfume.shop.entity.Cart;
import com.perfume.shop.entity.CartItem;
import com.perfume.shop.entity.Product;
import com.perfume.shop.entity.ProductVariant;
import com.perfume.shop.entity.User;
import com.perfume.shop.repository.CartItemRepository;
import com.perfume.shop.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductService productService;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.18"); // 18% GST
    private static final BigDecimal SHIPPING_COST = BigDecimal.ZERO; // No delivery cost for now

    public CartResponse getCart(User user) {
        Cart cart = getOrCreateCart(user);
        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponse addToCart(User user, AddToCartRequest request) {
        Cart cart = getOrCreateCart(user);
        Product product = productService.getProductEntityById(request.getProductId());
        ProductVariant variant = null;

        if (request.getVariantId() != null) {
            variant = product.getVariants().stream()
                    .filter(v -> v.getId().equals(request.getVariantId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Variant not found"));

            if (!variant.getActive()) {
                throw new RuntimeException("Variant is not available");
            }

            if (variant.getStock() < request.getQuantity()) {
                throw new RuntimeException("Insufficient stock for selected variant");
            }
        } else {
            // Base product stock check
            if (product.getStock() < request.getQuantity()) {
                throw new RuntimeException("Insufficient stock");
            }
        }

        CartItem existingItem;
        if (variant != null) {
            existingItem = cartItemRepository
                    .findByCartIdAndProductIdAndVariantId(cart.getId(), product.getId(), variant.getId())
                    .orElse(null);
        } else {
            existingItem = cartItemRepository
                    .findByCartIdAndProductIdAndVariantIsNull(cart.getId(), product.getId())
                    .orElse(null);
        }

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + request.getQuantity();

            // Re-check stock for total quantity
            if (variant != null) {
                if (variant.getStock() < newQuantity) {
                    throw new RuntimeException("Insufficient stock for selected variant");
                }
            } else {
                if (product.getStock() < newQuantity) {
                    throw new RuntimeException("Insufficient stock");
                }
            }

            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            BigDecimal price;
            if (variant != null) {
                price = variant.getDiscountPrice() != null ? variant.getDiscountPrice() : variant.getPrice();
            } else {
                price = product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();
            }

            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .variant(variant)
                    .quantity(request.getQuantity())
                    .price(price)
                    .build();

            cart.addItem(cartItem);
            cartItemRepository.save(cartItem);
        }

        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponse updateCartItem(User user, Long itemId, Integer quantity) {
        Cart cart = getOrCreateCart(user);

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Cart item does not belong to user");
        }

        if (quantity <= 0) {
            return removeFromCart(user, itemId);
        }

        if (cartItem.getVariant() != null) {
            if (cartItem.getVariant().getStock() < quantity) {
                throw new RuntimeException("Insufficient stock for selected variant");
            }
        } else if (cartItem.getProduct().getStock() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);

        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponse removeFromCart(User user, Long itemId) {
        Cart cart = getOrCreateCart(user);

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Cart item does not belong to user");
        }

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        return mapToCartResponse(cart);
    }

    @Transactional
    public void clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse mapToCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::mapToCartItemResponse)
                .collect(Collectors.toList());

        BigDecimal subtotal = cart.getItems().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tax = subtotal.multiply(TAX_RATE);
        BigDecimal shippingCost = SHIPPING_COST;
        BigDecimal total = subtotal.add(tax).add(shippingCost);

        int itemCount = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .subtotal(subtotal)
                .tax(tax)
                .shippingCost(shippingCost)
                .total(total)
                .itemCount(itemCount)
                .build();
    }

    private CartItemResponse mapToCartItemResponse(CartItem item) {
        String variantSize = item.getVariant() != null ? item.getVariant().getSize() + "ml" : null;
        Long variantId = item.getVariant() != null ? item.getVariant().getId() : null;

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productBrand(item.getProduct().getBrand())
                .productImage(item.getProduct().getImageUrl())
                .variantId(variantId)
                .variantSize(variantSize)
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .subtotal(item.getSubtotal())
                .availableStock(item.getVariant() != null ? item.getVariant().getStock() : item.getProduct().getStock())
                .build();
    }
}
