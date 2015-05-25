package com.words.controller.words;

import com.words.controller.words.wordkinds.WordComplexity;
import com.words.controller.words.wordkinds.WordType;
import java.nio.file.Path;
import java.time.LocalDate;

public interface Word {
    
    public static final String TRANSLATION_DELIMITER = ";";
    public static final String THIRD_FIELD_DESCRIPTION = "Synonyms";
    
    String getWord();
    void setWord(String word);
    
    String getTranslation();
    void setTranslation(String translation);
    
    LocalDate getBundle();
    void setBundle(LocalDate date);
    
    String getBoth();
    
    /**
     * Set and get mp3 file for current word.
     * This is a responsibility of the controller.
     * Originally the file is null. Controller uses lazy instantiation.
     * @return path the the mp3 file for this word
     */
    Path getMp3File();
    void setMp3File(Path mp3File);
    
    /**
     * Define how much time passed since word has been added.
     * @return formatted string according to English rules
     */
    String timePassedString();
    
    String getSynonyms();
    
    void setSynonyms(String synonyms);
    
    /**
     * Notify word it has been picked.
     * @param millis time in milliseconds since epoch
     * @param previousWord previous word unique id
     */
    void hasBeenPicked(long millis, String previousWord);
    
    WordType getWordType();
    void setWordType(WordType type);

    WordComplexity getComplexity();
    void setComplexity(WordComplexity complexity);
    
    void setLastPickedTimestamp(long millis);
    long getLastPickedTimestamp();
    
    /**
     * Methods is used to show how many time passed since last pick.
     * @return formatted string lime (1 day or 1 year 1 month, etc)
     */
    String getLastPickedString();

    int getTimesPicked();
    void setTimesPicked(int n);
    
    /**
     * Tells if words isn't a wrapper class instance.
     * @return true if word is a single concrete instance
     */
    boolean isSingleWord();
}
