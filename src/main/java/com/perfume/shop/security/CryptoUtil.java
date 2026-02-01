package com.perfume.shop.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class for cryptographic operations.
 * Provides secure hashing and signature verification methods.
 */
@Component
@Slf4j
public class CryptoUtil {
    
    /**
     * Generate SHA256 hash of input string
     * @param input String to hash
     * @return Hex-encoded SHA256 hash
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA256 algorithm not available", e);
            throw new RuntimeException("Cryptographic algorithm not available");
        }
    }
    
    /**
     * Secure string comparison to prevent timing attacks
     * @param a First string to compare
     * @param b Second string to compare
     * @return true if strings are equal, false otherwise
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();
        
        return constantTimeEquals(aBytes, bBytes);
    }
    
    /**
     * Secure byte array comparison to prevent timing attacks
     * @param a First byte array
     * @param b Second byte array
     * @return true if arrays are equal, false otherwise
     */
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return a == b;
        }
        
        int result = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            result |= a[i] ^ b[i];
        }
        
        return result == 0;
    }
}
