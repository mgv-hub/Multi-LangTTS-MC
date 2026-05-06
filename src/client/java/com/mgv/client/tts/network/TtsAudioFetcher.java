package com.mgv.client.tts.network;

import com.mgv.client.tts.util.TtsLogger;
import com.mgv.config.MultiLangTtsConfig;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

// TtsAudioFetcher: direct HTTP fetcher for Google TTS audio - used as fallback/legacy path
public class TtsAudioFetcher {
    private static final String TTS_URL_TEMPLATE = "https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&q=%s&tl=%s";

    private final OkHttpClient httpClient;
    private final TtsLogger logger;
    private volatile MultiLangTtsConfig config;

    public TtsAudioFetcher(TtsLogger logger, MultiLangTtsConfig config) {
        this.logger = logger;
        this.config = config;
        // timeouts match GoogleTtsEngine for consistency across providers
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    public void reloadConfig(MultiLangTtsConfig newConfig) {
        this.config = newConfig;
    }

    // blocking fetch - caller should run on background thread
    public byte[] fetchAudioData(String text) throws Exception {
        if (!config.providers.google.enabled) {
            logger.debug("Google TTS provider is disabled, skipping fetch");
            return null;
        }
        String lang = config.providers.google.language != null && !config.providers.google.language.isEmpty()
                ? config.providers.google.language : "ar";
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.name());
        String url = String.format(TTS_URL_TEMPLATE, encodedText, lang);
        
        // headers crafted to mimic browser request - reduces chance of simple bot blocks
        Request request = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "MultiLangTTS-MC/1.0 (Minecraft Fabric Mod)")
                .addHeader("Accept", "audio/mpeg, audio/*;q=0.9, */*;q=0.8")
                .addHeader("Accept-Language", lang + ";" + lang + ",q=0.9,en-US;q=0.8,en;q=0.7")
                .addHeader("Sec-Fetch-Dest", "audio")
                .addHeader("Sec-Fetch-Mode", "no-cors")
                .build();
                
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("HTTP " + response.code() + ": " + response.message());
                return null;
            }
            ResponseBody body = response.body();
            if (body == null) {
                logger.error("Response body is null");
                return null;
            }
            // guard against HTML error pages or rate-limit responses
            String contentType = response.header("Content-Type", "");
            if (contentType.contains("text/html") || contentType.contains("text/plain")) {
                String preview = body.string();
                if (preview.startsWith("<") || preview.toLowerCase().contains("sorry") || preview.toLowerCase().contains("captcha")) {
                    logger.error("Blocked or error response: " + preview.substring(0, Math.min(200, preview.length())));
                    return null;
                }
            }
            byte[] data = body.bytes();
            if (data.length < 10) {
                logger.error("Response too small: " + data.length + " bytes");
                return null;
            }
            logger.debug("Received " + data.length + " bytes of MP3 audio for lang: " + lang);
            return data;
        } catch (Exception e) {
            logger.error("Fetch error", e);
            throw e;
        }
    }

    public void shutdown() {
        httpClient.dispatcher().executorService().shutdownNow();
        httpClient.connectionPool().evictAll();
    }
}