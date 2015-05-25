package com.words.model.filemodel;

import com.words.controller.Controller;
import com.words.controller.utils.DateTimeUtils;
import com.words.controller.words.Word;
import com.words.controller.words.WordFactory;
import com.words.controller.words.wordkinds.WordComplexity;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * All words in a text single file.
 * File format:
 *      {BUNDLE_PREFIX} {date: dd.MM.yyyy}
 *      {list of words}
 *      {BUNDLE_PREFIX} {date: dd.MM.yyyy}
 *      {list of words}
 *      ...
 * @author vlad
 */
public class WordManager {
    
    public  static final String FILE_NAME = "words";
    
    public static final String BUNDLE_PREFIX = "###bundle: ";
    public static final String DELIMITER = "\t";
    
    private final Path projectDir;
    private final Path file;
    
    private final TreeMap<String, Word> allWords = new TreeMap<>((s1, s2) ->
        s1.replaceAll("^to ", "").compareTo(s2.replaceAll("^to ", "")));
    private final TreeMap<LocalDate, List<Word>> bundleMap;
    
    private final WordStats wordStats;
    
    WordManager(Path projectDir) throws IOException {
        this.projectDir = projectDir;
        file = projectDir.resolve(FILE_NAME);
        
        wordStats = new WordStats(projectDir);
        
        bundleMap = new TreeMap<>();
        
//        if (Files.notExists(file)) createDefaults();
        read();
    }
    
    Map<String, Word> getAllWords() { return allWords; }
    
    TreeMap<LocalDate, List<Word>> getBundleMap() { return bundleMap; }
    
