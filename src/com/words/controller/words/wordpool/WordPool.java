package com.words.controller.words.wordpool;

import com.words.controller.callbacks.ConsoleCallback;
import com.words.controller.words.Word;
import com.words.controller.words.wordkinds.WordType;
import com.words.controller.words.wordpool.pickstrategy.PickStrategy;
import com.words.controller.words.wordpool.pickstrategy.PickStrategyFactory;
import com.words.controller.words.wordpool.pickstrategy.UniformStrategy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WordPool {
    
    private static final int DEFAULT_MAX_QUEUE_SIZE = 1000;
    
    private static double lastWordProbability = 1.0d;
    
    private Runnable drainedWordPoolAction = null;
    
    protected PickStrategy pickStrategy;
    
    private int maxQueueSize = 0;
    private final LinkedList<Word> queue = new LinkedList<>();
    
    protected final List<Word> list = new ArrayList<>();
    
    public WordPool() {
        this.pickStrategy = PickStrategyFactory.getUniformStrategy();
    }
    
    public WordPool(PickStrategy strategy) {
        this.pickStrategy = strategy;
    }
    
    public void setDrainedWordPoolAction(Runnable r) {
        drainedWordPoolAction = r;
    }
    
    public void setPickStrategy(PickStrategy strategy) {
        this.pickStrategy = strategy;
    }
    
    private void updateMaxQueueSize() {
        maxQueueSize = size() / 2;
        if (maxQueueSize > DEFAULT_MAX_QUEUE_SIZE)
            maxQueueSize = DEFAULT_MAX_QUEUE_SIZE;
    }
    
    /**
     * Adds collection of words to the pool.
     * @param words any collection to add
     */
    public final void addWords(Collection<Word> words) {
        list.addAll(words);
        updateMaxQueueSize();
    }
    
    /**
     * Adds word to the pool.
     * @param word word to add
     */
    public void addWord(Word word) {
        list.add(word);
        updateMaxQueueSize();
    }
    
    /**
     * Adds word to the end of the queue. Adjusts queue size.
     * @param word word to add
     */
    public void addWordToQueue(Word word) {
        queue.add(word);
        adjustQueueSize();
    }
    
    /**
     * Adds word to the current queue according the underlying strategy.
     * @param word word to add
     */
    public void insertIntoQueue(Word word) {
        pickStrategy.insertIntoQueue(queue, word);
        adjustQueueSize();
    }
    
    /** @return number of words in this word pool */
    public int size() {
        return list.size() + queue.size();
    }
    
    /**
     * Get next random word.
     * Strategy depends on concrete implementation.
     * @param timestamp time when method has been executed
     * @param previousWord previous word
     * @return next word to show
     */
    public Word nextWord(long timestamp, String previousWord) {
        lastWordProbability = 1.0d;
        
        Word word = pickStrategy.nextWord(list);
        pickStrategy.insertIntoQueue(queue, word);
        if (queue.size() > maxQueueSize) list.add(queue.remove());
        
        word.hasBeenPicked(timestamp, previousWord);
        
        lastWordProbability *= pickStrategy.getLastProbability();

        return word;
    }
    
    /**
     * Check if specified word is in this word pool.
     * Since english word is an unique id. This operation is safe.
     * @param word english word to check
     * @return true if word has been deleted
     */
    public boolean containsWord(String word) {
        Objects.requireNonNull(word);
        
        return Stream.concat(queue.stream(), list.stream())
            .filter(Word::isSingleWord)
            .anyMatch(w -> w.getWord().equals(word));
    }
    
    /**
     * Deletes word and corrects pool size.
     * If word pool is drained can execute special action presetted with 
     * setDrainedWordPoolAction().
     * @param word word to delete
     * @return true if word was successfully deleted as a result of this call
     */
    public boolean deleteWord(Word word) {
        if (size() <= 2) {
            if (drainedWordPoolAction != null) drainedWordPoolAction.run();
            return false;
        }
        
        if (list.remove(word)) {
            adjustQueueSize();
            return true;
        }
        
        if (queue.remove(word)) {
            updateMaxQueueSize();
            return true;
        }
        
        return false;
    }
    
    /**
     * Get formatted string of current words
     * @return string typically delimited by commas
     */
    public String getWordsAsString() {
        return Stream.concat(list.stream(), queue.stream())
            .filter(w -> w.getWordType() == WordType.STANDARD)
            .map(Word::getWord).collect(Collectors.joining(", "));
    }
    
    /**
     * Delete all words from this word pool.
     */
    public void clear() {
        maxQueueSize = 0;
        list.clear();
        queue.clear();
    }
    
    /**
     * Dump current word pool to the console.
     * Shows queue and list.
     * @param console console to show dump
     * @param msg message to show before or {null} to skip it
     */
    public void dumpWordPool(ConsoleCallback console, String msg) {
        if (console == null)
            throw new IllegalArgumentException("Console can't be null");
        
        final int MIN_FIRST_COL_WIDTH = 15;
        final int GAP = 3;
        
        int firstColWidth = list.stream().mapToInt(w -> w.toString().length())
            .max().orElse(MIN_FIRST_COL_WIDTH - GAP);
        firstColWidth += GAP;
        if (firstColWidth < MIN_FIRST_COL_WIDTH)
            firstColWidth = MIN_FIRST_COL_WIDTH;
        final String FORMAT = "%-" + firstColWidth + "s%s%n";
        
        StringBuilder dump = new StringBuilder();
        dump.append(String.format(FORMAT, "list", "queue"));
        dump.append(String.format(FORMAT, "----------", "-----------"));
        
        int max = list.size() > queue.size() ? list.size() : queue.size();
        
        List<Word> queueList = new ArrayList<>(queue);
        
        for (int i = 0; i < max; i++) {
            String listWord = i < list.size() ?
                list.get(i).toString() : "";
            
            // empty string, empty message or specifically formatted string
            String queueWord = "";
            if (i < queue.size()) {
                queueWord = String.format(
                    "%" + ((int) Math.log10(queue.size()) + 1) + "d: %s",
                    i + 1, queueList.get(i));
            } else if (queue.size() == 0 && i == 0) {
                queueWord = "{queue is empty}";
            }
            
            dump.append(String.format(FORMAT, listWord, queueWord));
        }
        
        if (msg != null) console.addInfoMessage(msg);
        console.addInfoMessage(
            (list.size() + queue.size()) + " words in the pool:");
        console.addMessage(dump.toString());
    }
    
    public boolean isEmpty() {
        return size() == 0;
    }
    
    /**
     * Force word pool to adjust it's size.
     */
    public void adjustQueueSize() {
        updateMaxQueueSize();
        while (queue.size() > maxQueueSize) list.add(queue.remove());
    }
    
    /**
     * Add words to queue.
     * @param words collection of words to add
     */
    public void addWordsToQueue(Collection<Word> words) {
        queue.addAll(words);
    }
    
    /**
     * Since controller can pick only 1 word at a time this method returns
     * probability for that word and uses static field to
     * combine multiple probabilities for decorator words.
     * @return probability for last picked word (0 < probability <= 1)
     */
    public static double getLastWordProbability() {
        return lastWordProbability;
    }
}
