package com.mgv.client.tts.engine;

import com.mgv.client.tts.util.TtsLogger;
import com.mgv.config.MultiLangTtsConfig;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

// GoogleTtsEngine: lightweight wrapper for Google Translate TTS endpoint
// Note: uses public endpoint - no API key required, but subject to rate limits
public class GoogleTtsEngine implements TtsEngineProvider {
    // hardcoded to Arabic for now - could expose language config if multilingual support expands
    private static final String TTS_URL = "https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&q=%s&tl=ar";

    private final OkHttpClient httpClient;
    private final TtsLogger logger;

    public GoogleTtsEngine(TtsLogger logger) {
        this.logger = logger;
        // gzip interceptor reduces payload size for audio responses - minor but free optimization
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request compressed = original.newBuilder()
                            .addHeader("Accept-Encoding", "gzip")
                            .build();
                    return chain.proceed(compressed);
                })
                .build();
    }

    @Override
    public String getEngineId() {
        return "google";
    }

    @Override
    public boolean isAvailable() {
        // endpoint is always reachable - actual usage gated by config.enabled
        return true;
    }

    @Override
    public CompletableFuture<byte[]> synthesize(String text, MultiLangTtsConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.name());
                String url = String.format(TTS_URL, encodedText);
                
                // headers mimic browser request to avoid simple bot detection
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .addHeader("Accept", "audio/mpeg, audio/*;q=0.9")
                        .addHeader("Accept-Language", "ar-SA,ar;q=0.9")
                        .build();
                        
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        logger.warn("Google TTS HTTP " + response.code());
                        return null;
                    }
                    ResponseBody body = response.body();
                    if (body == null) return null;
                    
                    // guard against HTML error pages disguised as audio
                    String contentType = response.header("Content-Type", "");
                    if (contentType.contains("text/html")) {
                        return null;
                    }
                    
                    byte[] data = body.bytes();
                    // filter out trivial responses - likely rate limit or error payload
                    return data.length > 10 ? data : null;
                }
            } catch (Exception e) {
                logger.warn("Google TTS error: " + e.getMessage());
                return null;
            }
        });
    }

    @Override
    public void shutdown() {
        httpClient.dispatcher().executorService().shutdownNow();
        httpClient.connectionPool().evictAll();
    }
}