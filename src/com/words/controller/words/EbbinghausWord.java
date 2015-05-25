package com.words.controller.words;

import com.words.controller.words.wordkinds.WordType;
import com.words.controller.words.wordpool.WordPool;

class EbbinghausWord extends WordDecorator {
    
    EbbinghausWord(WordPool wordPool) {
        super(wordPool);
    }
    
    @Override
    public WordType getWordType() {
        return currentWord.getWordType() == WordType.REPEAT ?
            WordType.REPEAT : WordType.EBBINHAUS;
    }
}
