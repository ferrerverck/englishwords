package com.words.controller.words;

import com.words.controller.callbacks.ConsoleCallback;
import com.words.controller.words.wordpool.WordPool;
import java.util.function.Predicate;

class SelfDeletingWord extends WordWithCondition {
    
    private static final int DELETE_AFTER = 4;
    
    private int i = 0;
    private final ConsoleCallback console;
    
    SelfDeletingWord(Word word, WordPool wordPool,
        Predicate<Word> shouldHoldCondition, ConsoleCallback console) {
        super(word, wordPool, shouldHoldCondition);
        andCondition(w -> i < DELETE_AFTER);
        this.console = console; // console can be null
    }
    
    @Override
    protected void conditionIsNotSatisfied() {
        // if wordPool is too small replace word with concrete word
        if (wordPool.size() == 2) wordPool.addWord(word);
        wordPool.deleteWord(this);
        
        if (console != null) console.addErrorMessage(
            "Word «" + word.getWord() + "» has been deleted from the pool");
    }
    
    @Override
    public void hasBeenPicked(long millis, String previousWord) {
        i++;
        super.hasBeenPicked(millis, previousWord);
    }
    
    @Override
    public String toString() { return word.getWord() + "@" + i; }
}
