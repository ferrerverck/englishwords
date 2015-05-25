package com.words.controller.words.wordkinds.display.strategy;

import com.words.controller.utils.Utils;
import com.words.controller.words.Word;
import com.words.controller.words.wordkinds.display.WordDisplayType;
import static com.words.controller.words.wordkinds.display.WordDisplayType.SYNONYMS;
import static com.words.controller.words.wordkinds.display.WordDisplayType.TRANSLATION_SHUFFLED;
import static com.words.controller.words.wordkinds.display.WordDisplayType.TRANSLATION_PART;
import static com.words.controller.words.wordkinds.display.WordDisplayType.WORD;
import java.util.Random;

public class RandomDisplayStrategy implements DisplayStrategy {
        
    private static final Random RAND = Utils.RANDOM;
    
    @Override
    public WordDisplayType getNextType(int iters, Word word) {
        int random = RAND.nextInt(100);
        int synsProb = 1 + iters / 300;
        int partTranslationProb = 2 + iters / 300;
        
        if (random < partTranslationProb) return TRANSLATION_PART;
        if (random < synsProb + partTranslationProb) return SYNONYMS;
        
        if (RAND.nextBoolean()) return WORD;
        return TRANSLATION_SHUFFLED;
    }
}
