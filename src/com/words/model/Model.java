package com.words.model;

import com.words.controller.futurewords.FutureWord;
import com.words.controller.utils.DateTimeUtils;
import com.words.controller.words.Word;
import com.words.controller.words.wordkinds.WordComplexity;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableSet;

public interface Model {
    
    /**
     * Get Ebbinghaus words.
     * @return collection with words (can be empty)
     */
    default Collection<Word> getEbbinghausWords() {
        int additionalDays = DateTimeUtils.isOddDayOfMonth() ? 0 : 7;
        final int[] EBBINGHAUS_DAYS_TO_PASS = {21 + additionalDays,
            100 - additionalDays};
        
        NavigableSet<LocalDate> allBundles = allBundlesSorted();
        Collection<Word> list = new ArrayList<>(150);
        for (int days : EBBINGHAUS_DAYS_TO_PASS) {
            LocalDate bundle =
                DateTimeUtils.getNearestBundle(days, allBundles);
            if (bundle != null) list.addAll(getBundle(bundle));
        }
        
        return list;
    }
    
    /**
     * Get Word instance for specified string word.
     * @param wordToSearch word to search in the model
     * @return word instance or null if word doesn't exist
     * @throws NullPointerException null passed as argument
     */
    Word getWordInstance(String wordToSearch);
    
    /**
     * Method checks if word exists in the current model.
     * Should treat verbs starting with "to " and nouns as being the same.
     * @param wordToSearch word to search
     * @return true if word exists
     * @throws NullPointerException if wordToSearch is null
     */
    boolean wordExists(String wordToSearch);
    
    /**
     * Get last bundle.
     * @return list with last words
     */
    default Collection<Word> getLastWords() {
        return getBundle(getLastBundleName());
    }
    
    /**
     * Get all words. Should return unmodifiable map.
     * @return all words map
     */
    Map<String, Word> getAllWords();
    
    /**
     * Get today iterations. Today ends and 6 A.M. the next day in the morning.
     * @return today iterations
     */
    int getTodayIterations();
    
    /**
     * Set today iterations. Don't forget about 6 A.M. rule.
     * @param iter new iterations for today
     */
    void setTodayIterations(int iter);
    
    /**
     * Get total amount of iterations.
     * @return total iteration count
     */
    long getTotalIterations();
    
    /**
     * Get this week iterations. Current week starts on Monday.
     * @return amount of this week iterations without today
     */
    int getThisWeekIterations();
    
    /**
     * Get iterations for last n days. If n <= 0 returns 0.
     * For n = 1 returns also 0 because controller has its own iteration
     * counter. Which he should add to result of this call!
     * @param n amount of days
     * @return iteration for last n days without today
     */
    int getIterationsForDays(int n);
    
    /**
     * Check if bundle already exists.
     * @param bundle bundle
     * @return true if bundle exists
     * @throws NullPointerException if bundle is null
     */
    boolean isExistingBundle(LocalDate bundle);
    
    /**
     * Get all words from bundle.
     * @param bundle string bundle name
     * @return words of specified bundle or empty list if bundle doesn't exist
     *         returns empty collections if argument is null
     */
    Collection<Word> getBundle(LocalDate bundle);
    
    /**
     * Get last bundle name.
     * @return last bundle or null if it doesn't exist
     */
    LocalDate getLastBundleName();
    
    /**
     * Get penultimate bundle name.
     * @return name of the penultimate bundle or null if it doesn't exist
     */
    default LocalDate getPenultimateBundleName() {
        NavigableSet<LocalDate> bundles = allBundlesSorted();
        NavigableSet<LocalDate> bundlesWithoutLast =
            bundles.headSet(bundles.last(), false);
        
        if (bundlesWithoutLast.isEmpty()) return null;
        return bundlesWithoutLast.last();
    }
    
    /**
     * Repeat words
     * @return words to repeat
     */
    Collection<Word> getRepeatWords();
    
    /**
     * List all expired repeat words.
     * @return collection of all the words expired today
     */
    Collection<Word> getExpiredRepeatWords();
    
    /**
     * Add new repeating word.
     * @param word new word
     */
    void addRepeatWord(String word);
    
    /**
     * Delete repeat word.
     * @param word word to delete
     */
    void deleteRepeatWord(String word);
    
    /**
     * Save word for a long storage to a hard disk or into a database.
     * @param word word instance to add
     * @return true if words has been successfully added
     */
    boolean addNewWord(Word word);
    
    /**
     * Deletes specified word.
     * @param word to delete as string
     * @return true if model has been changed
     *         if word is null returns false
     */
    boolean deleteWord(String word);
    
    /**
     * Edits and saves words.
     * @param map map key = edited word and value = original word
     */
    void editWords(Map<Word, Word> map);
    
    /**
     * List all available bundles. List should be sorted.
     * Most recent bundle goes last.
     * @return NavigableSet of available bundle names
     */
    NavigableSet<LocalDate> allBundlesSorted();
    
    /**
     * Definition for specific word
     * @param word word to check
     * @return definition of specified word
     *         or null if there is no definition
     */
    String getDefinition(String word);
    void setDefinition(String word, String definition);
    
    void setComplexity(String word, WordComplexity complexity);
    
    /**
     * This method sets lastPickedTimestamp and increases timesPicked field.
     * @param word word
     * @param timestamp new timestamp
     */
    void setLastPickedTimestamp(String word, long timestamp);
    
    boolean addNewBundle(LocalDate bundle, Collection<Word> words);
    
    /**
     * Mechanism for working with future words.
     * @return future word map
     */
    Map<String, FutureWord> getFutureWords();
    
    /**
     * Adds or increases priority of specified future word.
     * @param word word to insert / update
     */
    void updateFutureWord(String word);
    
    /**
     * Deletes future words.
     * @param words words to delete
     */
    void deleteFutureWords(Collection<String> words);
    
    /**
     * Returns true if this model contains no words.
     * @return true if this model contains no words
     */
    boolean isEmpty();
    
    /**
     * Completely destroys current model. After an execution of this method 
     * application can't function properly and model is lost forever
     */
    default void destroy() {
        throw new UnsupportedOperationException("destroy");
    }
        
    /**
     * Backup method.
     * Can be used by controller to save model state.
     */
    void backup();
}
