package com.words.controller.words.wordpool.pickstrategy.weight;

import com.words.controller.words.Word;
import java.time.Duration;

public class DurationComplexityWeighter implements Weighter {
    
    private final Weighter complexityWeighter = new ComplexityWeigher();
    private final Weighter durationWeighter;
    
    public DurationComplexityWeighter(Duration duration, int w8) {
        durationWeighter = new DurationWeighter(duration, w8);
    }
    
    @Override
    public int getWeight(Word word, long currentTime) {
        return complexityWeighter.getWeight(word, currentTime) +
            durationWeighter.getWeight(word, currentTime);
    }
}
