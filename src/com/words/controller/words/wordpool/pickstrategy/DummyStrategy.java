package com.words.controller.words.wordpool.pickstrategy;

import com.words.controller.words.Word;
import java.util.List;

/**
 * Strategy always returns the very first word in a list.
 * @author vlad
 */
public class DummyStrategy implements PickStrategy {
    
    @Override
    public Word nextWord(List<Word> list) {
        return list.remove(0);
    }

    @Override
    public double getLastProbability() {
        return 1.0;
    }
}
