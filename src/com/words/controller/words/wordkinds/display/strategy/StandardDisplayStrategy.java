package com.words.controller.words.wordkinds.display.strategy;

import com.words.controller.words.Word;
import com.words.controller.words.wordkinds.WordComplexity;
import com.words.controller.words.wordkinds.display.WordDisplayType;
import static com.words.controller.words.wordkinds.display.WordDisplayType.BOTH;
import static com.words.controller.words.wordkinds.display.WordDisplayType.TRANSLATION;
import static com.words.controller.words.wordkinds.display.WordDisplayType.WORD;

class StandardDisplayStrategy extends RandomDisplayStrategy {
    
    private static final int PHASE_ITERATIONS = 50;
    
    @Override
    public WordDisplayType getNextType(int iters, Word word) {
        if (iters < 3 * PHASE_ITERATIONS && word.isSingleWord() &&
            !word.getComplexity().isNotHarderThan(WordComplexity.EASY)) {
            if (iters < PHASE_ITERATIONS) return BOTH;
            if (iters < 2 * PHASE_ITERATIONS) return WORD;
            if (iters < 3 * PHASE_ITERATIONS) return TRANSLATION;
        }
        
        return super.getNextType(iters, word);
    }
}
