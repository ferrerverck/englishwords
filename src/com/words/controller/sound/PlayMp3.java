package com.words.controller.sound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

public class PlayMp3 {
    
    private static final URL DUMMY_WORD_URL = PlayMp3.class.getResource(
        "/resources/sounds/next.mp3");
    
    private static final Executor EXEC = new SoundExecutor();
      
    public static void playFile(final Path mp3File) {
        if (mp3File == null) {
            nextWord();
            return;
        }
        
        EXEC.execute(() -> {
            try (InputStream is = new BufferedInputStream(
                Files.newInputStream(mp3File))) {
                Player player = new Player(is);
                player.play();
            } catch (IOException | JavaLayerException ex) { }
        });
    }
    
    /**
     * Default "next" word.
     */
    public static void nextWord() {
        EXEC.execute(() -> {
            try (InputStream is = new BufferedInputStream(
                DUMMY_WORD_URL.openStream())) {
                Player player = new Player(is);
                player.play();
            } catch (IOException | JavaLayerException ex) { }
        });
    }
    
    private PlayMp3() { throw new AssertionError(); }
}
