package com.words.controller.words;

import com.words.controller.words.wordkinds.WordType;
import com.words.controller.words.wordpool.WordPool;

class RepeatWord extends WordDecorator {

    RepeatWord(WordPool wordPool) {
        super(wordPool);
        currentWord.setWordType(WordType.REPEAT); // required
    }
    
    @Override
    public WordType getWordType() {
        return currentWord.getWordType();
    }
}
