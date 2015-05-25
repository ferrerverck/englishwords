package com.words.controller.futurewords;

import com.words.controller.utils.DateTimeUtils;
import com.words.controller.utils.Utils;
import java.util.Objects;
import java.util.Random;

/**
 * Class represents future word.
 * Priority is required generate new bundles.
 * Date can be null.
 * @author vlad
 */
public class FutureWord implements Comparable<FutureWord> {
    
    private static final int EXPIRATION_DAYS = 21;
    
    public static final int FACTOR = 1000;
    private static final Random RAND = Utils.RANDOM;

    private int priority = 0;
    
    private String word;
    
    private String dateAdded = null;
    private String dateChanged = null;

    public FutureWord(String word) { this.word = word; }
    
    public String getDateChanged() { return dateChanged; }

    public void setDateChanged(String dateChanged) { 
        this.dateChanged = dateChanged;
    }

    public void setWord(String word) { this.word = word; }
    
    public void setPriority(int pr) {
        priority = pr * FACTOR + RAND.nextInt(FACTOR);
    }
    
    public void setDateAdded(String dateAdded) { this.dateAdded = dateAdded; }

    /**
     * Increase priority by 1.
     * @param newDate new dateAdded of current word
     */
    public void incPriority(String newDate) {
        priority += FACTOR;
        dateChanged = newDate;
    }

    public String getWord() { return word; }
     
    public String getDateAdded() { return dateAdded; }

    @Override
    public String toString() {
        return "[FutureWord word=" + word + "; priority="  + priority +
            "; dateAdded=" + dateAdded + "; dateChanged=" + dateChanged + "]";
    }

    @Override
    public int compareTo(FutureWord that) {
        return this.priority > that.priority ? -1 : 
            this.priority < that.priority ? 1 :
            this.word.compareTo(that.word);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + this.priority;
        hash = 59 * hash + Objects.hashCode(this.word);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final FutureWord other = (FutureWord) obj;
        if (this.priority != other.priority) return false;
        if (!Objects.equals(this.word, other.word)) return false;
        return true;
    }
    
    /**
     * Defines if enough days have passed after word dateAdded.
     * @return true if enough amount of time have passed.
     */
    public boolean hasEnoughTimePassed() {
        String date;
        if (dateChanged == null) {
            if (dateAdded != null) date = dateAdded;
            else return true;
        } else date = dateChanged;
        
        return DateTimeUtils.isExpired(date, EXPIRATION_DAYS);
    }
    
    /**
     * Get word priority.
     * @return priority x1000 as integer
     */
    public int getPriority() { return priority; }
    
    public int getOriginalPriority() { return priority / FACTOR; }
}
