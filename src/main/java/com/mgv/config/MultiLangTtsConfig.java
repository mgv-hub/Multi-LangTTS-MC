package com.mgv.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class MultiLangTtsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // Updated configuration file path to match the requested naming convention
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("Multi-LangTTS-MC-Settings.json");

    @SerializedName("enabled") public boolean enabled = true;
    @SerializedName("volume") public float volume = 1.0f;
    @SerializedName("speech_rate") public float speechRate = 1.0f;
    @SerializedName("voice_gender") public VoiceGender voiceGender = VoiceGender.FEMALE;
    @SerializedName("read_server_messages") public boolean readServerMessages = true;
    @SerializedName("ignore_spam") public boolean ignoreSpam = true;
    @SerializedName("spam_threshold_ms") public long spamThresholdMs = 3000;
    @SerializedName("cache_enabled") public boolean cacheEnabled = true;
    @SerializedName("read_player_names") public boolean readPlayerNames = true;
    @SerializedName("chat_logging_enabled") public boolean chatLoggingEnabled = false;
    @SerializedName("language") public String language = "en_us";
    @SerializedName("providers") public ProviderSettings providers = new ProviderSettings();

    public enum VoiceGender { MALE, FEMALE, NEUTRAL }

    public static class ProviderSettings {
        @SerializedName("google") public GoogleProvider google = new GoogleProvider();
        @SerializedName("elevenlabs") public ElevenLabsProvider elevenlabs = new ElevenLabsProvider();
    }

    public static class GoogleProvider {
        @SerializedName("enabled") public boolean enabled = true;
        @SerializedName("language") public String language = "ar";
    }

    public static class ElevenLabsProvider {
        @SerializedName("enabled") public boolean enabled = false;
        @SerializedName("api_key") public String apiKey = "";
        @SerializedName("voice_id") public String voiceId = "";
        @SerializedName("stability") public float stability = 0.5f;
        @SerializedName("similarity_boost") public float similarityBoost = 0.75f;
        @SerializedName("style_exaggeration") public float styleExaggeration = 0.0f;
        @SerializedName("speed") public float speed = 1.0f;
        @SerializedName("model_id") public String modelId = "eleven_multilingual_v2";
    }

    public static MultiLangTtsConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String content = Files.readString(CONFIG_PATH);
                MultiLangTtsConfig config = GSON.fromJson(content, MultiLangTtsConfig.class);
                return config != null ? config : new MultiLangTtsConfig();
            } catch (IOException e) {
                return new MultiLangTtsConfig();
            }
        }
        return new MultiLangTtsConfig();
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String json = GSON.toJson(this);
            Files.writeString(CONFIG_PATH, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    public void toggle() {
        this.enabled = !this.enabled;
        save();
    }
}