package com.mgv.client.chat;

import com.mgv.client.tts.engine.OsTtsEngine;
import com.mgv.client.tts.util.TtsLogger;
import com.mgv.config.MultiLangTtsConfig;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

// ChatLogger: async file writer + in-memory buffer for TTS chat history
// Designed to avoid blocking the main thread during gameplay
public class ChatLogger {
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

	private final Path logDir;
	private final TtsLogger logger;
	private volatile MultiLangTtsConfig config;
	// CopyOnWriteArrayList: safe for concurrent add/read without explicit sync
	private final List<ChatEntry> pendingWrites = new CopyOnWriteArrayList<>();
	private final ExecutorService writerThread;
	// flag to prevent queueing multiple flush tasks simultaneously
	private final AtomicBoolean isWriting = new AtomicBoolean(false);
	private volatile LocalDate currentDate;

	public ChatLogger(TtsLogger logger, MultiLangTtsConfig config) {
		this.logger = logger;
		this.config = config;
		this.logDir = FabricLoader.getInstance().getGameDir().resolve("logs").resolve("multilangtts-mc");
		try { Files.createDirectories(logDir); } catch (IOException ignored) {}
		this.currentDate = LocalDate.now();
		// daemon thread: won't block JVM shutdown if mod reloads unexpectedly
		this.writerThread = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "multilangtts-chat-logger");
			t.setDaemon(true);
			return t;
		});
	}

	// entry point for logging - non-blocking, queues for async write
	public void log(String sender, String message) {
		if (!config.chatLoggingEnabled) return;
		rotateIfNewDay();
		pendingWrites.add(new ChatEntry(LocalTime.now(), sender, message));
		scheduleFlush();
	}

	/*
	 * considered loading logs on a background thread to avoid UI hitch
	 * shelved: today's logs are usually small (<1k lines), sync load is fine
	 * revisit if we add multi-day log browsing
	 */
	public List<ChatEntry> loadTodayLogs() {
		Path file = getCurrentLogFile();
		List<ChatEntry> entries = new ArrayList<>();
		if (!Files.exists(file)) return entries;
		try (BufferedReader br = Files.newBufferedReader(file)) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().isEmpty()) continue;
				parseTxtLine(line, entries);
			}
		} catch (IOException ignored) {}
		return entries;
	}

	// exports current buffer to any path - useful for backup or sharing logs
	public void exportToFile(Path targetPath) {
		List<ChatEntry> data = List.copyOf(pendingWrites);
		if (data.isEmpty()) return;
		try {
			try (BufferedWriter bw = Files.newBufferedWriter(targetPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
				for (ChatEntry r : data) {
					bw.write(String.format("[%s] <%s> %s%n", r.time().format(TIME_FMT), r.sender(), r.message()));
				}
			}
			logger.info("Chat log exported successfully");
		} catch (IOException e) {
			logger.error("Export failed: " + e.getMessage());
		}
	}

	// replay queued messages through TTS - handy for testing or recovering missed audio
	public void replayTtsMessages(OsTtsEngine ttsEngine) {
		for (ChatEntry entry : pendingWrites) {
			ttsEngine.enqueue(entry.message());
		}
		logger.info("Replaying " + pendingWrites.size() + " messages");
	}

	public void reloadConfig(MultiLangTtsConfig newConfig) {
		this.config = newConfig;
	}

	public void shutdown() {
		forceFlush();
		writerThread.shutdownNow();
	}

	// rotate log file at midnight - ensures daily log separation without cron
	private void rotateIfNewDay() {
		LocalDate today = LocalDate.now();
		if (!today.equals(currentDate)) {
			currentDate = today;
			forceFlush();
		}
	}

	// debounce flush calls: only submit one writer task at a time
	private void scheduleFlush() {
		if (isWriting.get()) return;
		if (isWriting.compareAndSet(false, true)) {
			writerThread.submit(this::processQueue);
		}
	}

	/*
	 * original version used a fixed 5s timer for flushes
	 * switched to idle-based flushing: writes when queue is stable for 200ms
	 * reduces disk I/O during chat bursts, better for performance
	 */
	private void processQueue() {
		try {
			while (!pendingWrites.isEmpty()) {
				Thread.sleep(200); // brief idle window to batch incoming messages
				forceFlush();
				}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			isWriting.set(false);
			// re-schedule if new messages arrived during flush
			if (!pendingWrites.isEmpty()) {
				scheduleFlush();
			}
		}
	}

	// actual disk write - synchronized to avoid interleaved writes from multiple flushes
	private synchronized void forceFlush() {
		if (pendingWrites.isEmpty()) return;
		List<ChatEntry> batch = List.copyOf(pendingWrites);
		pendingWrites.removeAll(batch);
		try {
			Path logFile = getCurrentLogFile();
			appendTxt(logFile, batch);
		} catch (Exception e) {
			logger.error("Failed to write chat log: " + e.getMessage());
		}
	}

	// log filename includes ISO date: chat-2024-05-01.txt
	public Path getCurrentLogFile() {
		return logDir.resolve("chat-" + LocalDate.now().format(DATE_FMT) + ".txt");
	}

	private void appendTxt(Path file, List<ChatEntry> records) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
			for (ChatEntry r : records) {
				bw.write(String.format("[%s] <%s> %s%n", r.time().format(TIME_FMT), r.sender(), r.message()));
			}
		}
	}

	// parser for our own log format - tolerant to minor formatting quirks
	private void parseTxtLine(String line, List<ChatEntry> entries) {
		int timeStart = line.indexOf('[');
		int timeEnd = line.indexOf(']');
		if (timeStart == -1 || timeEnd == -1) return;
		String timeStr = line.substring(timeStart + 1, timeEnd);
		int senderStart = line.indexOf('<');
		int senderEnd = line.indexOf('>');
		if (senderStart == -1 || senderEnd == -1) return;
		String sender = line.substring(senderStart + 1, senderEnd);
		String message = line.substring(senderEnd + 2).trim();
		try {
			LocalTime time = LocalTime.parse(timeStr, TIME_FMT);
			entries.add(new ChatEntry(time, sender, message));
		} catch (Exception ignored) {}
	}

	/*
	 * considered JSON log format for easier parsing by external tools
	 * stuck with plain text: human-readable, works with any text editor
	 * JSON can be added later as an optional export format if needed
	 */
	/*
	private void appendJson(Path file, List<ChatEntry> records) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
			for (ChatEntry r : records) {
				String json = String.format("{\"t\":\"%s\",\"s\":\"%s\",\"m\":\"%s\"}%n",
					r.time().format(TIME_FMT), escapeJson(r.sender()), escapeJson(r.message()));
				bw.write(json);
			}
		}
	}
	private String escapeJson(String s) { return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n"); }
	*/

	// immutable record for log entries - clean, concise, no boilerplate
	public record ChatEntry(LocalTime time, String sender, String message) {}
}