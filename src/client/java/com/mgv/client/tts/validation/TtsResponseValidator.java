package com.mgv.client.tts.validation;

import java.util.Arrays;

/**
 * Validates TTS response payloads by inspecting byte signatures.
 * Maintains strict format detection without external dependencies.
 */
public class TtsResponseValidator {
    // MP3 frame sync word: 11 consecutive 1s followed by version/layer bits
    private static final byte[] MP3_MAGIC = {(byte) 0xFF, (byte) 0xFB};
    // HTML document type declaration opening sequence
    private static final byte[] HTML_MAGIC = {(byte) 0x3C, (byte) 0x21};

    /**
     * Evaluates whether the provided byte sequence conforms to MP3 format specifications.
     * Checks standard magic bytes, MPEG sync words, and ID3v2 headers.
     */
    public boolean isValidMp3(byte[] data) {
        if (data.length < 2) return false;
        if (data[0] == MP3_MAGIC[0] && data[1] == MP3_MAGIC[1]) return true;
        // Validates MPEG-1/2 Layer III sync pattern
        if (data[0] == (byte) 0xFF && (data[1] & 0xE0) == 0xE0) return true;
        // Verifies ID3v2 metadata header presence
        if (data[0] == 'I' && data[1] == 'D' && data.length > 3 && data[2] == '3') return true;
        return false;
    }

    /*
     * Historical implementation utilizing javax.sound.sampled for format detection.
     * Deprecated due to excessive instantiation overhead and platform-specific codec dependencies.
     * Retained for archival reference.
     */
    /*
    public boolean isValidMp3Legacy(byte[] data) {
        try (var stream = new java.io.ByteArrayInputStream(data)) {
            var af = javax.sound.sampled.AudioSystem.getAudioInputStream(stream).getFormat();
            return af.getEncoding() == javax.sound.sampled.AudioFormat.Encoding.MP3;
        } catch (Exception e) {
            return false;
        }
    }
    */

    /**
     * Determines if the payload contains HTML markup based on initial byte sequences.
     * Accounts for case variations and common structural prefixes.
     */
    public boolean isHtmlResponse(byte[] data) {
        if (data.length < 2) return false;
        if (data[0] == HTML_MAGIC[0] && data[1] == HTML_MAGIC[1]) return true;
        // Broadens detection to include standard HTML element openings
        if (data[0] == '<' && (data[1] == 'h' || data[1] == 'H' || data[1] == '!' || data[1] == '/')) return true;
        return false;
    }

    /*
     * Previous heuristic employing regular expression matching against string-converted content.
     * Removed to mitigate unnecessary heap allocation during large payload processing.
     */
    /*
    public boolean isHtmlResponseRegex(byte[] data) {
        String content = new String(data, java.nio.charset.StandardCharsets.UTF_8);
        return content.matches("(?i)^\\s*<(!DOCTYPE|html|head|body|div|span|p|script|style)[\\s>]");
    }
    */

    /**
     * Extracts a preliminary text segment from HTML payloads for diagnostic purposes.
     * Enforces UTF-8 decoding and truncates output to prevent memory overallocation.
     */
    public String extractHtmlPreview(byte[] data) {
        if (data == null || data.length == 0) return "";
        return new String(data, java.nio.charset.StandardCharsets.UTF_8)
                .substring(0, Math.min(200, data.length));
    }

    /*
     * Stream-based extraction utilizing BufferedReader for controlled character consumption.
     * Superseded by direct substring operation to guarantee deterministic execution latency.
     */
    /*
    public String extractHtmlPreviewStream(byte[] data) {
        if (data == null || data.length == 0) return "";
        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(new java.io.ByteArrayInputStream(data), java.nio.charset.StandardCharsets.UTF_8))) {
            char[] buffer = new char[200];
            int read = reader.read(buffer);
            return read > 0 ? new String(buffer, 0, read) : "";
        } catch (java.io.IOException e) {
            return "";
        }
    }
    */

    /**
     * Generates a diagnostic string for unrecognized audio payloads.
     * Outputs initial byte sequence in array format for log aggregation compatibility.
     */
    public String formatUnknownFormatDebug(byte[] data) {
        if (data == null) return "null";
        return "Unknown audio format (first bytes: " + Arrays.toString(Arrays.copyOf(data, 8)) + ")";
    }

    /*
     * Alternative debugging routine utilizing hexadecimal formatting for raw byte inspection.
     * Transitioned to array representation to improve parsing consistency in centralized logging systems.
     */
    /*
    public String formatUnknownFormatDebugHex(byte[] data) {
        if (data == null) return "null";
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < Math.min(data.length, 8); i++) {
            hex.append(String.format("%02X ", data[i]));
        }
        return "Unknown audio format (first bytes: " + hex.toString().trim() + ")";
    }
    */
}