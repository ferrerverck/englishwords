package com.words.controller.words;

import com.words.controller.words.wordkinds.WordComplexity;
import com.words.controller.words.wordkinds.WordType;
import com.words.controller.words.wordpool.WordPool;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Class is created for inheritance.
 * Encapsulates logic of bounded sets for special kind of words.
 * Like Repeat words or Ebbinghaus words.
 * wordPool defines strategy to pick next word and
 * injected through constructor
 * @author vlad
 */
abstract class WordDecorator implements Word {
    
    private final WordPool wordPool;
    protected Word currentWord = new ConcreteWord();
    private long lastPickedTimestamp = System.currentTimeMillis();
    
    private int timesPicked = 0;
    
    WordDecorator(WordPool wordPool) {
        this.wordPool = wordPool;
    }
    
    private void nextWord(long millis, String previousWord) {
        int i = 0;
        
        do {
            if (i != 0) System.err.println("Collision detected: " + currentWord);
            currentWord = wordPool.nextWord(millis, previousWord);
            if (previousWord == null) return;
        } while (currentWord.getWord().equals(previousWord)  && (++i <= 3));
    }
    
    @Override
    public String timePassedString() {
        return currentWord.timePassedString();
    }
    
    @Override
    public String getBoth() {
        return currentWord.getBoth();
    }
    
    @Override
    public LocalDate getBundle() { return currentWord.getBundle(); }
    
    @Override
    public void setBundle(LocalDate date) { currentWord.setBundle(date); }
    
    @Override
    public Path getMp3File() {
        return currentWord.getMp3File();
    }
    @Override
    public String getTranslation() {
        return currentWord.getTranslation();
    }
    
    @Override
    public String getWord() {
        return currentWord.getWord();
    }
    
    @Override
    public void setMp3File(Path mp3File) { currentWord.setMp3File(mp3File); }
    
    @Override
    public void setTranslation(String translation) {
        currentWord.setTranslation(translation);
    }
    
    @Override
    public void setWord(String word) {
        currentWord.setWord(word);
    }
    
    @Override
    public void hasBeenPicked(long millis, String previousWord) {
        nextWord(millis, previousWord);
        setLastPickedTimestamp(millis);
        currentWord.hasBeenPicked(millis, previousWord);
        timesPicked++;
    }
    
    @Override
    public void setWordType(WordType type) {
        currentWord.setWordType(type);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + timesPicked;
    }
    
    @Override
    public String getSynonyms() {
        return currentWord.getSynonyms();
    }
    
    @Override
    public void setSynonyms(String synonyms) {
        currentWord.setSynonyms(synonyms);
    }
    
    @Override
    public void setComplexity(WordComplexity complexity) { 
        currentWord.setComplexity(complexity);
    }
    
    @Override
    public WordComplexity getComplexity() {
        return currentWord.getComplexity();
    }
    
    @Override
    public long getLastPickedTimestamp() {
        return lastPickedTimestamp;
    }
    
    @Override
    public void setLastPickedTimestamp(long millis) {
        lastPickedTimestamp = millis;
        currentWord.setLastPickedTimestamp(millis);
    }
    
    @Override
    public String getLastPickedString() { 
        return currentWord.getLastPickedString();
    }

    @Override
    public int getTimesPicked() {
        return currentWord.getTimesPicked();
    }
    
    @Override
    public void setTimesPicked(int n) { }
    
    @Override
    public boolean isSingleWord() { return false; }
}
