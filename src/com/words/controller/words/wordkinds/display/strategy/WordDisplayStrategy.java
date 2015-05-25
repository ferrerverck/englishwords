package com.words.controller.words.wordkinds.display.strategy;

import com.words.controller.words.Word;
import com.words.controller.words.wordkinds.display.WordDisplayType;

public class WordDisplayStrategy implements DisplayStrategy {

    @Override
    public WordDisplayType getNextType(int iters, Word word) {
        return WordDisplayType.WORD;
    }
}
