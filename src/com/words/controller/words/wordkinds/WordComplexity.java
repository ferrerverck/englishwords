package com.words.controller.words.wordkinds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

public enum WordComplexity {
    
    ELEMENTARY(1),
    SIMPLE(10),
    EASY(50),
    NORMAL(100),
    TOUGH(200),
    COMPLEX(400),
    CHALLENGING(500, true);
    
    private static final List<WordComplexity> SORTED_BY_WEIGHT =
        sortedValues();
    
    private final int weight;
    private final boolean privileged;
    
    private WordComplexity(int weight) {
        this(weight, false);
    }
    
    private WordComplexity(int weight, boolean privileged) {
        this.privileged = privileged;
        this.weight = weight;
    }
    
    public int getWeight() {
        return weight;
    }
    
    @Override
    public String toString() {
        String name = name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
    
    public static List<WordComplexity> sortedValues() {
        List<WordComplexity> list = new ArrayList<>();
        Collections.addAll(list, values());
        Collections.sort(list,
            Comparator.comparingInt(WordComplexity::getWeight));
        return list;
    }
    
    /**
     * Define if the word has privileges.
     * @return true if complexity has special status
     */
    public boolean isPrivileged() { return privileged; }
    
    /**
     * Get decreased priority. The one which is 1 step lower.
     * @return new complexity
     */
    public WordComplexity decreaseComplexity() {
        int index = SORTED_BY_WEIGHT.indexOf(this) - 1;
        if (index == -1) index = 0;
        return SORTED_BY_WEIGHT.get(index);
    }
    
    /**
     * Get increased priority. The one which is 2 step higher.
     * @return new complexity
     */
    public WordComplexity increaseComplexity() {
        int index = SORTED_BY_WEIGHT.indexOf(this) + 2;
        if (index >= SORTED_BY_WEIGHT.size())
            index = SORTED_BY_WEIGHT.size() - 1;
        return SORTED_BY_WEIGHT.get(index);
    }
    
    /**
     * Defines if word complexity is harder or equal to other complexity.
     * @param other other complexity to check against
     * @return true if this word is harder
     */
    public boolean isNotEasierThan(WordComplexity other) {
        return this.getWeight() >= other.getWeight();
    }
    
    /**
     * Defines if word complexity is easier or equal to other complexity.
     * @param other other complexity to check against
     * @return true if this word is harder
     */
    public boolean isNotHarderThan(WordComplexity other) {
        return this.getWeight() <= other.getWeight();
    }
    
    /**
     * Defines closest complexity for specified average weight.
     * @param weight average weight to compare against
     * @return WordComplexity that is closest to specified weight
     */
    public static WordComplexity closest(int weight) {
        int min = Integer.MAX_VALUE;
        WordComplexity closest = null;
        
        for (WordComplexity wc : values()) {
            final int diff = Math.abs(wc.weight - weight);
            
            if (diff < min) {
                min = diff;
                closest = wc;
            }
        }
        
        return closest;
    }
}
