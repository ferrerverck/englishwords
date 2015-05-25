package com.words.model.filemodel;

import com.words.controller.futurewords.FutureWord;
import com.words.controller.utils.DateTimeUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class FutureWordManager {
    
    public static final String FILE_NAME = "future";
    public static final String DELIMITER = "\t";
    
    private final Path file;
    private final Map<String, FutureWord> futureWords;
    
    FutureWordManager(Path projectDir) {
        file = projectDir.resolve(FILE_NAME);
        futureWords = new TreeMap<>();
        read();
    }
    
    Map<String, FutureWord> getFutureWords() { return futureWords; }
    
    synchronized void save() {
        try (BufferedWriter out =
            Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            futureWords.values().stream().forEach(fw -> {
                StringBuilder sb = new StringBuilder();
                sb.append(fw.getWord()).append(DELIMITER + DELIMITER)
                    .append(fw.getPriority() / FutureWord.FACTOR);
                if (fw.getDateAdded() != null) sb.append(DELIMITER + DELIMITER)
                    .append(fw.getDateAdded());
                if (fw.getDateChanged() != null)
                    sb.append(DELIMITER + DELIMITER).append(
                        fw.getDateChanged());
                
                try {
                    out.write(sb.toString());
                    out.newLine();
                } catch(IOException ioe) { }
            });
        } catch(IOException ioe) {
            System.out.println("Can't write file");
        }
    }
    
    private void read() {
        try (BufferedReader in =
            Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = in.readLine()) != null) {
                String[] tokens = line.split(DELIMITER + "+");
                FutureWord fw = new FutureWord(tokens[0]);

                if (tokens.length > 1) {
                    int priority;
                    
                    try {
                        priority = Integer.parseInt(tokens[1]);
                    } catch (NumberFormatException nfe) {
                        priority = 0;
                    }
                    
                    fw.setPriority(priority);
                    
                    if (tokens.length > 2) fw.setDateAdded(tokens[2]);
                    
                    if (tokens.length > 3) fw.setDateChanged(tokens[3]);
                }
                
                futureWords.put(fw.getWord(), fw);
            }
        } catch(IOException ioe) { }
    }
    
    synchronized void updateFutureWord(String word) {
        String todayAsString = DateTimeUtils.todayAsString();
        
        if (!futureWords.containsKey(word)) {
            FutureWord fw = new FutureWord(word);
            fw.setDateAdded(todayAsString);
            
            futureWords.put(word, fw);
        } else futureWords.get(word).incPriority(todayAsString);
        
        save();
    }
    
    synchronized void deleteFutureWords(Collection<String> words) {
        if (words.isEmpty()) return;
        
        words.stream().forEach(word -> futureWords.remove(word));
        save();
    }
    
    void destroy() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) { }
    }
}
