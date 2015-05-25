package com.words.controller.words.wordpool.pickstrategy.weight;

import com.words.controller.words.Word;

@FunctionalInterface
public interface Weighter {
    int getWeight(Word word, long currentTime);
}
