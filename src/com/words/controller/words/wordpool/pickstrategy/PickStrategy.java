package com.words.controller.words.wordpool.pickstrategy;

import com.words.controller.utils.Utils;
import com.words.controller.words.Word;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public interface PickStrategy {
    
    static final Random RAND = Utils.RANDOM;
    
    /**
     * Deletes and returns word from the list. Modifies list.
     * @param list list of words to work with
     * @return picked word according to strategy
     */
    Word nextWord(List<Word> list);
    
    /**
     * Inserts picked word into queue.
     * @param queue list or queue to insert into
     * @param word word which should be inserted
     */
    default void insertIntoQueue(LinkedList<Word> queue, Word word) {
        queue.add(word);
    }
    
    /**
     * Get probability of the last picked word.
     * Should be executed by word pool after {nextWord} method.
     * @return double between 0 and 1 as probability for last picked word
     */
    double getLastProbability();
}
