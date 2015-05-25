package com.words.controller.words;

import com.words.controller.words.wordkinds.WordType;
import com.words.controller.words.wordpool.WordPool;

class RandomWord extends WordDecorator {
    
    RandomWord(WordPool wordPool) {
        super(wordPool);
    }

    @Override
    public WordType getWordType() {
        return currentWord.getWordType() == WordType.REPEAT ?
            WordType.REPEAT : WordType.RANDOM;
    }
}
