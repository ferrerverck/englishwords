package com.words.model.filemodel;

import com.words.controller.utils.DateTimeUtils;
import com.words.controller.utils.Utils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Repeat words to remember old forgotten words.
 */
public class RepeatWords {
    
    public static final String FILE_NAME = "repeat";
    
    // Delimiter for property string
    public static final String DELIMITER = ";";
    
    private static final int DATE_RANGE = 3;
    
    // Number of times to jump backwords for additional words
    private static final int NUM_TIMES = 5;
    
    private final Path file;
    private final Properties prop;
    private final Set<String> repeatWords;
    
    public RepeatWords(Path projectDir) {
        this.file = projectDir.resolve(FILE_NAME);
        prop = new Properties();
        
        try (BufferedReader br =
            Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            prop.load(br);
        } catch (IOException e) { }
        
        // set used as cache to fast find repeat words
        repeatWords = new HashSet<>();
        LocalDate deadline =
            DateTimeUtils.getCurrentLocalDate().minusDays(NUM_TIMES + 1);
        prop.stringPropertyNames().stream()
            .filter(key -> DateTimeUtils.parseDate(key).isAfter(deadline))
            .forEach(key -> repeatWords.addAll(
                Arrays.asList(prop.getProperty(key).split(DELIMITER))));
    }
    
    private synchronized void saveProperties() {
        try (BufferedWriter bw =
            Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            prop.store(bw, "");
        } catch (IOException e) { }
    }
    
    /**
     * Add word to repeat later.
     * @param word word as string
     */
    public synchronized void addRepeatWord(String word) {
        if (repeatWords.contains(word)) return;
        
        LocalDate date = DateTimeUtils.getCurrentLocalDate();
        
        List<String> keysWithMinWords = new ArrayList<>();
        int min = Integer.MAX_VALUE;
        
        String key, value;
        for(int i = 0; i < DATE_RANGE; i++) {
            date = date.plusDays(1);
            
            key = DateTimeUtils.localDateToString(date);
            value = prop.getProperty(key);
            
            int amount = 0;
            if (value != null) amount = value.split(DELIMITER).length;
            
            if (min == amount) keysWithMinWords.add(key);
            else if (amount < min) {
                min = amount;
                keysWithMinWords.clear();
                keysWithMinWords.add(key);
            }
        }
        
        key = keysWithMinWords.get(
            Utils.RANDOM.nextInt(keysWithMinWords.size()));
        
        addWord(key, word);
    }
    
    private synchronized void addWord(String key, String word) {
        String value = prop.getProperty(key, "");
        
        if (!value.isEmpty()) value += DELIMITER;
        value += word;
        
        prop.setProperty(key, value);
        repeatWords.add(word);
        saveProperties();
    }
    
    /**
     * Delete repeat word.
     * @param wordToDelete word to delete
     */
    public synchronized void deleteRepeatWord(String wordToDelete) {
        if (!repeatWords.contains(wordToDelete)) return;
        
        LocalDate deadline =
            DateTimeUtils.getCurrentLocalDate().minusDays(NUM_TIMES + 1);
        
        for (String key : prop.stringPropertyNames()) {
            LocalDate date = DateTimeUtils.parseDate(key);
            if (date.isAfter(deadline)) {
                String value = prop.getProperty(key);
                if (value.contains(wordToDelete)) {
                    List<String> tokens =
                        new ArrayList<>(Arrays.asList(value.split(DELIMITER)));
                    tokens.remove(wordToDelete);
                    value = String.join(DELIMITER, tokens);
                    
                    if (value.isEmpty()) prop.remove(key);
                    else prop.put(key, value);
                    
                    saveProperties();
                    repeatWords.remove(wordToDelete);
                    return;
                }
            }
        }
    }
    
    /**
     * Get all currently active repeat words.
     * @return set with words
     */
    public Collection<String> getRepeatWords() {
        return repeatWords;
    }
    
    /**
     * Get words deleted today from the repeat list.
     * @return expired repeat words
     */
    public Collection<String> getExpiredRepeatWords() {
        LocalDate deadline =
            DateTimeUtils.getCurrentLocalDate().minusDays(NUM_TIMES + 1);
        String key = DateTimeUtils.localDateToString(deadline);
        String value = prop.getProperty(key, "");
        
        return Arrays.asList(value.split(DELIMITER));
    }
    
    void destroy() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) { }
    }
}
