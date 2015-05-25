package com.words.controller.words.wordpool.pickstrategy.weight;

import com.words.controller.words.Word;
import java.time.Duration;
import java.util.Objects;

/**
 * Class is required to change word weights according to time passed since 
 * word last time has been picked. Duration is the time period after which 
 * weight should be increased. w8 is a weight to add.
 * @author vlad
 */
public class DurationWeighter implements Weighter {
    
    private final long durationMillis;
    private final int w8;
    
    public DurationWeighter(Duration duration, int w8) {
        Objects.requireNonNull(duration);
        
        this.durationMillis = duration.toMillis();
        if (durationMillis <= 0L) throw new IllegalArgumentException(
            "Duration can't be less or equal to zero");
        
        this.w8 = w8;
    }
    
    @Override
    public int getWeight(Word word, long currentTime) {
        if (!word.isSingleWord()) return 0;
        return durationToWeight(word, currentTime);
    }
    
    private int durationToWeight(Word word, long currentTime) {
        long timePassed = currentTime - word.getLastPickedTimestamp();
        
        if (timePassed <= 0) return 0;
        
        return (int) (timePassed / durationMillis) * w8;
    }
}
