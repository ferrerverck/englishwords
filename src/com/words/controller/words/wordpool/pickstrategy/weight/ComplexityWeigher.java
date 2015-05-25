package com.words.controller.words.wordpool.pickstrategy.weight;

import com.words.controller.words.Word;
import com.words.controller.words.wordkinds.WordComplexity;
import com.words.controller.words.wordkinds.WordType;

/**
 * Class maps word complexity to its weight.
 * For single words returns their current complexity w8.
 * For repeat words returns tough w8.
 * For others normal w8.
 * @author vlad
 */
public class ComplexityWeigher implements Weighter {

    @Override
    public int getWeight(Word word, long currentTime) {
        if (word.isSingleWord()) return word.getComplexity().getWeight();
        
        if (word.getWordType() == WordType.REPEAT)
            return WordComplexity.TOUGH.getWeight();
        
        return WordComplexity.NORMAL.getWeight();
    }
}
