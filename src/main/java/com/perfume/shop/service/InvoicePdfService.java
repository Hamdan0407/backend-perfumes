package com.perfume.shop.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.perfume.shop.entity.Order;
import com.perfume.shop.entity.OrderItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Service to generate PDF invoices for orders
 */
@Service
@Slf4j
public class InvoicePdfService {
    
    public byte[] generateInvoicePdf(Order order) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            // Set margins
            document.setMargins(20, 20, 20, 20);
            
            // Title
            Paragraph title = new Paragraph("INVOICE")
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);
            
            // Company info
            Paragraph companyInfo = new Paragraph("MUWAS - Luxury Fragrances & Premium Scents\n" +
                    "No 3, Modi Ibrahim Street, Ambur, Tamil Nadu 635802\n" +
                    "📞 +91 9629004158\n" +
                    "📧 muwas2021@gmail.com | 🌐 www.muwas.com")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(companyInfo);
            
            document.add(new Paragraph("\n"));
            
            // Order details section
            Paragraph orderDetails = new Paragraph("ORDER DETAILS")
                    .setFontSize(12)
                    .setBold();
            document.add(orderDetails);
            
            Paragraph orderInfo = new Paragraph(
                    "Order Number: " + order.getOrderNumber() + "\n" +
                    "Order Date: " + order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) + "\n" +
                    "Status: " + order.getStatus() + "\n" +
                    "Payment Method: " + order.getPaymentMethod()
            ).setFontSize(10);
            document.add(orderInfo);
            
            document.add(new Paragraph("\n"));
            
            // Customer info
            Paragraph customerTitle = new Paragraph("BILL TO")
                    .setFontSize(12)
                    .setBold();
            document.add(customerTitle);
            
            Paragraph customerInfo = new Paragraph(
                    order.getUser().getFirstName() + " " + (order.getUser().getLastName() != null ? order.getUser().getLastName() : "") + "\n" +
                    order.getUser().getEmail() + "\n" +
                    order.getShippingPhone() != null ? order.getShippingPhone() : "N/A"
            ).setFontSize(10);
            document.add(customerInfo);
            
            document.add(new Paragraph("\n"));
            
            // Shipping address
            Paragraph shippingTitle = new Paragraph("SHIP TO")
                    .setFontSize(12)
                    .setBold();
            document.add(shippingTitle);
            
            Paragraph shippingAddress = new Paragraph(
                    order.getShippingAddress() + "\n" +
                    order.getShippingCity() + ", " + order.getShippingCountry() + " " + order.getShippingZipCode()
            ).setFontSize(10);
            document.add(shippingAddress);
            
            document.add(new Paragraph("\n"));
            
            // Items table
            Table table = new Table(4);
            table.addCell("Product");
            table.addCell("Qty");
            table.addCell("Price");
            table.addCell("Total");
            
            for (OrderItem item : order.getItems()) {
                table.addCell(item.getProduct().getName());
                table.addCell(String.valueOf(item.getQuantity()));
                table.addCell("₹" + String.format("%.2f", item.getPrice()));
                BigDecimal itemTotal = item.getPrice().multiply(new BigDecimal(item.getQuantity()));
                table.addCell("₹" + String.format("%.2f", itemTotal));
            }
            
            document.add(table);
            
            document.add(new Paragraph("\n"));
            
            // Summary
            Table summaryTable = new Table(2);
            summaryTable.addCell("Subtotal:");
            summaryTable.addCell("₹" + String.format("%.2f", order.getSubtotal()));
            summaryTable.addCell("Shipping:");
            summaryTable.addCell("₹" + String.format("%.2f", order.getShippingCost()));
            summaryTable.addCell("Tax:");
            summaryTable.addCell("₹" + String.format("%.2f", order.getTax()));
            summaryTable.addCell("TOTAL:");
            summaryTable.addCell("₹" + String.format("%.2f", order.getTotalAmount()));
            
            document.add(summaryTable);
            
            document.add(new Paragraph("\n\nThank you for your purchase!").setTextAlignment(TextAlignment.CENTER));
            
            document.close();
            
            byte[] pdfBytes = baos.toByteArray();
            log.info("Generated PDF invoice for order: {}", order.getOrderNumber());
            return pdfBytes;
            
        } catch (Exception e) {
            log.error("Failed to generate PDF invoice for order: {}", order.getOrderNumber(), e);
            throw new RuntimeException("Failed to generate PDF invoice", e);
        }
    }
}
