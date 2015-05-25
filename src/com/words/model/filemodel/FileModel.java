package com.words.model.filemodel;

import com.words.controller.futurewords.FutureWord;
import com.words.controller.words.Word;
import com.words.controller.words.wordkinds.WordType;
import com.words.model.Model;
import com.words.controller.utils.DateTimeUtils;
import com.words.controller.utils.Utils;
import com.words.controller.words.wordkinds.WordComplexity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.stream.Collectors;

public class FileModel implements Model {
    
    private final String todayAsString;
    
    private final IterationLog iterations;
    
    private final Path projectDir;
    
    private final WordManager wordManager;
    private final RepeatWords repeatWords;
    private final WordDefinitions definitions;
    private final FutureWordManager futureWordManager;
    
    public FileModel(Path projectDir) throws IOException {
        this.projectDir = projectDir;
        
        if (Files.notExists(projectDir))
            Files.createDirectories(projectDir);
        
        todayAsString = DateTimeUtils.todayAsString();
        
        definitions = new WordDefinitions(projectDir);
        
        iterations = new IterationLog(projectDir);
        
        futureWordManager = new FutureWordManager(projectDir);
        
        wordManager = new WordManager(projectDir);
        
        repeatWords = new RepeatWords(projectDir);
        
        // mark repeat words
        getRepeatWords().forEach(word -> word.setWordType(WordType.REPEAT));
    }
    
    @Override
    public void backup() {
        System.err.println("Clearing and backuping");
        
        definitions.clearRedundancies(wordManager.getAllWords());
        wordManager.clearRedundancies();
        
        try {
            BackupFileModel bfm = new BackupFileModel(projectDir);
            bfm.backup();
            
            System.err.println("Completed");
        } catch (IOException ex) { }
    }
    
    @Override
    public boolean wordExists(String wordToSearch) {
        Objects.requireNonNull(wordToSearch);
        
        wordToSearch = wordToSearch.replace("^to ", "");
        return wordManager.getAllWords().get(wordToSearch) != null ||
            wordManager.getAllWords().get("to " + wordToSearch) != null;
    }
    
    @Override
    public Word getWordInstance(String wordToSearch) {
        Objects.requireNonNull(wordToSearch);
        return wordManager.getAllWords().get(wordToSearch);
    }
    
    @Override
    public final Collection<Word> getRepeatWords() {
        return repeatWords.getRepeatWords().stream()
            .filter(wordManager.getAllWords()::containsKey) // filter invalid keys
            .map(wordManager.getAllWords()::get)
            .collect(Collectors.toSet());
    }
    
    @Override
    public void addRepeatWord(String word) {
        repeatWords.addRepeatWord(word);
    }
    
    @Override
    public void deleteRepeatWord(String word) {
        repeatWords.deleteRepeatWord(word);
    }
    
    @Override
    public int getTodayIterations() {
        return iterations.getIterations(todayAsString);
    }
    
    @Override
    public long getTotalIterations() {
        return wordManager.getAllWords().values().stream()
            .mapToLong(Word::getTimesPicked).sum();
    }
    
    
    @Override
    public int getThisWeekIterations() {
        LocalDate today = DateTimeUtils.parseDate(todayAsString);
        return getIterationsForDays(today.getDayOfWeek().getValue());
    }
    
    @Override
    public int getIterationsForDays(int n) {
        return iterations.getIterationsForDays(n, todayAsString);
    }
    
    @Override
    public boolean isExistingBundle(LocalDate bundle)  {
        Objects.requireNonNull(bundle);
        return wordManager.getBundleMap().containsKey(bundle);
    }
    
    @Override
    public void setTodayIterations(int iter) {
        iterations.setIterations(todayAsString, iter);
    }
    
    @Override
    public Collection<Word> getLastWords() {
        return wordManager.getBundleMap().lastEntry().getValue();
    }
    
    @Override
    public Map<String, Word> getAllWords() {
        return Collections.unmodifiableMap(wordManager.getAllWords());
    }
    
    @Override
    public Collection<Word> getBundle(LocalDate bundle) {
        if (bundle == null) return Collections.emptyList();
        return wordManager.getBundleMap()
            .getOrDefault(bundle, Collections.emptyList());
    }
    
    @Override
    public Map<String, FutureWord> getFutureWords() {
        return futureWordManager.getFutureWords();
    }
    
    @Override
    public void updateFutureWord(String word) {
        futureWordManager.updateFutureWord(word);
    }
    
    @Override
    public void deleteFutureWords(Collection<String> words) {
        futureWordManager.deleteFutureWords(words);
    }
    
    @Override
    public boolean addNewWord(Word word) {
        wordManager.addNewWord(word);
        return true;
    }
    
    @Override
    public boolean deleteWord(String word) {
        if (word == null) return false;
        return wordManager.deleteWord(word);
    }
    
    @Override
    public void editWords(Map<Word, Word> map) {
        wordManager.editWords(map);
        definitions.editWords(map);
    }
    
    @Override
    public NavigableSet<LocalDate> allBundlesSorted() {
        return (NavigableSet<LocalDate>) wordManager.getBundleMap().keySet();
    }
    
    @Override
    public LocalDate getLastBundleName() {
        return allBundlesSorted().last();
    }
    
    @Override
    public Collection<Word> getExpiredRepeatWords() {
        Collection<Word> expiredWords = new HashSet<>();
        
        repeatWords.getExpiredRepeatWords().stream().forEach(
            key -> expiredWords.add(wordManager.getAllWords().get(key)));
        expiredWords.remove(null);
        
        return expiredWords;
    }
    
    @Override
    public String getDefinition(String word) {
        return definitions.getDefinition(word);
    }
    
    @Override
    public void setDefinition(String word, String definition) {
        definitions.setDefinition(
            Utils.normalizeFor3rdParties(word), definition);
    }
    
    @Override
    public void setComplexity(String word, WordComplexity complexity) {
        wordManager.setComplexity(word, complexity);
    }
    
    @Override
    public void setLastPickedTimestamp(String word, long timestamp) {
        wordManager.setLastPickedTimestamp(word, timestamp);
    }
    
    @Override
    public boolean addNewBundle(LocalDate bundle, Collection<Word> words) {
        return wordManager.addNewBundle(bundle, words);
    }
    
    @Override
    public boolean isEmpty() {
        return wordManager.getAllWords().isEmpty();
    }
    
    @Override
    public void destroy() {
        wordManager.destroy();
        repeatWords.destroy();
        definitions.destroy();
        futureWordManager.destroy();
        iterations.destroy();
    }
}
