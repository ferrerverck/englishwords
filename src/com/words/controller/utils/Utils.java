package com.words.controller.utils;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

public class Utils {
    
    public static final Random RANDOM = new SecureRandom();
    
    private Utils() { throw new AssertionError(); }
    
    /**
     * Get correct word according to specified number.
     * @param number number to check
     * @return "word" or "words" depending on the number passed
     */
    public static String getEnglishNumeralWord(long number) {
        return number == 1L ? "word" : "words";
    }
    
    /**
     * Trim trailing punctuation from specified string.
     * @param str String to trim
     * @return trimmed string
     */
    public static String trimTrailingPunctuation(String str) {
        return str.replaceAll("[.,;:!?]+\\s*$", "");
    }
    
    /**
     * Get formatted english numeral according to language rules.
     * @param n amount of
     * @param word word
     * @return formatted string like "3 cats"
     */
    public static String getNumeralWithWord(long n, String word) {
        if (n == 1L) return "a " + word;
        return n + " " + word + "s";
    }
    
    /**
     * Normalize word to third parties.
     * String leading "to" for verbs.
     * @param word word to normalize
     * @return normalized word
     */
    public static String normalizeFor3rdParties(String word) {
        Objects.requireNonNull(word);
        return formatString(word).replaceAll("^to ", "");
    }
    
    /**
     * Format string for using in a model.
     * @param word word to format
     * @return formatted word in lower case without multiple white spaces
     */
    public static String formatString(String word) {
        return word.trim().toLowerCase().replace("\\s+", " ");
    }
    
    /**
     * Trims trailing prepositions.
     * @param word word to trim
     * @return trimmed word without prepositions (if exist)
     */
    public static String trimTrailingPrepositions(String word) {
        String regex = Arrays.asList("over", "on", "with", "in", "up", "to", "of")
            .stream()
            .collect(Collectors.joining("|", "\\s(?:", ")$"));
        return word.replaceAll(regex, "");
    }
    
    /**
     * @param word string word to normalize
     * @return mp3 file name for specified word
     */
    public static String getMp3FileName(String word) {
        return normalizeFor3rdParties(word) + ".mp3";
    }
}
