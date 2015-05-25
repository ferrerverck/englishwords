package com.words.controller.words.wordpool;

import com.words.controller.words.Word;
import com.words.controller.words.wordpool.pickstrategy.PickStrategy;

public class CommaWordPool extends WordPool {
    
    @Override
    public Word nextWord(long timestamp, String previousWord) {
        Word word;
        do {
            word = pickStrategy.nextWord(list);
        } while (!word.getTranslation().contains(","));
        
        System.err.println(list.size());
        
        word.hasBeenPicked(timestamp, previousWord);
        
        return word;
    }
}
