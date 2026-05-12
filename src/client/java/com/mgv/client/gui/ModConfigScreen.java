package com.mgv.client.gui;

import com.mgv.client.chat.ChatLogger;
import com.mgv.client.CoreModClient;
import com.mgv.config.MultiLangTtsConfig;
import com.mgv.client.tts.engine.TtsEngineProvider;
import com.mgv.client.tts.playback.JLayerAudioPlayer;
import com.mgv.client.tts.util.TtsLogger;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;

// ModConfigScreen: builds ClothConfig2-based UI for TTS settings
// Categories mirror provider structure for easier navigation
public class ModConfigScreen {
    private final Screen parent;
    private final MultiLangTtsConfig config;
    private final ChatLogger chatLogger;
    private String testPhrase = "مرحبا";

    public ModConfigScreen(Screen parent, MultiLangTtsConfig config, ChatLogger chatLogger) {
        this.parent = parent;
        this.config = config;
        this.chatLogger = chatLogger;
    }

    public Screen create() {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("text.multilangtts.config.title"));

        // General Settings
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("text.multilangtts.config.general"));
        general.addEntry(builder.entryBuilder()
                .startBooleanToggle(Text.translatable("text.multilangtts.config.enabled"), config.enabled)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> config.enabled = newValue)
                .build());
        general.addEntry(builder.entryBuilder()
                .startBooleanToggle(Text.translatable("text.multilangtts.config.read_server_messages"), config.readServerMessages)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> config.readServerMessages = newValue)
                .build());
        general.addEntry(builder.entryBuilder()
                .startBooleanToggle(Text.translatable("text.multilangtts.config.ignore_spam"), config.ignoreSpam)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> config.ignoreSpam = newValue)
                .build());
        general.addEntry(builder.entryBuilder()
                .startBooleanToggle(Text.translatable("text.multilangtts.config.cache_enabled"), config.cacheEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> config.cacheEnabled = newValue)
                .build());
        general.addEntry(builder.entryBuilder()
                .startBooleanToggle(Text.translatable("text.multilangtts.config.read_names"), config.readPlayerNames)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> config.readPlayerNames = newValue)
                .build());
        // Strips numeric characters from speaker labels to improve TTS pronunciation of gamertags
        general.addEntry(builder.entryBuilder()
                .startBooleanToggle(Text.translatable("text.multilangtts.config.strip_numbers_from_names"), config.stripNumbersFromNames)
                .setDefaultValue(false)
                .setSaveConsumer(newValue -> config.stripNumbersFromNames = newValue)
                .build());

        // Google Translate Provider
        ConfigCategory googleProvider = builder.getOrCreateCategory(Text.translatable("text.multilangtts.config.provider.google"));
        googleProvider.addEntry(builder.entryBuilder()
                .startBooleanToggle(Text.translatable("text.multilangtts.config.provider.enabled"), config.providers.google.enabled)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> config.providers.google.enabled = newValue)
                .build());
        googleProvider.addEntry(builder.entryBuilder()
                .startStrField(Text.translatable("text.multilangtts.config.provider.google.language"), config.providers.google.language)
                .setDefaultValue("ar")
                .setTooltip(Text.translatable("text.multilangtts.config.provider.google.language.tooltip"))
                .setSaveConsumer(newValue -> config.providers.google.language = newValue)
                .build());

        // ElevenLabs Provider
        ConfigCategory elevenLabsProvider = builder.getOrCreateCategory(Text.translatable("text.multilangtts.config.provider.elevenlabs"));
        elevenLabsProvider.addEntry(builder.entryBuilder()
                .startBooleanToggle(Text.translatable("text.multilangtts.config.provider.enabled"), config.providers.elevenlabs.enabled)
                .setDefaultValue(false)
                .setSaveConsumer(newValue -> config.providers.elevenlabs.enabled = newValue)
                .build());
        elevenLabsProvider.addEntry(builder.entryBuilder()
                .startStrField(Text.translatable("text.multilangtts.config.provider.elevenlabs.api_key"), config.providers.elevenlabs.apiKey)
                .setDefaultValue("")
                .setSaveConsumer(newValue -> config.providers.elevenlabs.apiKey = newValue)
                .build());
        elevenLabsProvider.addEntry(builder.entryBuilder()
                .startStrField(Text.translatable("text.multilangtts.config.provider.elevenlabs.voice_id"), config.providers.elevenlabs.voiceId)
                .setDefaultValue("")
                .setSaveConsumer(newValue -> config.providers.elevenlabs.voiceId = newValue)
                .build());
        elevenLabsProvider.addEntry(builder.entryBuilder()
                .startIntSlider(Text.translatable("text.multilangtts.config.provider.elevenlabs.stability"), (int) (config.providers.elevenlabs.stability * 100), 0, 100)
                .setDefaultValue(50)
                .setSaveConsumer(newValue -> config.providers.elevenlabs.stability = newValue / 100f)
                .build());
        elevenLabsProvider.addEntry(builder.entryBuilder()
                .startIntSlider(Text.translatable("text.multilangtts.config.provider.elevenlabs.similarity_boost"), (int) (config.providers.elevenlabs.similarityBoost * 100), 0, 100)
                .setDefaultValue(75)
                .setSaveConsumer(newValue -> config.providers.elevenlabs.similarityBoost = newValue / 100f)
                .build());
        elevenLabsProvider.addEntry(builder.entryBuilder()
                .startStrField(Text.translatable("text.multilangtts.config.provider.elevenlabs.model_id"), config.providers.elevenlabs.modelId)
                .setDefaultValue("eleven_multilingual_v2")
                .setSaveConsumer(newValue -> config.providers.elevenlabs.modelId = newValue)
                .build());

        // Chat Logger
        ConfigCategory chatLog = builder.getOrCreateCategory(Text.translatable("text.multilangtts.config.chat_log"));
        chatLog.addEntry(builder.entryBuilder()
                .startBooleanToggle(Text.translatable("text.multilangtts.config.chat_logging_enabled"), config.chatLoggingEnabled)
                .setDefaultValue(false)
                .setSaveConsumer(newValue -> config.chatLoggingEnabled = newValue)
                .build());

        // Test Voice Section
        ConfigCategory test = builder.getOrCreateCategory(Text.translatable("text.multilangtts.config.test_category"));
        test.addEntry(builder.entryBuilder()
                .startStrField(Text.translatable("text.multilangtts.config.test_phrase"), testPhrase)
                .setDefaultValue("مرحبا")
                .setSaveConsumer(newValue -> testPhrase = newValue)
                .build());
        test.addEntry(builder.entryBuilder()
                .startBooleanToggle(Text.translatable("text.multilangtts.config.test_button"), false)
                .setDefaultValue(false)
                .setSaveConsumer(newValue -> {
                    if (newValue) runTtsTest(testPhrase);
                })
                .build());

        // Config Management
        ConfigCategory management = builder.getOrCreateCategory(Text.translatable("text.multilangtts.config.management"));
        management.addEntry(builder.entryBuilder()
                .startBooleanToggle(Text.translatable("text.multilangtts.config.reset_button"), false)
                .setDefaultValue(false)
                .setSaveConsumer(newValue -> {
                    if (newValue) resetConfigToDefaults();
                })
                .build());

        builder.setSavingRunnable(() -> {
            config.save();
            CoreModClient.reloadConfig();
        });

        return builder.build();
    }

    private void resetConfigToDefaults() {
        config.enabled = true;
        config.volume = 1.0f;
        config.speechRate = 1.0f;
        config.voiceGender = MultiLangTtsConfig.VoiceGender.FEMALE;
        config.readServerMessages = true;
        config.ignoreSpam = true;
        config.spamThresholdMs = 3000;
        config.cacheEnabled = true;
        config.readPlayerNames = true;
        config.stripNumbersFromNames = false;
        config.chatLoggingEnabled = false;
        config.language = "en_us";
        config.providers.google.enabled = true;
        config.providers.google.language = "ar";
        config.providers.elevenlabs.enabled = false;
        config.providers.elevenlabs.apiKey = "";
        config.providers.elevenlabs.voiceId = "";
        config.providers.elevenlabs.stability = 0.5f;
        config.providers.elevenlabs.similarityBoost = 0.75f;
        config.providers.elevenlabs.modelId = "eleven_multilingual_v2";
        config.save();
        CoreModClient.reloadConfig();
    }

    // async test runner - avoids freezing UI during TTS synthesis
    private void runTtsTest(String text) {
        if (text == null || text.trim().isEmpty()) return;
        CompletableFuture.runAsync(() -> {
            try {
                TtsLogger logger = new TtsLogger();
                TtsEngineProvider activeProvider = TtsEngineProvider.resolveActive(
                        TtsEngineProvider.createAll(logger, config), config);
                if (activeProvider == null) {
                    logger.error("No TTS provider enabled for test");
                    return;
                }
                byte[] audioData = activeProvider.synthesize(text, config).join();
                if (audioData != null) new JLayerAudioPlayer(logger).play(audioData);
            } catch (Exception e) {
                new TtsLogger().error("Test TTS failed: " + e.getMessage());
            }
        });
    }
}