    private void createDefaults() {
        LocalDate today = DateTimeUtils.getCurrentLocalDate();
        
        URL url = getClass().getResource("/resources/irregular_verbs");
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                Word word = wordFromRawString(line, today);
                addWord(word);
            }
        } catch(IOException ex) {
            System.out.println("Error while reading first bundle");
        }
        
        save();
    }
    
    private void read() {
        try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            LocalDate bundle = LocalDate.now();
            while ((line = in.readLine()) != null) {
                if (line.startsWith(BUNDLE_PREFIX)) {
                    bundle = DateTimeUtils.parseDate(line.substring(BUNDLE_PREFIX.length()));
                } else {
                    Word word = wordFromRawString(line, bundle);
                    
                    // set stats
                    WordStats.Stats stats = wordStats.getWordStats(word.getWord());
                    word.setLastPickedTimestamp(stats.getLastPickedTimestamp());
                    word.setTimesPicked(stats.getTimesPicked());
                    word.setComplexity(stats.getComplexity());
                    
                    addWord(word);
                }
            }
        } catch (IOException ex) { }
    }
    
    synchronized void save() {
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (LocalDate bundle : bundleMap.keySet()) {
                out.write(BUNDLE_PREFIX + DateTimeUtils.localDateToString(bundle));
                out.newLine();
                
                for (Word word : bundleMap.get(bundle)) {
                    out.write(wordToRawString(word));
                    out.newLine();
                }
            }
        } catch (IOException ex) { 
            System.err.println("Can't open file");
        }
    }
    
    private Word wordFromRawString(String rawString, LocalDate date) {
        String[] tokens = rawString.trim().split(DELIMITER + "+");
        if (tokens.length < 2) return null;
        
        Word word = WordFactory.newWord();
        word.setWord(tokens[0]);
        word.setTranslation(tokens[1]);
        if (tokens.length > 2) word.setSynonyms(tokens[2]);
        word.setBundle(date);
        
        return word;
    }
    
    private String wordToRawString(Word word) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(word.getWord()).append(DELIMITER).append(DELIMITER)
            .append(DELIMITER).append(word.getTranslation());
        if (!word.getSynonyms().isEmpty())
            sb.append(DELIMITER).append(DELIMITER).append(DELIMITER)
                .append(word.getSynonyms());
        
        return sb.toString();
    }
    
    private void addWord(Word word) {
        if (word != null) {
            allWords.put(word.getWord(), word);
            addToBundleMap(word.getBundle(), word);
        }
    }
    
    private void addToBundleMap(LocalDate bundle, Word word) {
        List<Word> value =
            bundleMap.getOrDefault(bundle, new ArrayList<>());
        value.add(word);
        bundleMap.put(bundle, value);
    }
    
    synchronized void addNewWord(Word word) {
        LocalDate date = word.getBundle();
        
        allWords.put(word.getWord(), word);
        
        List<Word> bundleList = bundleMap.get(date);
        if (bundleList == null) bundleList = new ArrayList();
        bundleList.add(word);
        bundleMap.put(date, bundleList);
        
        save();
    }
    
    synchronized boolean addNewBundle(LocalDate bundle, Collection<Word> words) {
        if (bundleMap.containsKey(bundle)) return false;
        
        List<Word> list = new ArrayList<>(words);
        
        list.forEach(word -> {
            word.setBundle(bundle);
            allWords.put(word.getWord(), word);
        });
        
        bundleMap.put(bundle, list);
        
        save();
        
        return true;
    }
    
    synchronized boolean deleteWord(String word) {
        Word wordToDelete = allWords.remove(word);
        if (wordToDelete == null) return false;
        
        LocalDate bundle = wordToDelete.getBundle();
        List<Word> words = bundleMap.get(bundle);
        if (words == null) return false;
        
        boolean deleted = words.remove(wordToDelete);
        if (words.isEmpty()) bundleMap.remove(bundle);
        
        save();
        
        return deleted;
    }
    
    synchronized void editWords(Map<Word, Word> map) {
        for (Map.Entry<Word, Word> entry : map.entrySet()) {
            Word originalWord = entry.getValue();
            Word editedWord = entry.getKey();
            
            if (!originalWord.getWord().equals(editedWord.getWord())) {
                allWords.remove(originalWord.getWord());
//                allWords.remove(editedWord.getWord());
            }
            
            LocalDate originalBundle = originalWord.getBundle();
            List<Word> words = bundleMap.get(originalBundle);
            
            words.remove(originalWord);
            words.remove(editedWord);
            
            if (words.isEmpty()) bundleMap.remove(originalBundle);
            
            allWords.put(editedWord.getWord(), editedWord);
            LocalDate editedBundle = editedWord.getBundle();
            List<Word> editedWords =
                bundleMap.getOrDefault(editedBundle, new ArrayList<>());
            editedWords.add(editedWord);
            bundleMap.put(editedBundle, editedWords);
        }
        
        save();
    }
    
    public void setComplexity(String word, WordComplexity complexity) {
        wordStats.setComplexity(word, complexity);
    }
    
    public void setLastPickedTimestamp(String word, long timestamp) {
        wordStats.setLastPickedTimestamp(word, timestamp);
    }
    
    void clearRedundancies() {
        wordStats.clearRedundancies(allWords);
        
        // check mp3 files
        Path soundDir = projectDir.resolve(Controller.SOUND_DIR_NAME);
        if (Files.notExists(soundDir) || !Files.isDirectory(soundDir)) return;
        
        try (DirectoryStream<Path> stream =
            Files.newDirectoryStream(soundDir, "*.{mp3}")) {
            List<Path> toDelete = new ArrayList<>();
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                fileName = fileName.substring(0, fileName.length() - 4);
                
                if (!allWords.containsKey(fileName) &&
                    !allWords.containsKey("to " + fileName))
                    toDelete.add(path);
            }
            
            toDelete.forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException ex) { }
            });
            System.err.println("Deleted redundant mp3 files");
        } catch (IOException ex) {
            System.err.println("Error while cleaning mp3 files");
        }
    }
    
    void destroy() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) { }

        wordStats.destroy();
    }
}
