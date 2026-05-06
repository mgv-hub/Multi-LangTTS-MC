package com.mgv.client.tts.engine;

import com.mgv.client.tts.util.TtsLogger;
import com.mgv.config.MultiLangTtsConfig;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

// ElevenLabsTtsEngine: async HTTP client for ElevenLabs TTS API
public class ElevenLabsTtsEngine implements TtsEngineProvider {
    private static final String API_URL = "https://api.elevenlabs.io/v1/text-to-speech/";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    private final OkHttpClient httpClient;
    private final TtsLogger logger;
    private final Gson gson = new Gson();

    public ElevenLabsTtsEngine(TtsLogger logger) {
        this.logger = logger;
        // timeouts tuned for TTS: longer read timeout accounts for voice generation latency
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String getEngineId() {
        return "elevenlabs";
    }

    @Override
    public boolean isAvailable() {
        // always available - actual enablement controlled via config flags
        return true;
    }

    @Override
    public CompletableFuture<byte[]> synthesize(String text, MultiLangTtsConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            MultiLangTtsConfig.ElevenLabsProvider providerConfig = config.providers.elevenlabs;
            if (providerConfig == null || !providerConfig.enabled) {
                return null;
            }
            if (providerConfig.apiKey == null || providerConfig.apiKey.isEmpty()) {
                logger.warn("ElevenLabs API key is missing");
                return null;
            }
            // fallback to default voice/model if not configured - avoids null checks downstream
            String voiceId = providerConfig.voiceId != null && !providerConfig.voiceId.isEmpty()
                    ? providerConfig.voiceId : "EXAVITQu4vr4xnSDxMaL";
            String modelId = providerConfig.modelId != null && !providerConfig.modelId.isEmpty()
                    ? providerConfig.modelId : "eleven_multilingual_v2";
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("text", text);
                payload.addProperty("model_id", modelId);
                payload.addProperty("speed", providerConfig.speed);
                // voice_settings control expressiveness - exposed as sliders in config UI
                JsonObject voiceSettings = new JsonObject();
                voiceSettings.addProperty("stability", providerConfig.stability);
                voiceSettings.addProperty("similarity_boost", providerConfig.similarityBoost);
                voiceSettings.addProperty("style", providerConfig.styleExaggeration);
                payload.add("voice_settings", voiceSettings);
                
                String jsonPayload = gson.toJson(payload);
                RequestBody body = RequestBody.create(jsonPayload, JSON_MEDIA_TYPE);
                
                Request request = new Request.Builder()
                        .url(API_URL + voiceId)
                        .addHeader("xi-api-key", providerConfig.apiKey)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Accept", "audio/mpeg")
                        .post(body)
                        .build();
                        
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        logger.warn("ElevenLabs HTTP " + response.code());
                        return null;
                    }
                    ResponseBody responseBody = response.body();
                    if (responseBody == null) return null;
                    byte[] data = responseBody.bytes();
                    // sanity check: filter out tiny responses that likely indicate errors
                    return data.length > 10 ? data : null;
                }
            } catch (Exception e) {
                logger.warn("ElevenLabs TTS error: " + e.getMessage());
                return null;
            }
        });
    }

    @Override
    public void shutdown() {
        // clean shutdown: release threads and connection pool to avoid resource leaks on mod reload
        httpClient.dispatcher().executorService().shutdownNow();
        httpClient.connectionPool().evictAll();
    }
    
    /*
     * considered adding retry logic with exponential backoff for transient HTTP failures
     * shelved: ElevenLabs API is generally stable, and retries can compound latency during gameplay
     * can add later if users report frequent timeout issues on slow connections
     */
    /*
    private byte[] executeWithRetry(Request request, int attempts) throws IOException {
        IOException lastException = null;
        for (int i = 0; i < attempts; i++) {
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    ResponseBody body = response.body();
                    return body != null ? body.bytes() : null;
                }
                if (response.code() >= 500) throw new IOException("Server error: " + response.code());
                return null; // client error - no point retrying
            } catch (IOException e) {
                lastException = e;
                Thread.sleep(Math.min(2000, 500 * (i + 1))); // simple backoff
            }
        }
        throw lastException != null ? lastException : new IOException("Retry failed");
    }
    */
}