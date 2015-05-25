package com.words.controller.words.wordkinds.display.strategy;

import com.words.controller.words.Word;
import com.words.controller.words.wordkinds.display.WordDisplayType;

class TranslationDisplayStrategy implements DisplayStrategy {
    
    @Override
    public WordDisplayType getNextType(int iters, Word word) {
        return WordDisplayType.TRANSLATION;
    }
}
