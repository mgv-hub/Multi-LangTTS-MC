package com.mgv.client.handler;

import com.mgv.client.chat.ChatLogger;
import com.mgv.client.tts.engine.OsTtsEngine;
import com.mgv.client.tts.util.ArabicTextUtil;
import com.mgv.config.MultiLangTtsConfig;

import com.mojang.authlib.GameProfile;
import net.minecraft.text.Text;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.message.MessageType;


import java.time.Instant;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

// ChatTtsHandler: intercepts chat messages, filters/spam-checks, and routes to TTS
public class ChatTtsHandler {
    // matches any text with at least one letter or digit - filters out pure emoji/symbol spam
    private static final Pattern TEXT_CONTENT_PATTERN = Pattern.compile(".*[\\p{L}\\p{N}]+.*");
    // Arabic Unicode ranges - used for language-aware formatting in buildTtsText
    private static final Pattern ARABIC_PATTERN = Pattern.compile("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]+");
    
    private final OsTtsEngine ttsEngine;
    private final ChatLogger chatLogger;
    private MultiLangTtsConfig config;
    // simple in-memory rate limiter: key = sender:message, value = last seen timestamp
    private final ConcurrentHashMap<String, Long> lastMessageTime = new ConcurrentHashMap<>();
    
    public ChatTtsHandler(OsTtsEngine ttsEngine, ChatLogger chatLogger, MultiLangTtsConfig config) {
        this.ttsEngine = ttsEngine;
        this.chatLogger = chatLogger;
        this.config = config;
    }
    
    // main entry point - called by Fabric's chat event system
    public void onChatMessage(Text message, SignedMessage signedMessage, GameProfile sender, MessageType.Parameters parameters, Instant timestamp) {
        if (!config.enabled || message == null) return;
        
        String raw = message.getString();
        boolean isServerMessage = sender == null;
        
        if (isServerMessage && !config.readServerMessages) return;
        
        // spam filter: skip if same sender+message repeats within threshold
        if (config.ignoreSpam && sender != null) {
            String senderName = sender.name() != null ? sender.name() : "unknown";
            String key = senderName + ":" + raw;
            long now = System.currentTimeMillis();
            Long last = lastMessageTime.get(key);
            if (last != null && now - last < config.spamThresholdMs) return;
            lastMessageTime.put(key, now);
            cleanupOldEntries();
        }
        
        String cleaned = sanitizeText(raw);
        if (cleaned.isEmpty()) return;
        
        // apply RTL formatting for Arabic before TTS/logging
        String processedText = ArabicTextUtil.processArabicText(cleaned);
        
        // log uses clean text (no bidi markers) for readability in file viewer
        String cleanTextForLog = ArabicTextUtil.stripBidiMarkers(processedText);
        chatLogger.log(sender != null ? sender.name() : "Server", cleanTextForLog);
        
        if (shouldSpeak(sender, processedText)) {
            String finalText = buildTtsText(sender, processedText);
            ttsEngine.enqueue(finalText);
        }
    }
    
    // decides whether a message should trigger TTS playback
    private boolean shouldSpeak(GameProfile sender, String message) {
        if (sender == null) {
            // server messages: only speak if they contain actual text content
            return TEXT_CONTENT_PATTERN.matcher(message).matches();
        }
        // player messages: speak if names are enabled OR message has text content
        if (config.readPlayerNames) return true;
        return TEXT_CONTENT_PATTERN.matcher(message).matches();
    }
    
    // formats the final string sent to TTS engine - adds speaker label when needed
    private String buildTtsText(GameProfile sender, String message) {
        if (sender == null) return message;
        
        String playerName = sender.name() != null ? sender.name() : "unknown";
        if (!config.readPlayerNames) return message;
        
        // strip redundant name prefixes that MC chat sometimes adds
        String lowerMessage = message.toLowerCase();
        String lowerName = playerName.toLowerCase();
        
        if (lowerMessage.startsWith("<" + lowerName) || lowerMessage.startsWith(lowerName) ||
            lowerMessage.startsWith("[" + lowerName + "]") || lowerMessage.contains(lowerName + ":")) {
            message = message.replaceAll("(?i)(<|\\[)?" + Pattern.quote(playerName) + "(>|\\])?:\\s*", "");
            message = message.trim();
        }
        
        // language-aware speaker label: Arabic uses "يقول", others use "says"
        boolean hasArabic = ARABIC_PATTERN.matcher(message).find();
        return hasArabic ? playerName + " \u064A\u0642\u0648\u0644: " + message : playerName + " says: " + message;
    }
    
    // removes MC formatting codes and trims excess - keeps TTS input clean
    private String sanitizeText(String text) {
        String result = text;
        
        // strip both § and & style color/format codes
        result = result.replaceAll("\u00A7[0-9a-fk-or]", "");
        result = result.replaceAll("&[0-9a-fk-or]", "");
        
        // remove common markup patterns that slip through
        result = result.replaceAll("\\{[^}]*\\}", "");
        result = result.replaceAll("<[^>]*>", "");
        
        result = result.trim();
        
        // hard cap to avoid TTS engines choking on huge messages
        if (result.length() > 200) {
            result = result.substring(0, 200);
        }
        
        return result;
    }
    
    // prunes rate-limit map to prevent unbounded memory growth
    private void cleanupOldEntries() {
        long now = System.currentTimeMillis();
        long threshold = TimeUnit.MINUTES.toMillis(5);
        lastMessageTime.entrySet().removeIf(e -> now - e.getValue() > threshold);
    }
    
    public void reloadConfig(MultiLangTtsConfig newConfig) {
        this.config = newConfig;
        ttsEngine.reloadConfig(newConfig);
        chatLogger.reloadConfig(newConfig);
    }
    
    /*
     * considered using Guava's CacheBuilder for automatic expiration
     * stuck with manual cleanup: avoids extra dependency, logic is simple enough
     * switch if we add more complex rate-limit rules later
     */
    /*
    private final Cache<String, Long> messageCache = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build();
    
    private boolean isSpam(String key) {
        Long last = messageCache.getIfPresent(key);
        if (last != null) return true;
        messageCache.put(key, System.currentTimeMillis());
        return false;
    }
    */
    
    /*
     * alternative: pre-compile the name-stripping regex per sender to avoid Pattern.quote overhead
     * shelved: benefit is negligible for typical chat volume, adds complexity
     */
    /*
    private static final ThreadLocal<Map<String, Pattern>> nameStripPatterns =
        ThreadLocal.withInitial(ConcurrentHashMap::new);
    
    private String stripNamePrefix(String message, String playerName) {
        Pattern p = nameStripPatterns.get().computeIfAbsent(playerName,
            n -> Pattern.compile("(?i)(<|\\[)?" + Pattern.quote(n) + "(>|\\])?:\\s*"));
        return p.matcher(message).replaceFirst("");
    }
    */
}