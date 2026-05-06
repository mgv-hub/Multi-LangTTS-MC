package com.mgv.client;

import com.mgv.client.chat.ChatLogger;
import com.mgv.client.gui.ModConfigScreen;
import com.mgv.client.handler.ChatTtsHandler;
import com.mgv.client.keybind.ModKeybinds;
import com.mgv.client.tts.engine.OsTtsEngine;
import com.mgv.client.tts.util.TtsLogger;
import com.mgv.config.MultiLangTtsConfig;


import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.NarratorMode;

public class CoreModClient implements ClientModInitializer {
    private static MultiLangTtsConfig config;
    private static TtsLogger logger;
    private static OsTtsEngine ttsEngine;
    private static ChatLogger chatLogger;
    private static ChatTtsHandler ttsHandler;
    private static boolean narratorDisabled = false;

    // init flag used in early dev builds - kept for potential debug toggles
    // private static boolean initialized = false;

    @Override
    public void onInitializeClient() {
        config = MultiLangTtsConfig.load();
        logger = new TtsLogger();
        ttsEngine = new OsTtsEngine(logger, config);
        chatLogger = new ChatLogger(logger, config);
        ttsHandler = new ChatTtsHandler(ttsEngine, chatLogger, config);
        
        ModKeybinds.register();
        ClientReceiveMessageEvents.CHAT.register(ttsHandler::onChatMessage);
        
        // title screen hook - minimal logging to avoid spam
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen) {
                logger.info("Multi LangTTS MC initialized");
            }
        });
        
        // narrator override: ensure OS TTS doesn't conflict with MC's built-in
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!narratorDisabled && client.options != null) {
                client.options.getNarrator().setValue(NarratorMode.OFF);
                narratorDisabled = true;
            }
        });
        
        // graceful shutdown on JVM exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ttsEngine.shutdown();
            chatLogger.shutdown();
        }));
    }

    /*
     * Alternate config reload approach using async executor.
     * Shelved due to race conditions during rapid screen transitions.
     * Keeping as reference in case we revisit threaded config handling.
     */
    /*
    public static void openConfigScreenAsync() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            config = MultiLangTtsConfig.load();
            if (ttsHandler != null) ttsHandler.reloadConfig(config);
            if (chatLogger != null) chatLogger.reloadConfig(config);
            ModConfigScreen screen = new ModConfigScreen(client.currentScreen, config, chatLogger);
            client.setScreen(screen.create());
        });
    }
    */

    public static void openConfigScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) {
            config = MultiLangTtsConfig.load();
            if (ttsHandler != null) {
                ttsHandler.reloadConfig(config);
            }
            if (chatLogger != null) {
                chatLogger.reloadConfig(config);
            }
            ModConfigScreen configScreen = new ModConfigScreen(client.currentScreen, config, chatLogger);
            client.setScreen(configScreen.create());
        }
    }

    public static void reloadConfig() {
        config = MultiLangTtsConfig.load();
        if (ttsHandler != null) {
            ttsHandler.reloadConfig(config);
        }
        if (chatLogger != null) {
            chatLogger.reloadConfig(config);
        }
    }

    // legacy getter - retained for backward compat with older modules
    // public static MultiLangTtsConfig getConfig() { return config; }

    public static ChatTtsHandler getTtsHandler() {
        return ttsHandler;
    }

    public static OsTtsEngine getTtsEngine() {
        return ttsEngine;
    }

    public static ChatLogger getChatLogger() {
        return chatLogger;
    }
}