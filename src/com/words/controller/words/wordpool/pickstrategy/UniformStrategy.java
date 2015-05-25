package com.words.controller.words.wordpool.pickstrategy;

import com.words.controller.words.Word;
import java.util.List;

/**
 * Strategy with uniform distribution.
 * @author vlad
 */
public class UniformStrategy implements PickStrategy {

    private double probability;
    
    @Override
    public Word nextWord(List<Word> list) {
        probability = list.isEmpty() ? 0 : 1.0 / list.size();
        return list.remove(RAND.nextInt(list.size()));
    }

    @Override
    public double getLastProbability() {
        return probability;
    }
}