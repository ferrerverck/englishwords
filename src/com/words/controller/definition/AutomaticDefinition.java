package com.words.controller.definition;

import com.words.controller.utils.Utils;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Downloads automatic definition for specified word.
 * @author vlad
 */
public abstract class AutomaticDefinition {
    
    public static final String NO_MATCH_FOUND =
        "\u00A0\u00A0\u00A0\u00A0No match found";
    
    protected static final int TIMEOUT_MILLIS = 2000;

    /**
     * Downloads normalized word. Implementations specific.
     * @param word to define
     * @return definition for this word or null
     */
    protected abstract String downloadDefinition(String word);
    
    /**
     * Downloads word definition from extraneous host.
     * Normalizes word for using with third parties.
     * Saves original formatting.
     * @param word word to check online
     * @return definition of specified word or {NO_MATCH_FOUND} string
     */
    public String getDefinition(String word) {
        word = Utils.normalizeFor3rdParties(word);
        
        String definition = downloadDefinition(word);

        if (definition == null) {
            String wordWithoutPrepositions = Utils.trimTrailingPrepositions(word);
            if (word.equals(wordWithoutPrepositions)) return NO_MATCH_FOUND;
            
            definition = downloadDefinition(wordWithoutPrepositions);
            if (definition == null) return NO_MATCH_FOUND;
        }
        
        return definition;
    }
}
