package com.words.controller.words.wordpool.pickstrategy.weight;

import com.words.controller.utils.DateTimeUtils;
import com.words.controller.words.Word;
import java.time.Duration;
import java.time.LocalDate;

/**
 * This weighter increases weight for recent words.
 * Other weights stay the same as calculated in DurationWeighter class.
 * @author vlad
 */
public class RecentDurationWeighter extends DurationWeighter {
    
    private static final int FACTOR = 5;
    private static final LocalDate DEADLINE =
        DateTimeUtils.getCurrentLocalDate().minusMonths(6);
    
    public RecentDurationWeighter(Duration duration, int w8) {
        super(duration, w8);
    }
    
    @Override
    public int getWeight(Word word, long currentTime) {
        int w8 = super.getWeight(word, currentTime);
        if (word.getBundle().isAfter(DEADLINE)) w8 *= FACTOR;
        return w8;
    }
}
