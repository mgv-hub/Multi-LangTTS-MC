package com.mgv.client.tts.engine;

import com.mgv.client.tts.playback.JLayerAudioPlayer;
import com.mgv.client.tts.util.TtsRateLimiter;
import com.mgv.client.tts.util.TtsLogger;
import com.mgv.config.MultiLangTtsConfig;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

// OsTtsEngine: central orchestrator for TTS request queueing, provider selection, and playback
public class OsTtsEngine {
	// retry policy: 3 attempts with exponential backoff (1s, 2s, 4s)
	private static final int MAX_RETRIES = 3;
	private static final int[] RETRY_DELAYS_MS = { 1000, 2000, 4000 };

	// non-blocking queue - safe for concurrent enqueue from chat events
	private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
	
	// single worker thread: ensures ordered processing and avoids TTS overlap
	private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "multilangtts-worker");
		t.setDaemon(true);
		// slightly lower priority to avoid interfering with game rendering
		t.setPriority(Thread.NORM_PRIORITY - 1);
		return t;
	});

	private final List<TtsEngineProvider> providers;
	private final JLayerAudioPlayer audioPlayer;
	private final TtsRateLimiter rateLimiter;
	private final TtsLogger logger;
	private volatile MultiLangTtsConfig config;
	private volatile TtsEngineProvider activeProvider;
	// prevents multiple worker submissions while queue is being drained
	private final AtomicBoolean processing = new AtomicBoolean(false);

	public OsTtsEngine(TtsLogger logger, MultiLangTtsConfig config) {
		this.logger = logger;
		this.config = config;
		// 1.5s minimum interval between TTS calls - balances responsiveness with API rate limits
		this.rateLimiter = new TtsRateLimiter(1500, logger);
		this.providers = TtsEngineProvider.createAll(logger, config);
		this.activeProvider = TtsEngineProvider.resolveActive(providers, config);
		this.audioPlayer = new JLayerAudioPlayer(logger);
	}

	// public entry point - non-blocking, queues text for async processing
	public void enqueue(String text) {
		if (text == null || text.trim().isEmpty() || !config.enabled || activeProvider == null) {
			return;
		}
		queue.offer(text.trim());
		triggerProcessing();
	}

	// debounced trigger: only submit worker if not already running
	private void triggerProcessing() {
		if (processing.get()) return;
		if (processing.compareAndSet(false, true)) {
			worker.submit(this::runQueue);
		}
	}

	// main processing loop: drains queue, handles retries, respects rate limits
	private void runQueue() {
		try {
			while (!Thread.currentThread().isInterrupted()) {
				String text = queue.poll();
				if (text == null) break;
				logger.debug("Processing TTS: " + text);
				executeTtsWithRetry(text);
				// brief pause between items to avoid audio stacking
				safeSleep(500);
			}
		} catch (Exception e) {
			logger.error("Queue processing error", e);
		} finally {
			processing.set(false);
			// re-trigger if new items arrived during processing
			if (!queue.isEmpty()) triggerProcessing();
		}
	}

	// executes synthesis with retry logic - isolates transient failures from permanent ones
	private void executeTtsWithRetry(String text) {
		Exception lastException = null;
		for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
			try {
				rateLimiter.enforceLimit();
				byte[] audioData = activeProvider.synthesize(text, config).join();
				if (audioData != null && audioData.length > 0) {
					audioPlayer.play(audioData);
					return;
				}
			} catch (Exception e) {
				lastException = e;
				logger.warn("Attempt " + (attempt + 1) + " failed: " + e.getMessage());
			}
			if (attempt < MAX_RETRIES - 1) safeSleep(RETRY_DELAYS_MS[attempt]);
		}
		logger.error("Failed after " + MAX_RETRIES + " attempts", lastException);
	}

	private void safeSleep(long millis) {
		try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
	}

	public void reloadConfig(MultiLangTtsConfig newConfig) {
		this.config = newConfig;
		this.activeProvider = TtsEngineProvider.resolveActive(providers, newConfig);
		logger.info("Configuration reloaded, active provider: " + (activeProvider != null ? activeProvider.getEngineId() : "none"));
	}

	public void shutdown() {
		queue.clear();
		worker.shutdownNow();
		try {
			if (!worker.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
				logger.warn("Worker did not terminate gracefully");
			}
		} catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		for (TtsEngineProvider provider : providers) {
			provider.shutdown();
		}
	}
}