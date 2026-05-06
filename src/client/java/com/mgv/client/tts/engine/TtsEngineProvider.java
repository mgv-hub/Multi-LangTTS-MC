package com.mgv.client.tts.engine;

import com.mgv.client.tts.util.TtsLogger;
import com.mgv.config.MultiLangTtsConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


// TtsEngineProvider: contract for TTS backends - enables swappable synthesis implementations
public interface TtsEngineProvider {
    // unique identifier for config lookup and logging
    String getEngineId();

    // runtime availability check - can be overridden for health checks if needed
    boolean isAvailable();

    // async synthesis: returns MP3 bytes or null on failure
    CompletableFuture<byte[]> synthesize(String text, MultiLangTtsConfig config);

    // cleanup hook for HTTP clients, thread pools, etc.
    void shutdown();

    // factory: instantiates all known providers - extend here when adding new engines
    static List<TtsEngineProvider> createAll(TtsLogger logger, MultiLangTtsConfig config) {
        List<TtsEngineProvider> engines = new ArrayList<>();
        engines.add(new GoogleTtsEngine(logger));
        engines.add(new ElevenLabsTtsEngine(logger));
        return engines;
    }

    // resolution logic: priority-based selection respecting config flags and credentials
    static TtsEngineProvider resolveActive(List<TtsEngineProvider> providers, MultiLangTtsConfig config) {
        // ElevenLabs first if enabled and API key present - higher quality, paid tier
        if (config.providers.elevenlabs.enabled && config.providers.elevenlabs.apiKey != null && !config.providers.elevenlabs.apiKey.isEmpty()) {
            return providers.stream()
                    .filter(p -> p.getEngineId().equals("elevenlabs"))
                    .findFirst()
                    .orElse(null);
        }
        // fallback to Google - free, no key required, but rate-limited
        if (config.providers.google.enabled) {
            return providers.stream()
                    .filter(p -> p.getEngineId().equals("google"))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}