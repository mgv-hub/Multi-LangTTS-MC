package com.mgv.client.tts.util;

import java.text.Bidi;

/**
 * Utility for Arabic/RTL text handling with Unicode bidirectional support.
 */
public class ArabicTextUtil {
    

    // Unicode bidi control chars - kept as constants for clarity
    private static final char RLE = '\u202B';  // Right-to-Left Embedding
    private static final char PDF = '\u202C';  // Pop Directional Formatting
    private static final char LRE = '\u202A';  // Left-to-Right Embedding
    private static final char RLO = '\u202E';  // Right-to-Left Override
    private static final char LRO = '\u202D';  // Left-to-Right Override
    private static final char LRI = '\u2066';  // Left-to-Right Isolate
    private static final char RLI = '\u2067';  // Right-to-Left Isolate
    private static final char FSI = '\u2068';  // First Strong Isolate
    private static final char PDI = '\u2069';  // Pop Directional Isolate
    private static final char BN = '\u200E';   // Left-to-Right Mark
    

    // cached Arabic block ranges - considered precomputing but overhead negligible
    // private static final int[][] ARABIC_RANGES = {{0x0600,0x06FF},{0x0750,0x077F},...};

    public static boolean containsArabic(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // Arabic Unicode blocks - standard ranges per Unicode spec
            if ((c >= 0x0600 && c <= 0x06FF) ||     // Arabic
                (c >= 0x0750 && c <= 0x077F) ||     // Arabic Supplement
                (c >= 0x08A0 && c <= 0x08FF) ||     // Arabic Extended-A
                (c >= 0xFB50 && c <= 0xFDFF) ||     // Arabic Presentation Forms-A
                (c >= 0xFE70 && c <= 0xFEFF)) {     // Arabic Presentation Forms-B
                return true;
            }
        }
        return false;
    }
    public static String stripBidiMarkers(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // Skip bidirectional formatting characters
            if (c == RLE || c == PDF || c == LRE || c == RLO || 
                c == LRO || c == LRI || c == RLI || c == FSI || 
                c == PDI || c == BN) {
                continue;
            }
            result.append(c);
        }
        return result.toString();
    }

    public static String processArabicText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        if (!containsArabic(text)) {
            return text;
        }
        
        try {
            Bidi bidi = new Bidi(text, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
            if (!bidi.isLeftToRight()) {
                return RLE + text + PDF;
            }
            return text;
        } catch (Exception e) {
            // fallback on any Bidi edge case
            return RLE + text + PDF;
        }
    }
    
    // simple wrapper - alias for processArabicText when explicit wrapping is preferred
    // public static String wrapWithRTLMarkers(String text) { return processArabicText(text); }

    public static String wrapWithRTLMarkers(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return RLE + text + PDF;
    }

    public static String applyRTLEmbedding(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return RLE + text + PDF;
    }
}
