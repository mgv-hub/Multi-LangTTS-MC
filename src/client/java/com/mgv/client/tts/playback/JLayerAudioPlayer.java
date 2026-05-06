package com.mgv.client.tts.playback;

import com.mgv.client.tts.util.TtsLogger;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

// JLayerAudioPlayer: simple blocking MP3 player using javazoom JLayer
public class JLayerAudioPlayer {
	private final TtsLogger log;
	private volatile Player activePlayer;

	public JLayerAudioPlayer(TtsLogger logger) {
		this.log = logger;
	}

	// blocking call - runs on worker thread, don't invoke from main game loop
	public void play(byte[] data) {
		play(data, null);
	}


	// blocking call with skip flag support - checks flag per-frame for instant interruption
	public void play(byte[] data, AtomicBoolean skipFlag) {
		Player pl = null;

		try {
			log.debug("playing " + data.length + "B via JLayer");
			InputStream in = new ByteArrayInputStream(data);
			pl = new Player(in);
			activePlayer = pl;
			
			if (skipFlag != null) {
				// pl.play(1) decodes exactly one MP3 frame (~26ms)
				// loop checks skipFlag every frame for responsive cancellation
				while (!skipFlag.get() && pl.play(1));
			} else {
				pl.play();
			}
			log.debug("done");
		} catch (JavaLayerException e) {
			// closing player during skip throws exception - suppress log if intentional skip
			if (skipFlag == null || !skipFlag.get()) {
				log.error("jlayer fail", e);
			}
		} finally {
			activePlayer = null;
			if (pl != null) {
				try { pl.close(); } catch (Exception ignored) {}
			}
		}
	}


	// hard interrupt from external thread if polling loop isn't responding
	public void forceStop() {
		Player pl = activePlayer;
		if (pl != null) {
			try { pl.close(); } catch (Exception ignored) {}
		}
	}
}