package com.words.controller.words.wordpool.pickstrategy.weight;

import com.words.controller.words.Word;
import java.util.ArrayList;
import java.util.List;

/**
 * Compound weighter. Incorporates different weighters.
 * @author vlad
 */
public class CompoundWeighter implements Weighter {

    private final Weighter w1, w2;
    
    public CompoundWeighter(Weighter w1, Weighter w2) {
        this.w1 = w1;
        this.w2 = w2;
    }

    @Override
    public int getWeight(Word word, long currentTime) {
        return w1.getWeight(word, currentTime) + w2.getWeight(word, currentTime);
    }
}
