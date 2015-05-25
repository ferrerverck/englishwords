package com.words.controller.words.wordpool.pickstrategy;

import com.words.controller.words.wordkinds.WordComplexity;
import com.words.controller.words.wordpool.pickstrategy.weight.ComplexityWeigher;
import com.words.controller.words.wordpool.pickstrategy.weight.CompoundWeighter;
import com.words.controller.words.wordpool.pickstrategy.weight.DurationWeighter;
import com.words.controller.words.wordpool.pickstrategy.weight.RecentDurationWeighter;
import com.words.controller.words.wordpool.pickstrategy.weight.WeightStrategy;
import com.words.controller.words.wordpool.pickstrategy.weight.Weighter;
import java.time.Duration;

/**
 * Factory creates different strategies.
 * Strategies should be passed as a parameter to WordPool.
 * @author vlad
 */
public class PickStrategyFactory {
    
    private static final int DEFAULT_WEIGHT =
        WordComplexity.NORMAL.getWeight();
    
    private static final Weighter UNIFORM_WEIGHTER =
        (duration, w8) -> DEFAULT_WEIGHT;
    private static final Weighter STANDARD_WEIGHTER =
        new ComplexityWeigher();
    
    public PickStrategyFactory() { throw new AssertionError(); }
    
    public static PickStrategy getRepeatStrategy() {
        return new WeightStrategy(new CompoundWeighter(
            UNIFORM_WEIGHTER,
            new DurationWeighter(Duration.ofDays(1L), DEFAULT_WEIGHT)));
    }
    
    public static PickStrategy getEbbinghausStrategy() {
        return new WeightStrategy(new CompoundWeighter(UNIFORM_WEIGHTER,
            new DurationWeighter(Duration.ofHours(3L), WordComplexity.EASY.getWeight())));
    }
    
    public static PickStrategy getStandardStrategy(Duration duration, int weight) {
        return new WeightStrategy(new CompoundWeighter(STANDARD_WEIGHTER,
            new DurationWeighter(duration, weight)));
    }
    
    public static PickStrategy getRecentStandardStrategy(Duration duration, int weight) {
        return new WeightStrategy(new CompoundWeighter(STANDARD_WEIGHTER,
            new RecentDurationWeighter(duration, weight)));
    }
    
    public static PickStrategy getStandardEverydayStrategy() {
        return PickStrategyFactory.getStandardStrategy(
            Duration.ofHours(5L), WordComplexity.EASY.getWeight());
    }
    
    public static PickStrategy getDummyStrategy() {
        return new DummyStrategy();
    }
    
    public static PickStrategy getUniformStrategy() {
        return new UniformStrategy();
    }
}
