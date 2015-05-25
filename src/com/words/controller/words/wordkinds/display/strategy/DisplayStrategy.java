package com.words.controller.words.wordkinds.display.strategy;

import com.words.controller.words.Word;
import com.words.controller.words.wordkinds.display.WordDisplayType;

/**
 * Used to pick next word display type.
 * @author vlad
 */
public interface DisplayStrategy {
    
    /**
     * Get word display type depending on number of previous iterations or 
     * any information about word.
     * @param iters iterations
     * @param word word to display
     * @return WordDisplayType display type for current iteration
     */
    WordDisplayType getNextType(int iters, Word word);
}
