package com.words.controller.sound.downloadmp3;

import com.words.controller.utils.Utils;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Downloads mp3 files from the Internet.
 * @author vlad
 */
public class Mp3Downloader {
    
    private static final String USER_AGENT = "Mozilla/5.0 " +
        "(Windows NT 6.1; WOW64; rv:24.0) Gecko/20100101 Firefox/24.0";
    
    private static final int TIMEOUT = 3000;
    
    private final Path soundDir;
    
    private final Set<String> runningDownloads = Collections.synchronizedSet(new HashSet<>());
    
    public Mp3Downloader(Path soundDir) {
        this.soundDir = soundDir;
    }
    
    /**
     * Downloads mp3 file for specified word. Word should be normalized without
     * double spaces and particle "to " at the beginning.
     * Prohibits simultaneous downloads for equal words.
     * Throws exception if occurs.
     * @param word word to search for mp3
     * @return Path to downloaded file or null if can't download sound file
     */
    public Path download(String word) throws AlreadyDownloadingException {
        word = Utils.normalizeFor3rdParties(word);
        
        synchronized(runningDownloads) {
            if (runningDownloads.contains(word))
                throw new AlreadyDownloadingException(word);
            
            runningDownloads.add(word);
        }
        
        try {
            return downloadSoundFile(word);
        } finally {
            runningDownloads.remove(word);
        }
    }

    private Path downloadSoundFile(String word) {
        Path mp3File = soundDir.resolve(Utils.getMp3FileName(word));
        String normalizedWord = word.replaceAll("\\s", "%20");
        
        // howjsay.com
        String howjsayUrlStr = "http://howjsay.com/mp3/" + normalizedWord + ".mp3";
        if (downloadUrl(howjsayUrlStr, mp3File)) return mp3File;
        
        String withoutPreposition = Utils.trimTrailingPrepositions(word)
            .replaceAll("\\s", "%20");
        if (!normalizedWord.equals(withoutPreposition)){
            howjsayUrlStr = "http://howjsay.com/mp3/" + withoutPreposition + ".mp3";
            if (downloadUrl(howjsayUrlStr, mp3File)) return mp3File;
        }
        
        System.out.println("Word «" + word + "» doesn't exist on howjsay.com");
        
        // google
        String googleUrlStr =
            "http://translate.google.com/translate_tts?tl=en&q=" + normalizedWord;
        if (downloadUrl(googleUrlStr, mp3File)) return mp3File;
        System.out.println("Word «" + word + "» doesn't exist on Google");
        
        try {
            Files.deleteIfExists(mp3File);
        } catch (IOException cannotOccur) { }
        
        return null;
    }
    
    private boolean downloadUrl(String urlString, Path mp3File) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
            httpcon.addRequestProperty("User-Agent", USER_AGENT);
            
            httpcon.setReadTimeout(TIMEOUT);
            httpcon.connect();
            
            try (FileOutputStream fos = new FileOutputStream(mp3File.toFile());
                InputStream in = new BufferedInputStream(httpcon.getInputStream())) {
                ReadableByteChannel rbc = Channels.newChannel(in);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }
            
            return true;
        } catch (IOException ioe) {
            return false;
        }
    }
}
