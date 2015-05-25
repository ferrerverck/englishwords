package com.words.controller.words.wordkinds.display.strategy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class should be used to change word display types.
 * It should be used in a gui to create menu items with possible types.
 * Controller should change display type using an instance of this class.
 * @author vlad
 */
public class WordDisplayFactory {
    
    // The very first item corresponds to standard strategy by contract.
    // Other items should be filled lazily using computeIfAbsent method.
    // Standard strategy should be initialised in constructor.
    public static final List<String> DESCRIPTIONS = Arrays.asList(
        "Standard", "Both", "Word", "Translation", "Synonyms");
    
    private static DisplayStrategy compute(String desc) {
        int index = DESCRIPTIONS.indexOf(desc);
        switch (index) {
        case 1: return new BothDisplayStrategy();
        case 2: return new WordDisplayStrategy();
        case 3: return new TranslationDisplayStrategy();
        case 4: return new SynonymsDisplayStrategy();
        default: return null;
        }
    }
    
    private final DisplayStrategy standardStrategy;
    private final Map<String, DisplayStrategy> strategies =
        new HashMap<>();
    
    /**
     * Public constructor. Instance should be used by controller.
     */
    public WordDisplayFactory() {
        standardStrategy = new StandardDisplayStrategy();
        strategies.put(DESCRIPTIONS.get(0), standardStrategy);
    }
    
    public DisplayStrategy getDisplayStrategy(String description) {
        DisplayStrategy s = strategies.computeIfAbsent(
            description, d -> compute(d));
        return s != null ? s : standardStrategy;
    }
    
    public DisplayStrategy getDefaultStrategy() {
        return standardStrategy;
    }
}
