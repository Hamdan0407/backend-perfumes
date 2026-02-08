package com.perfume.shop.service;

import com.perfume.shop.dto.*;
import com.perfume.shop.entity.*;
import com.perfume.shop.repository.CartRepository;
import com.perfume.shop.repository.OrderHistoryRepository;
import com.perfume.shop.repository.OrderRepository;
import com.perfume.shop.repository.ProductRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    /**
     * Generate modern startup-style PDF invoice for an order using OpenPDF
     */
    public byte[] generateInvoicePdf(Order order) {
        try {
            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4, 50, 50, 50,
                    50);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            com.lowagie.text.pdf.PdfWriter writer = com.lowagie.text.pdf.PdfWriter.getInstance(document, baos);
            document.open();

            // Define Colors - Application Theme
            java.awt.Color primaryColor = new java.awt.Color(26, 32, 44); // Deep Slate
            java.awt.Color accentColor = new java.awt.Color(245, 158, 11); // Warm Gold
            java.awt.Color darkColor = new java.awt.Color(17, 24, 39);
            java.awt.Color grayColor = new java.awt.Color(107, 114, 128);
            java.awt.Color lightGray = new java.awt.Color(243, 244, 246);

            // Define Fonts
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 32,
                    com.lowagie.text.Font.BOLD, primaryColor);
            com.lowagie.text.Font subtitleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11,
                    com.lowagie.text.Font.NORMAL, grayColor);
            com.lowagie.text.Font headingFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 14,
                    com.lowagie.text.Font.BOLD, darkColor);
            com.lowagie.text.Font normalFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10,
                    com.lowagie.text.Font.NORMAL, darkColor);
            com.lowagie.text.Font boldFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10,
                    com.lowagie.text.Font.BOLD, darkColor);
            com.lowagie.text.Font smallFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8,
                    com.lowagie.text.Font.NORMAL, grayColor);

            // Header Section
            com.lowagie.text.Paragraph companyName = new com.lowagie.text.Paragraph("Parfumé", titleFont);
            companyName.setAlignment(com.lowagie.text.Element.ALIGN_LEFT);
            document.add(companyName);

            com.lowagie.text.Paragraph tagline = new com.lowagie.text.Paragraph("Luxury Fragrances & Premium Scents",
                    subtitleFont);
            tagline.setAlignment(com.lowagie.text.Element.ALIGN_LEFT);
            tagline.setSpacingAfter(15);
            document.add(tagline);

            // Invoice Info Section
            com.lowagie.text.pdf.PdfPTable headerTable = new com.lowagie.text.pdf.PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[] { 1, 1 });

            // Company Contact Details
            com.lowagie.text.pdf.PdfPCell companyCell = new com.lowagie.text.pdf.PdfPCell();
            companyCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            companyCell.addElement(new com.lowagie.text.Paragraph("123 Perfume Lane, Mumbai, MH 400001", smallFont));
            companyCell.addElement(new com.lowagie.text.Paragraph("Phone: +91 9894722186", smallFont));
            companyCell.addElement(new com.lowagie.text.Paragraph("Email: muwas2021@gmail.com", smallFont));
            companyCell
                    .addElement(new com.lowagie.text.Paragraph("GSTIN: 27AAAAA0000A1Z5 | PAN: AAAAA0000A", smallFont));
            headerTable.addCell(companyCell);

            // Invoice Details
            com.lowagie.text.pdf.PdfPCell invoiceCell = new com.lowagie.text.pdf.PdfPCell();
            invoiceCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            invoiceCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            com.lowagie.text.Paragraph taxInvoice = new com.lowagie.text.Paragraph("TAX INVOICE", headingFont);
            taxInvoice.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            invoiceCell.addElement(taxInvoice);
            invoiceCell.addElement(new com.lowagie.text.Paragraph("Invoice No: " + order.getOrderNumber(), boldFont));
            invoiceCell.addElement(new com.lowagie.text.Paragraph(
                    "Date: " + order.getCreatedAt().toString().substring(0, 10), normalFont));
            invoiceCell.addElement(new com.lowagie.text.Paragraph("Status: " + order.getStatus(), normalFont));
            headerTable.addCell(invoiceCell);

            headerTable.setSpacingAfter(20);
            document.add(headerTable);

            // Bill To & Ship To Section
            com.lowagie.text.pdf.PdfPTable addressTable = new com.lowagie.text.pdf.PdfPTable(2);
            addressTable.setWidthPercentage(100);
            addressTable.setWidths(new float[] { 1, 1 });
            addressTable.setSpacingAfter(20);

            // Bill To
            com.lowagie.text.pdf.PdfPCell billToCell = new com.lowagie.text.pdf.PdfPCell();
            billToCell.setBackgroundColor(new java.awt.Color(250, 245, 255));
            billToCell.setPadding(10);
            billToCell.addElement(new com.lowagie.text.Paragraph("BILL TO", boldFont));
            String customerName = order.getUser().getFirstName() + " "
                    + (order.getUser().getLastName() != null ? order.getUser().getLastName() : "");
            billToCell.addElement(new com.lowagie.text.Paragraph(customerName, headingFont));
            billToCell.addElement(new com.lowagie.text.Paragraph(order.getUser().getEmail(), normalFont));
            if (order.getUser().getPhoneNumber() != null) {
                billToCell.addElement(new com.lowagie.text.Paragraph(order.getUser().getPhoneNumber(), normalFont));
            }
            addressTable.addCell(billToCell);

            // Ship To
            com.lowagie.text.pdf.PdfPCell shipToCell = new com.lowagie.text.pdf.PdfPCell();
            shipToCell.setBackgroundColor(new java.awt.Color(255, 247, 237));
            shipToCell.setPadding(10);
            shipToCell.addElement(new com.lowagie.text.Paragraph("SHIP TO", boldFont));
            shipToCell.addElement(new com.lowagie.text.Paragraph(customerName, headingFont));
            shipToCell.addElement(new com.lowagie.text.Paragraph(order.getShippingAddress(), normalFont));
            shipToCell.addElement(new com.lowagie.text.Paragraph(
                    order.getShippingCity() + ", " + order.getShippingZipCode(), normalFont));
            shipToCell.addElement(new com.lowagie.text.Paragraph(order.getShippingCountry(), normalFont));
            if (order.getShippingPhone() != null) {
                shipToCell.addElement(new com.lowagie.text.Paragraph("Phone: " + order.getShippingPhone(), normalFont));
            }
            addressTable.addCell(shipToCell);

            document.add(addressTable);

            // Items Table
            com.lowagie.text.pdf.PdfPTable itemsTable = new com.lowagie.text.pdf.PdfPTable(5);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[] { 0.5f, 3, 0.8f, 1.2f, 1.5f });
            itemsTable.setSpacingAfter(20);

            // Table Header
            com.lowagie.text.Font tableHeaderFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9,
                    com.lowagie.text.Font.BOLD, java.awt.Color.WHITE);
            String[] headers = { "#", "PRODUCT DETAILS", "QTY", "UNIT PRICE", "AMOUNT" };
            for (String header : headers) {
                com.lowagie.text.pdf.PdfPCell headerCell = new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Phrase(header, tableHeaderFont));
                headerCell.setBackgroundColor(primaryColor);
                headerCell.setPadding(8);
                headerCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_LEFT);
                if (header.equals("QTY")) {
                    headerCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                } else if (header.equals("UNIT PRICE") || header.equals("AMOUNT")) {
                    headerCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
                }
                itemsTable.addCell(headerCell);
            }

            // Table Items
            int index = 1;
            java.math.BigDecimal itemSubtotal = java.math.BigDecimal.ZERO;
            for (OrderItem item : order.getItems()) {
                itemSubtotal = itemSubtotal.add(item.getSubtotal());

                // Index
                com.lowagie.text.pdf.PdfPCell indexCell = new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Phrase(String.valueOf(index), normalFont));
                indexCell.setPadding(8);
                indexCell.setBackgroundColor(index % 2 == 0 ? lightGray : java.awt.Color.WHITE);
                itemsTable.addCell(indexCell);

                // Product Name
                com.lowagie.text.pdf.PdfPCell nameCell = new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Phrase(item.getProduct().getName(), boldFont));
                nameCell.setPadding(8);
                nameCell.setBackgroundColor(index % 2 == 0 ? lightGray : java.awt.Color.WHITE);
                itemsTable.addCell(nameCell);

                // Quantity
                com.lowagie.text.pdf.PdfPCell qtyCell = new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Phrase(String.valueOf(item.getQuantity()), normalFont));
                qtyCell.setPadding(8);
                qtyCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                qtyCell.setBackgroundColor(index % 2 == 0 ? lightGray : java.awt.Color.WHITE);
                itemsTable.addCell(qtyCell);

                // Price
                com.lowagie.text.pdf.PdfPCell priceCell = new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Phrase("₹" + item.getPrice(), normalFont));
                priceCell.setPadding(8);
                priceCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
                priceCell.setBackgroundColor(index % 2 == 0 ? lightGray : java.awt.Color.WHITE);
                itemsTable.addCell(priceCell);

                // Amount
                com.lowagie.text.pdf.PdfPCell amountCell = new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Phrase("₹" + item.getSubtotal(), boldFont));
                amountCell.setPadding(8);
                amountCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
                amountCell.setBackgroundColor(index % 2 == 0 ? lightGray : java.awt.Color.WHITE);
                itemsTable.addCell(amountCell);

                index++;
            }

            document.add(itemsTable);

            // Summary Section
            com.lowagie.text.pdf.PdfPTable summaryTable = new com.lowagie.text.pdf.PdfPTable(2);
            summaryTable.setWidthPercentage(40);
            summaryTable.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            summaryTable.setWidths(new float[] { 1.5f, 1 });

            // Subtotal
            summaryTable.addCell(createSummaryCell("Subtotal:", normalFont, false));
            summaryTable.addCell(createSummaryCell("₹" + order.getSubtotal(), boldFont, true));

            // Tax
            summaryTable.addCell(createSummaryCell("GST (18%):", normalFont, false));
            summaryTable.addCell(createSummaryCell("₹" + order.getTax(), boldFont, true));

            // Shipping
            summaryTable.addCell(createSummaryCell("Shipping:", normalFont, false));
            summaryTable.addCell(createSummaryCell(
                    order.getShippingCost().compareTo(java.math.BigDecimal.ZERO) > 0 ? "₹" + order.getShippingCost()
                            : "FREE",
                    boldFont, true));

            // Total
            com.lowagie.text.Font totalFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 13,
                    com.lowagie.text.Font.BOLD, java.awt.Color.WHITE);
            com.lowagie.text.pdf.PdfPCell totalLabelCell = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Phrase("TOTAL:", totalFont));
            totalLabelCell.setBackgroundColor(primaryColor);
            totalLabelCell.setPadding(10);
            totalLabelCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_LEFT);
            summaryTable.addCell(totalLabelCell);

            com.lowagie.text.pdf.PdfPCell totalAmountCell = new com.lowagie.text.pdf.PdfPCell(
                    new com.lowagie.text.Phrase("₹" + order.getTotalAmount(), totalFont));
            totalAmountCell.setBackgroundColor(primaryColor);
            totalAmountCell.setPadding(10);
            totalAmountCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            summaryTable.addCell(totalAmountCell);

            summaryTable.setSpacingAfter(30);
            document.add(summaryTable);

            // Payment Information
            com.lowagie.text.Paragraph paymentInfo = new com.lowagie.text.Paragraph("Payment Information", boldFont);
            paymentInfo.setSpacingAfter(5);
            document.add(paymentInfo);

            com.lowagie.text.Paragraph paymentDetails = new com.lowagie.text.Paragraph(
                    "Payment Method: " + order.getPaymentMethod() + " | Order Status: " + order.getStatus(),
                    normalFont);
            paymentDetails.setSpacingAfter(25);
            document.add(paymentDetails);

            // Footer
            com.lowagie.text.Paragraph thankYou = new com.lowagie.text.Paragraph("🎉 Thank you for shopping with us!",
                    headingFont);
            thankYou.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            thankYou.setSpacingAfter(10);
            document.add(thankYou);

            com.lowagie.text.Paragraph footerText = new com.lowagie.text.Paragraph(
                    "We appreciate your business and look forward to serving you again.\nNeed help? Contact us at muwas2021@gmail.com or call +91 9894722186",
                    smallFont);
            footerText.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(footerText);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    private com.lowagie.text.pdf.PdfPCell createSummaryCell(String text, com.lowagie.text.Font font,
            boolean alignRight) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(text, font));
        cell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        cell.setPadding(5);
        if (alignRight) {
            cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        }
        return cell;
    }

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final EmailService emailService;
    private final RazorpayService razorpayService;
    private final CouponService couponService;

    @Value("${app.stripe.api-key}")
    private String stripeApiKey;

    @Value("${app.razorpay.key-id}")
    private String razorpayKeyId;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.18"); // 18% GST
    private static final BigDecimal SHIPPING_COST = new BigDecimal("10.00");

    /**
     * Transactional checkout flow with Razorpay integration.
     * Stock validation and price locking are done, but stock is NOT deducted until
     * payment confirmation.
     * Uses pessimistic locking to prevent race conditions.
     * Creates a Razorpay order for payment processing.
     */
    @Transactional
    public RazorpayOrderResponse createOrder(User user, CheckoutRequest request) {
        log.info("Creating order for user: {}", user.getId());

        // Step 1: Retrieve and validate cart
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Step 2: Lock products with pessimistic write lock to prevent concurrent
        // modifications
        List<Long> productIds = cart.getItems().stream()
                .map(item -> item.getProduct().getId())
                .collect(Collectors.toList());

        List<Product> lockedProducts = productRepository.findAllByIdWithLock(productIds);
        var productMap = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // Step 3: Validate stock availability with locked products
        StringBuilder stockErrors = new StringBuilder();
        for (CartItem item : cart.getItems()) {
            Product product = productMap.get(item.getProduct().getId());

            if (product == null) {
                stockErrors.append("Product not found: ").append(item.getProduct().getName()).append(". ");
                continue;
            }

            if (!product.getActive()) {
                stockErrors.append("Product no longer available: ").append(product.getName()).append(". ");
                continue;
            }

            if (product.getStock() < item.getQuantity()) {
                stockErrors.append("Insufficient stock for ").append(product.getName())
                        .append(". Available: ").append(product.getStock())
                        .append(", Requested: ").append(item.getQuantity()).append(". ");
            }
        }

        if (stockErrors.length() > 0) {
            throw new RuntimeException("Stock validation failed: " + stockErrors.toString());
        }

        // Step 4: Lock prices - capture current prices from locked products
        for (CartItem item : cart.getItems()) {
            Product product = productMap.get(item.getProduct().getId());
            BigDecimal currentPrice = product.getDiscountPrice() != null
                    ? product.getDiscountPrice()
                    : product.getPrice();
            item.setPrice(currentPrice);
        }

        // Step 5: Calculate totals with locked prices
        BigDecimal subtotal = cart.getItems().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tax = subtotal.multiply(TAX_RATE);
        BigDecimal total = subtotal.add(tax).add(SHIPPING_COST);

        // Step 5.5: Apply coupon discount if provided
        BigDecimal discount = BigDecimal.ZERO;
        String appliedCouponCode = null;
        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            try {
                var couponValidation = couponService.validateCoupon(request.getCouponCode(), subtotal);
                if (couponValidation.getValid()) {
                    discount = couponValidation.getDiscountAmount();
                    total = total.subtract(discount);
                    appliedCouponCode = request.getCouponCode();
                    log.info("Coupon {} applied. Discount: {}", appliedCouponCode, discount);
                }
            } catch (Exception e) {
                log.warn("Coupon validation failed: {}", e.getMessage());
                // Continue without coupon - don't fail checkout
            }
        }

        // Step 6: Generate unique order number
        String orderNumber = generateOrderNumber();

        // Step 7: Create order with locked prices and validated stock
        Order order = Order.builder()
                .user(user)
                .orderNumber(orderNumber)
                .subtotal(subtotal)
                .tax(tax)
                .shippingCost(SHIPPING_COST)
                .discount(discount)
                .couponCode(appliedCouponCode)
                .totalAmount(total)
                .status(Order.OrderStatus.PLACED)
                .shippingAddress(request.getShippingAddress())
                .shippingCity(request.getShippingCity())
                .shippingCountry(request.getShippingCountry())
                .shippingZipCode(request.getShippingZipCode())
                .shippingPhone(request.getShippingPhone())
                .paymentMethod("razorpay")
                .build();

        // Step 8: Create order items with locked prices
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            Product product = productMap.get(cartItem.getProduct().getId());
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPrice())
                    .build();
            orderItems.add(orderItem);
        }

        order.setItems(orderItems);

        // Step 9: Persist order (stock not deducted yet)
        order = orderRepository.save(order);
        log.info("Order created: {} with total: {}", orderNumber, total);

        // Step 10: Create Razorpay Order
        // Convert amount to paise (multiply by 100 for INR)
        Long amountInPaise = total.multiply(new BigDecimal("100")).longValue();

        RazorpayOrderRequest razorpayRequest = RazorpayOrderRequest.builder()
                .amount(amountInPaise)
                .currency("INR")
                .receipt(orderNumber)
                .customerId(user.getId().toString())
                .customerName(user.getEmail())
                .customerEmail(user.getEmail())
                .customerPhone(request.getShippingPhone())
                .build();

        RazorpayOrderResponse razorpayResponse;
        try {
            razorpayResponse = razorpayService.createRazorpayOrder(razorpayRequest);

            // Step 11: Save Razorpay Order ID to our order
            order.setRazorpayOrderId(razorpayResponse.getRazorpayOrderId());
            order = orderRepository.save(order);

            // Step 12: Return response with Razorpay details for frontend
            return razorpayResponse.toBuilder()
                    .orderId(order.getId())
                    .orderNumber(orderNumber)
                    .build();

        } catch (Exception e) {
            log.error("Razorpay order creation failed for order: {}", orderNumber, e);
            throw new RuntimeException("Payment initialization failed: " + e.getMessage());
        }
    }

    /**
     * Confirm payment after successful Razorpay payment.
     * /**
     * Called by webhook or frontend after payment verification.
     * Atomically deducts stock and updates order status.
     * Implements comprehensive idempotency and error handling.
     */
    @Transactional
    public Order confirmPayment(String razorpayOrderId, String razorpayPaymentId) {
        log.info("Confirming payment for Razorpay order: {}", razorpayOrderId);

        // Step 1: Find order by Razorpay order ID
        Order order = orderRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found for Razorpay order ID: " + razorpayOrderId));

        // Step 2: Check idempotency - if already confirmed, return existing order
        if (order.getStatus() != Order.OrderStatus.PLACED) {
            log.warn("Order {} already has status: {}. Skipping confirmation.", order.getOrderNumber(),
                    order.getStatus());
            return order;
        }

        // Step 3: Validate Razorpay payment ID
        if (razorpayPaymentId == null || razorpayPaymentId.trim().isEmpty()) {
            throw new RuntimeException("Razorpay payment ID is required for payment confirmation");
        }

        // Step 4: Lock products with pessimistic write lock to prevent concurrent
        // modifications
        List<Long> productIds = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .collect(Collectors.toList());

        List<Product> lockedProducts = productRepository.findAllByIdWithLock(productIds);
        var productMap = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // Step 5: Validate stock availability before deduction
        StringBuilder stockErrors = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            Product product = productMap.get(item.getProduct().getId());

            if (product == null) {
                stockErrors.append("Product not found: ").append(item.getProduct().getName()).append(". ");
                continue;
            }

            if (!product.getActive()) {
                stockErrors.append("Product no longer available: ").append(product.getName()).append(". ");
                continue;
            }

            int currentStock = product.getStock();
            if (currentStock < item.getQuantity()) {
                stockErrors.append("Insufficient stock for ").append(product.getName())
                        .append(". Available: ").append(currentStock)
                        .append(", Required: ").append(item.getQuantity()).append(". ");
            }
        }

        if (stockErrors.length() > 0) {
            log.error("Stock validation failed for order {}: {}", order.getOrderNumber(), stockErrors.toString());
            // Stock became unavailable - this is a critical error
            throw new RuntimeException("Stock validation failed after payment: " + stockErrors.toString());
        }

        // Step 6: Deduct stock atomically
        for (OrderItem item : order.getItems()) {
            Product product = productMap.get(item.getProduct().getId());
            int newStock = product.getStock() - item.getQuantity();

            if (newStock < 0) {
                throw new RuntimeException("Stock underflow for product: " + product.getName());
            }

            product.setStock(newStock);
            productRepository.save(product);
            log.debug("Deducted stock for product {}: {} units (now {})",
                    product.getId(), item.getQuantity(), newStock);
        }

        // Step 7: Update order status and save payment ID
        order.setStatus(Order.OrderStatus.PLACED);
        order.setRazorpayPaymentId(razorpayPaymentId);
        order = orderRepository.save(order);

        // Step 8: Create order history entry
        createOrderHistoryEntry(order, Order.OrderStatus.PLACED, "SYSTEM",
                "Payment confirmed via Razorpay. Payment ID: " + razorpayPaymentId);

        log.info("Payment confirmed for order: {} with payment ID: {}", order.getOrderNumber(), razorpayPaymentId);

        // Step 9: Clear user's cart asynchronously
        try {
            Cart cart = cartRepository.findByUserId(order.getUser().getId()).orElse(null);
            if (cart != null) {
                cart.getItems().clear();
                cartRepository.save(cart);
                log.debug("Cleared cart for user: {}", order.getUser().getId());
            }
        } catch (Exception e) {
            log.error("Failed to clear cart for user: {}", order.getUser().getId(), e);
            // Don't fail transaction if cart clearing fails
        }

        // Step 10: Send confirmation emails asynchronously
        try {
            emailService.sendOrderConfirmation(order);
            emailService.sendAdminOrderNotification(order);
        } catch (Exception e) {
            log.error("Failed to send order confirmation emails for order: {}", order.getOrderNumber(), e);
            // Don't fail transaction if email fails
        }

        return order;
    }

    /**
     * Legacy Stripe payment confirmation (keeping for backward compatibility if
     * needed)
     */
    @Transactional
    public Order confirmPaymentByStripe(String paymentIntentId) {
        Order order = orderRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new RuntimeException("Order not found for payment"));

        if (order.getStatus() != Order.OrderStatus.PLACED) {
            return order;
        }

        List<Long> productIds = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .collect(Collectors.toList());

        List<Product> lockedProducts = productRepository.findAllByIdWithLock(productIds);
        var productMap = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        StringBuilder stockErrors = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            Product product = productMap.get(item.getProduct().getId());

            if (product == null) {
                stockErrors.append("Product not found: ").append(item.getProduct().getName()).append(". ");
                continue;
            }

            int currentStock = product.getStock();
            if (currentStock < item.getQuantity()) {
                stockErrors.append("Insufficient stock for ").append(product.getName())
                        .append(". Available: ").append(currentStock)
                        .append(", Required: ").append(item.getQuantity()).append(". ");
            }
        }

        if (stockErrors.length() > 0) {
            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(order);
            throw new RuntimeException("Stock validation failed after payment: " + stockErrors.toString());
        }

        for (OrderItem item : order.getItems()) {
            Product product = productMap.get(item.getProduct().getId());
            int newStock = product.getStock() - item.getQuantity();

            if (newStock < 0) {
                throw new RuntimeException("Stock underflow for product: " + product.getName());
            }

            product.setStock(newStock);
            productRepository.save(product);
        }

        order.setStatus(Order.OrderStatus.PLACED);
        order = orderRepository.save(order);

        Cart cart = cartRepository.findByUserId(order.getUser().getId()).orElse(null);
        if (cart != null) {
            cart.getItems().clear();
            cartRepository.save(cart);
        }

        try {
            emailService.sendOrderConfirmation(order);
        } catch (Exception e) {
            log.error("Failed to send order confirmation email", e);
        }

        return order;
    }

    @Cacheable(value = "orders", key = "'user_' + #user.id")
    public List<Order> getUserOrders(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Page<Order> getUserOrdersPage(User user, Pageable pageable) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    @Cacheable(value = "orders", key = "#id")
    public Order getOrderById(Long id, User user) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        return order;
    }

    public Order getOrderByNumber(String orderNumber, User user) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        return order;
    }

    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    @CacheEvict(value = "orders", allEntries = true)
    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Order.OrderStatus previousStatus = order.getStatus();

        log.info("Updating order {} status from {} to {}", order.getOrderNumber(), previousStatus, status);

        // Restore stock if changing to CANCELLED or REFUNDED from a paid status
        if ((status == Order.OrderStatus.CANCELLED || status == Order.OrderStatus.REFUNDED) &&
                (previousStatus == Order.OrderStatus.PLACED ||
                        previousStatus == Order.OrderStatus.CONFIRMED ||
                        previousStatus == Order.OrderStatus.PACKED ||
                        previousStatus == Order.OrderStatus.SHIPPED ||
                        previousStatus == Order.OrderStatus.DELIVERED)) {
            restoreStockForOrder(order);
            log.info("Stock restored for order {} changed from {} to {}",
                    order.getOrderNumber(), previousStatus, status);
        }

        order.setStatus(status);
        order = orderRepository.save(order);

        // Create history entry
        createOrderHistoryEntry(order, status, "SYSTEM", null);

        // Send notification email
        try {
            emailService.sendOrderStatusUpdate(order);
            log.info("Email notification triggered for order {} status change to {}", order.getOrderNumber(), status);
        } catch (Exception e) {
            log.error("Failed to trigger email notification for order {}", order.getOrderNumber(), e);
        }

        return order;
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus status, String updatedBy, String notes) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Order.OrderStatus previousStatus = order.getStatus();

        log.info("Updating order {} status from {} to {} by {}", order.getOrderNumber(), previousStatus, status,
                updatedBy);

        // Restore stock if changing to CANCELLED or REFUNDED from a paid status
        if ((status == Order.OrderStatus.CANCELLED || status == Order.OrderStatus.REFUNDED) &&
                (previousStatus == Order.OrderStatus.PLACED ||
                        previousStatus == Order.OrderStatus.CONFIRMED ||
                        previousStatus == Order.OrderStatus.PACKED ||
                        previousStatus == Order.OrderStatus.SHIPPED ||
                        previousStatus == Order.OrderStatus.DELIVERED)) {
            restoreStockForOrder(order);
            log.info("Stock restored for order {} changed from {} to {} by {}",
                    order.getOrderNumber(), previousStatus, status, updatedBy);
        }

        order.setStatus(status);
        order = orderRepository.save(order);

        // Create history entry
        createOrderHistoryEntry(order, status, updatedBy, notes);

        // Send notification email
        try {
            emailService.sendOrderStatusUpdate(order);
            log.info("Email notification triggered for order {} status change to {} by {}", order.getOrderNumber(),
                    status, updatedBy);
        } catch (Exception e) {
            log.error("Failed to trigger email notification for order {}", order.getOrderNumber(), e);
        }

        return order;
    }

    private void createOrderHistoryEntry(Order order, Order.OrderStatus status, String updatedBy, String notes) {
        OrderHistory history = OrderHistory.builder()
                .order(order)
                .status(status)
                .timestamp(java.time.LocalDateTime.now())
                .notes(notes)
                .updatedBy(updatedBy)
                .build();

        orderHistoryRepository.save(history);
    }

    public List<OrderTimelineResponse> getOrderTimeline(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        List<OrderHistory> historyList = orderHistoryRepository.findByOrderIdOrderByTimestampAsc(orderId);

        return historyList.stream()
                .map(history -> OrderTimelineResponse.builder()
                        .id(history.getId())
                        .status(history.getStatus().toString())
                        .timestamp(history.getTimestamp())
                        .notes(history.getNotes())
                        .updatedBy(history.getUpdatedBy())
                        .isActive(history.getStatus() == order.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public Order updateTrackingNumber(Long orderId, String trackingNumber) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setTrackingNumber(trackingNumber);
        order.setStatus(Order.OrderStatus.SHIPPED);
        order = orderRepository.save(order);

        // Create history entry
        createOrderHistoryEntry(order, Order.OrderStatus.SHIPPED, "SYSTEM", "Tracking number added: " + trackingNumber);

        emailService.sendShippingNotification(order);

        return order;
    }

    /**
     * Generate unique order number with professional format: ORD-YYYYMMDD-XXX
     * Example: ORD-20260208-001, ORD-20260208-002, etc.
     * 
     * Uses database to ensure sequential numbering per day
     */
    private String generateOrderNumber() {
        // Get current date in YYYYMMDD format
        String dateStr = java.time.LocalDate.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Get count of orders created today
        String datePrefix = "ORD-" + dateStr + "-";
        long todayOrderCount = orderRepository.countByOrderNumberStartingWith(datePrefix);

        // Generate sequential number (001, 002, 003, etc.)
        String sequentialNumber = String.format("%03d", todayOrderCount + 1);

        return datePrefix + sequentialNumber;
    }

    /**
     * Cancel order if payment hasn't been confirmed
     */
    @Transactional
    public Order cancelOrder(Long orderId, User user) {
        Order order = getOrderById(orderId, user);

        if (order.getStatus() == Order.OrderStatus.SHIPPED ||
                order.getStatus() == Order.OrderStatus.DELIVERED ||
                order.getStatus() == Order.OrderStatus.PACKED) {
            throw new RuntimeException("Cannot cancel order in current status: " + order.getStatus());
        }

        // Restore stock for cancelled orders that have been paid
        if (order.getStatus() == Order.OrderStatus.PLACED ||
                order.getStatus() == Order.OrderStatus.CONFIRMED) {
            restoreStockForOrder(order);
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        // Create history entry
        createOrderHistoryEntry(order, Order.OrderStatus.CANCELLED, user.getEmail(), "Order cancelled by user");

        return order;
    }

    /**
     * Restore stock when order is cancelled
     */
    private void restoreStockForOrder(Order order) {
        log.info("Restoring stock for cancelled order: {}", order.getOrderNumber());

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int restoredStock = product.getStock() + item.getQuantity();
            product.setStock(restoredStock);
            productRepository.save(product);
            log.debug("Restored stock for product {}: +{} units (now {})",
                    product.getId(), item.getQuantity(), restoredStock);
        }
    }

    /**
     * Get total count of all orders
     */
    public Long countTotalOrders() {
        return orderRepository.count();
    }
}
