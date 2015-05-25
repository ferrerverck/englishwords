package com.words.model.filemodel;

import com.words.controller.utils.Utils;
import com.words.controller.words.Word;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Definitions for the words
 * @author vlad
 */
public class WordDefinitions {
    
    public static final String FILE_NAME = "definitions";
    
    private final Path file;
    private final Properties props = new Properties();
    
    WordDefinitions(Path projectDir) {
        file = projectDir.resolve(FILE_NAME);
        read();
    }
    
    /**
     * Get definition for specified word.
     * @param word word to check
     * @return definition or null if words is not presented
     */
    synchronized String getDefinition(String word) {
        return props.getProperty(word);
    }
    
    /**
     * Set and save definition.
     * @param word word to set definition for
     * @param definition definition
     */
    synchronized void setDefinition(String word, String definition) {
        if (props == null) read();
        props.setProperty(word, definition);
        save();
    }
    
    private synchronized void read() {
        try (BufferedReader in =
            Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(in);
        } catch (IOException e) { }
    }
    
    private synchronized void save() {
        try (BufferedWriter out =
            Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            props.store(out, "");
        } catch (IOException e) { }
    }
    
    synchronized void clearRedundancies(Map<String, Word> words) {
        boolean changed = false;
        Iterator<Map.Entry<Object,Object>> iter = props.entrySet().iterator();
        
        while (iter.hasNext()) {
            String key = iter.next().getKey().toString();
            if (!words.containsKey(key) && !words.containsKey("to " + key)) {
                iter.remove();
                changed = true;
            }
        }
        
        if (changed) save();
    }
    
    /**
     * Synchronizes all word statistics.
     * @param map edited words
     */
    public synchronized void editWords(Map<Word, Word> map) {
        boolean changed = false;
        
        for (Map.Entry<Word, Word> entry : map.entrySet()) {
            String editedWord = entry.getKey().getWord();
            String originalWord = entry.getValue().getWord();
            
            if (!editedWord.equals(originalWord)) {
                // swap definitions
                String definition = props.getProperty(originalWord);
                
                if (definition != null) {
                    props.remove(originalWord);
                    props.put(editedWord, definition);
                    
                    changed = true;
                }
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
