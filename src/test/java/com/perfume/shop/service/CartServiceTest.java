package com.perfume.shop.service;

import com.perfume.shop.dto.AddToCartRequest;
import com.perfume.shop.dto.CartResponse;
import com.perfume.shop.entity.Cart;
import com.perfume.shop.entity.CartItem;
import com.perfume.shop.entity.Product;
import com.perfume.shop.entity.User;
import com.perfume.shop.repository.CartItemRepository;
import com.perfume.shop.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Tests")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private CartService cartService;

    private User testUser;
    private Cart testCart;
    private Product testProduct;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .build();
        testUser.setId(1L);

        testProduct = Product.builder()
                .name("Test Perfume")
                .price(new BigDecimal("100.00"))
                .stock(50)
                .active(true)
                .build();
        testProduct.setId(1L);

        testCart = Cart.builder()
                .user(testUser)
                .items(new ArrayList<>())
                .build();
        testCart.setId(1L);

        testCartItem = CartItem.builder()
                .cart(testCart)
                .product(testProduct)
                .quantity(2)
                .price(new BigDecimal("100.00"))
                .build();
        testCartItem.setId(1L);
    }

    @Test
    @DisplayName("Should get cart for user")
    void testGetCart() {
        // Given
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

        // When
        CartResponse response = cartService.getCart(testUser);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should create cart if not exists")
    void testGetCartCreateNew() {
        // Given
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        CartResponse response = cartService.getCart(testUser);

        // Then
        assertThat(response).isNotNull();
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should add item to cart")
    void testAddToCart() {
        // Given
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(productService.getProductEntityById(1L)).thenReturn(testProduct);
        when(cartItemRepository.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(testCartItem);

        // When
        CartResponse response = cartService.addToCart(testUser, request);

        // Then
        assertThat(response).isNotNull();
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Should throw exception when adding item with insufficient stock")
    void testAddToCartInsufficientStock() {
        // Given
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(1L);
        request.setQuantity(100);

        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(productService.getProductEntityById(1L)).thenReturn(testProduct);

        // When & Then
        assertThatThrownBy(() -> cartService.addToCart(testUser, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("Should update cart item quantity")
    void testUpdateCartItem() {
        // Given
        testCart.getItems().add(testCartItem);
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(testCartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(testCartItem);

        // When
        CartResponse response = cartService.updateCartItem(testUser, 1L, 5);

        // Then
        assertThat(response).isNotNull();
        verify(cartItemRepository).save(argThat(item -> item.getQuantity() == 5));
    }

    @Test
    @DisplayName("Should remove item when quantity is zero")
    void testUpdateCartItemZeroQuantity() {
        // Given
        testCart.getItems().add(testCartItem);
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(testCartItem));
        doNothing().when(cartItemRepository).delete(any(CartItem.class));

        // When
        CartResponse response = cartService.updateCartItem(testUser, 1L, 0);

        // Then
        assertThat(response).isNotNull();
        verify(cartItemRepository).delete(testCartItem);
    }

    @Test
    @DisplayName("Should remove item from cart")
    void testRemoveFromCart() {
        // Given
        testCart.getItems().add(testCartItem);
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(testCartItem));
        doNothing().when(cartItemRepository).delete(any(CartItem.class));

        // When
        CartResponse response = cartService.removeFromCart(testUser, 1L);

        // Then
        assertThat(response).isNotNull();
        verify(cartItemRepository).delete(testCartItem);
    }

    @Test
    @DisplayName("Should throw exception when removing item from another user's cart")
    void testRemoveFromCartUnauthorized() {
        // Given
        Cart anotherCart = Cart.builder().build();
        anotherCart.setId(2L);
        testCartItem.setCart(anotherCart);

        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(testCartItem));

        // When & Then
        assertThatThrownBy(() -> cartService.removeFromCart(testUser, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not belong to user");
    }

    @Test
    @DisplayName("Should clear cart")
    void testClearCart() {
        // Given
        testCart.getItems().add(testCartItem);
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // When
        cartService.clearCart(testUser);

        // Then
        verify(cartRepository).save(argThat(cart -> cart.getItems().isEmpty()));
    }

    @Test
    @DisplayName("Should calculate cart totals correctly")
    void testCartTotalsCalculation() {
        // Given
        testCart.getItems().add(testCartItem);
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

        // When
        CartResponse response = cartService.getCart(testUser);

        // Then
        assertThat(response.getSubtotal()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(response.getTax()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(response.getTotal()).isEqualByComparingTo(new BigDecimal("220.00"));
        assertThat(response.getItemCount()).isEqualTo(2);
    }
}
