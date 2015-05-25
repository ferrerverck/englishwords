package com.words.controller.sound;

import java.net.URL;
import java.util.concurrent.Executor;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

/**
 * Plays notification sounds.
 * @author vladimir
 */
public class PlayWav {
    
    private static final Executor EXEC = new SoundExecutor();
    
    private enum SoundType {
        
        NOTIFICATION("notification.wav"),
        EXCLAMATION("exclamation.wav");
        
        private final URL url;
        
        private SoundType(String fileName) {
            url = PlayWav.class.getResource("/resources/sounds/" + fileName);
        }
        
        public URL getUrl() { return url; }
    }
    
    private static Clip clip;
    
    private PlayWav() {
        throw new AssertionError("Unable to instantiate utility class.");
    }
    
    private static void play(final URL url) {
        if (clip == null || !clip.isRunning()) {
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(url)) {
                clip = AudioSystem.getClip();
                clip.open(ais);
                
                // close clip to prevent memory leak
                clip.addLineListener(event -> {
                    // System.out.println(event);
                    if(event.getType() == LineEvent.Type.STOP){
                        event.getLine().close();
                    }
                });
                
                clip.start();
            } catch (Exception ex) { 
                ex.printStackTrace();
            }
        }
    }
    
    public static void notification() {
        EXEC.execute(() -> play(SoundType.NOTIFICATION.getUrl()));
    }
    
    public static void exclamation() {
        EXEC.execute(() -> play(SoundType.EXCLAMATION.getUrl()));
    }
    
    public static void main(String[] args) throws InterruptedException {
        exclamation();
        Thread.sleep(1000);
    }
}
