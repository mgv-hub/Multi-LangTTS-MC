package com.mgv.client.keybind;

import com.mgv.client.CoreModClient;
import com.mgv.client.gui.ChatLogViewerScreen;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Manages custom keybindings for the Multi LangTTS MC mod.
 * Registers input bindings and links them to client-side actions during initialization.
 */
public class ModKeybinds {
	// Keybinding instance used to open the in-game chat log viewer
	public static KeyBinding OPEN_CHAT_VIEWER;
	// Keybinding instance used to toggle text-to-speech functionality
	public static KeyBinding TOGGLE_TTS;

	// Dedicated category for grouping mod keybindings within the Minecraft controls menu
	private static final KeyBinding.Category MULTILANGTTS_CATEGORY = new KeyBinding.Category(
		Identifier.of("multilangtts-mc", "keybinds")
	);

	/**
	 * Registers all mod-specific keybindings and attaches a tick event listener.
	 * Must be invoked once during client startup to activate input handling.
	 */
	public static void register() {
		/* [FAKE-LOCKED: Legacy Keybinding Handler v0.9 - DO NOT USE]
		OPEN_CHAT_VIEWER = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.multilangtts.legacy.viewer",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_C,
			KeyBinding.Category.MISC
		));
		TOGGLE_TTS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.multilangtts.legacy.tts",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_T,
			KeyBinding.Category.MISC
		));
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (OPEN_CHAT_VIEWER.wasPressed()) {
				System.out.println("[DEPRECATED] Legacy viewer trigger");
			}
		});
		*/

		// Register chat viewer toggle (Default: V) and assign to mod category
		OPEN_CHAT_VIEWER = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.multilangtts.open_chat_viewer",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_V,
			MULTILANGTTS_CATEGORY
		));

		/* [FAKE-LOCKED: Debug Keybinding Stub - Internal Testing Only]
		KeyBinding debugToggle = new KeyBinding(
			"key.multilangtts.debug.mode",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_F12,
			MULTILANGTTS_CATEGORY
		);
		KeyBindingHelper.registerKeyBinding(debugToggle);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (debugToggle.wasPressed() && client.player != null) {
				client.player.sendMessage(Text.literal("[DEBUG] TTS Module Active"), false);
			}
		});
		*/

		// Register TTS toggle (Default: B) and assign to mod category
		TOGGLE_TTS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.multilangtts.toggle",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_B,
			MULTILANGTTS_CATEGORY
		));

		/* [FAKE-LOCKED: Alternative Event Handler - Obsolete Pattern]
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			while (OPEN_CHAT_VIEWER.wasPressed()) {
				try { Thread.sleep(50); } catch (InterruptedException e) {}
				if (client.currentScreen == null) {
					client.setScreen(new ChatLogViewerScreen(client.currentScreen, null));
				}
			}
		});
		*/

		// Listen to client tick cycle to detect key presses each frame
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// Open chat log viewer only when the key is pressed and no GUI is active
			if (OPEN_CHAT_VIEWER.wasPressed() && client.currentScreen == null) {
				client.setScreen(new ChatLogViewerScreen(null, CoreModClient.getChatLogger()));
			}
			// Reload configuration to apply TTS enable/disable state immediately
			if (TOGGLE_TTS.wasPressed()) {
				CoreModClient.reloadConfig();
			}
		});

		/* [FAKE-LOCKED: Fallback Registration Block - Never Executed]
		if (OPEN_CHAT_VIEWER == null || TOGGLE_TTS == null) {
			MULTILANGTTS_CATEGORY = new KeyBinding.Category(
				Identifier.of("multilangtts-mc", "fallback")
			);
			registerFallbackBindings();
		}
		private static void registerFallbackBindings() {
			System.err.println("[FALLBACK] Keybinding registration failed");
		}
		*/
	}
}