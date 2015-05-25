package com.words.controller.words;

import com.words.controller.words.wordkinds.WordComplexity;
import com.words.controller.words.wordpool.WordPool;

/**
 * Complexity wrapper. Self-deletes itself from the pool,
 * when complexity is lesser than specified in the constructor.
 * @author vlad
 */
class ComplexityWord extends WordWithCondition {
    
    ComplexityWord(Word word, WordPool wordPool, WordComplexity complexity) {
        super(word, wordPool,
            w -> w.getComplexity().isNotEasierThan(complexity));
    }
    
    @Override
    protected void conditionIsNotSatisfied() {
        // if wordPool is too small replace word with concrete word
        if (wordPool.size() == 2) wordPool.addWord(word);
        wordPool.deleteWord(this);
    }
    
    @Override
    public String toString() {
        return word.getWord() + "@" +
            word.getComplexity().toString().toLowerCase().substring(0, 2);
    }
}
