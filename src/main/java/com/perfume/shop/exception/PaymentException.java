package com.perfume.shop.exception;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payment-specific exception for Razorpay integration.
 * Provides detailed error context for payment failures.
 */
@Data
@NoArgsConstructor
public class PaymentException extends RuntimeException {
    
    private String errorCode;
    private String errorMessage;
    private String transactionId;
    private String orderNumber;
    
    public PaymentException(String message) {
        super(message);
        this.errorMessage = message;
    }
    
    public PaymentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.errorMessage = message;
    }
    
    public PaymentException(String message, String errorCode, String transactionId) {
        super(message);
        this.errorCode = errorCode;
        this.errorMessage = message;
        this.transactionId = transactionId;
    }
    
    public PaymentException(String message, Throwable cause) {
        super(message, cause);
        this.errorMessage = message;
    }
    
    public PaymentException withOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
        return this;
    }
    
    public PaymentException withTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }
}
