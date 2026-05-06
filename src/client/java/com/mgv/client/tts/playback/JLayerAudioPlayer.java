package com.mgv.client.tts.playback;

import com.mgv.client.tts.util.TtsLogger;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

// JLayerAudioPlayer: simple blocking MP3 player using javazoom JLayer
public class JLayerAudioPlayer {
    private final TtsLogger log;

    public JLayerAudioPlayer(TtsLogger logger) {
        this.log = logger;
    }

    // blocking call - runs on worker thread, don't invoke from main game loop
    public void play(byte[] data) {
        Player pl = null;
        try {
            log.debug("playing " + data.length + "B via JLayer");
            InputStream in = new ByteArrayInputStream(data);
            pl = new Player(in);
            pl.play(); // blocks until EOF or error
            log.debug("done");
        } catch (JavaLayerException e) {
            log.error("jlayer fail", e);
        } finally {
            if (pl != null) {
                try { pl.close(); } catch (Exception ignored) {}
            }
        }
    }
}