package com.words.controller.words;

import com.words.controller.words.wordkinds.WordComplexity;
import com.words.controller.words.wordkinds.WordType;
import com.words.controller.utils.DateTimeUtils;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents concrete word.
 * @author vlad
 */
class ConcreteWord implements Word {
    
    private String word;
    private String translation;
    
    private LocalDate date;
    
    private Path mp3File;
    
    // synonyms are initialized with empty value
    private String synonyms = "";
    
    private int timesPicked = 0;
    
    private WordType wordType = WordType.STANDARD;
    
    // time passed string, uses lazy instantiation
    private String timePassed = null;
    
    private WordComplexity complexity = WordComplexity.NORMAL;
    
    private long lastPickedTimestamp = 0L;
    private String lastPickedString = null; // uses lazy instantiation
        
    ConcreteWord() {
        word = null;
        translation = null;
        date = null;
        mp3File = null;
    }
    
    @Override
    public void setMp3File(Path mp3File) {
        this.mp3File = mp3File;
    }
    
    @Override
    public Path getMp3File() { return mp3File; }

    @Override
    public String getWord() { return word; }
    
    @Override
    public void setWord(String word) {
        this.word = word.trim().toLowerCase();
        this.mp3File = null;
    }

    @Override
    public String getTranslation() { return translation; }

    @Override
    public void setTranslation(String translation) {
        this.translation = translation.trim().toLowerCase();
    }

    @Override
    public LocalDate getBundle() { return date; }
    
    @Override
    public void setBundle(LocalDate date) { this.date = date; }
    
    @Override
    public String getBoth() { return word + " — " + translation; }
    
    @Override
    public String toString() { return getWord(); }

    /**
     * Defines how many days passed since word has been added.
     * Uses lazy calculation.
     * @return formatted string according to English rules
     */
    @Override
    public String timePassedString() {
        if (timePassed == null) timePassed =
            DateTimeUtils.getFormattedPeriod(date);
        return timePassed;
    }
    
    @Override
    public void hasBeenPicked(long millis, String previousWord) { 
        timesPicked++;
        setLastPickedTimestamp(millis);
        lastPickedString = DateTimeUtils.getFormattedPeriodFromMillis(millis);
    }

    @Override
    public void setWordType(WordType type) {
        wordType = type;
    }

    @Override
    public WordType getWordType() {
        return wordType;
    }

    @Override
    public String getSynonyms() {
        return synonyms;
    }
    
    @Override
    public void setSynonyms(String synonyms) {
        this.synonyms = synonyms.trim().toLowerCase();
    }
    
    @Override
    public long getLastPickedTimestamp() { return lastPickedTimestamp; }
    
    @Override
    public void setLastPickedTimestamp(long millis) { 
        lastPickedTimestamp = millis;
    } 

    @Override
    public String getLastPickedString() {
        if (timesPicked == 0) return "never";
        if (lastPickedString == null) {
            lastPickedString = DateTimeUtils.getFormattedPeriodFromMillis(
                lastPickedTimestamp);
        }
        
        return lastPickedString;
    }
    
    @Override
    public WordComplexity getComplexity() { return complexity; }
    
    @Override
    public void setComplexity(WordComplexity complexity) {
        this.complexity = complexity;
    }
    
    @Override
    public int getTimesPicked() { return timesPicked; }
    
    @Override
    public void setTimesPicked(int n) { timesPicked = n; }
    
    @Override
    public boolean isSingleWord() { return true; }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 61 * hash + Objects.hashCode(this.word);
        hash = 61 * hash + Objects.hashCode(this.translation);
        hash = 61 * hash + Objects.hashCode(this.date);
        hash = 61 * hash + Objects.hashCode(this.synonyms);
        hash = 61 * hash + this.timesPicked;
        hash = 61 * hash + Objects.hashCode(this.wordType);
        hash = 61 * hash + Objects.hashCode(this.complexity);
        hash = 61 * hash + (int) (this.lastPickedTimestamp ^ (this.lastPickedTimestamp >>> 32));
        
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (getClass() != obj.getClass()) return false;
        final ConcreteWord other = (ConcreteWord) obj;
        
        if (!Objects.equals(this.word, other.word)) return false;
        if (!Objects.equals(this.translation, other.translation)) return false;
        if (!Objects.equals(this.date, other.date)) return false;
        if (!Objects.equals(this.synonyms, other.synonyms)) return false;
        if (this.timesPicked != other.timesPicked) return false;
        if (this.wordType != other.wordType) return false;
        if (this.complexity != other.complexity) return false;
        if (this.lastPickedTimestamp != other.lastPickedTimestamp) return false;
        
        return true;
    }
}
