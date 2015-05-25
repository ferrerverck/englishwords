package com.words.model.filemodel;

import com.words.controller.words.Word;
import com.words.controller.words.wordkinds.WordComplexity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Encapsulates word statistics into a properties file.
 * Statistics to save: word picked times, last picked date, word complexity
 * @author vlad
 */
public class WordStats {
    
    static class Stats {
        
        private static final long DEFAULT_TIMESTAMP =
//            System.currentTimeMillis();
            LocalDate.of(2012, Month.DECEMBER, 10).toEpochDay() *
            1000 * 24 * 3600;
        
        private int timesPicked = 0;
        private long lastPickedTimestamp = DEFAULT_TIMESTAMP;
        private WordComplexity complexity = WordComplexity.NORMAL;
        
        public Stats(String rawString) {
            if (rawString == null) return;
            
            String[] tokens = rawString.split(DELIMITER);
            if (tokens.length < 3) return;
            
            try {
                timesPicked = Integer.valueOf(tokens[0]);
            } catch (NumberFormatException nfe) {
                return;
            }
            
            try {
                lastPickedTimestamp = Long.valueOf(tokens[1]);
            } catch (NumberFormatException nfe) {
                return;
            }
            
            try {
                complexity = WordComplexity.valueOf(tokens[2]);
            } catch (IllegalArgumentException iae) {
                complexity = WordComplexity.NORMAL;
            }
        }
        
        @Override
        public String toString() {
            return "" + timesPicked + DELIMITER + lastPickedTimestamp +
                DELIMITER + complexity.name();
        }
        
        public int getTimesPicked() {
            return timesPicked;
        }
        
        public void setLastPickedTimestamp(long lastPickedTimestamp) {
            timesPicked++;
            this.lastPickedTimestamp = lastPickedTimestamp;
        }
        
        public long getLastPickedTimestamp() {
            return lastPickedTimestamp;
        }
        
        public void setComplexity(WordComplexity complexity) {
            this.complexity = complexity;
        }
        
        public WordComplexity getComplexity() {
            return complexity;
        }
    }
    
    public static final String DELIMITER = ";";
    private static final String INFO_MESSAGE = "FORMAT: {times picked};" +
        "{timestamp millis};{COMPLEXITY}";
    public static final String FILE_NAME = "wordstats";
    
    private final Path file;
    private Properties props;
    
    WordStats(Path projectDir) {
        file = projectDir.resolve(FILE_NAME);
        read();
    }
    
    private synchronized void read() {
        props = new Properties();
        
        try (BufferedReader in =
            Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(in);
        } catch (IOException e) { }
    }
    
    private synchronized void save() {
        try (BufferedWriter out =
            Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            props.store(out, INFO_MESSAGE);
        } catch (IOException e) { }
    }
    
    public Stats getWordStats(String word) {
        return new Stats(props.getProperty(word));
    }
    
    public synchronized void setComplexity(String word, WordComplexity complexity) {
        Stats stats = new Stats(props.getProperty(word));
        stats.setComplexity(complexity);
        props.put(word, stats.toString());
        save();
    }
    
    public synchronized void setLastPickedTimestamp(String word, long timestamp) {
        Stats stats = new Stats(props.getProperty(word));
        stats.setLastPickedTimestamp(timestamp);
        props.put(word, stats.toString());
        save();
    }
    
    synchronized void clearRedundancies(Map<String, Word> words) {
        boolean changed = false;
        Iterator<Map.Entry<Object,Object>> iter = props.entrySet().iterator();
        
        while (iter.hasNext()) {
            if (!words.containsKey(iter.next().getKey().toString())) {
                iter.remove();
                changed = true;
            }
        }
        
        if (changed) save();
    }
    
    void destroy() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) { }
    }
}
