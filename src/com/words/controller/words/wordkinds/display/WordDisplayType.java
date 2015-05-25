package com.words.controller.words.wordkinds.display;

import com.words.controller.sound.PlayMp3;
import com.words.controller.words.Word;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Enum represents possible word display types.
 * Display type determines which part of a word will be shown
 * on the main button. Also determines sound policy for each type.
 * @author vlad
 */
public enum WordDisplayType {
    
    WORD {
        
        @Override
        public String getText(Word word) {
            return word.getWord();
        }
        
        @Override
        public void sound(Word word) {
            PlayMp3.playFile(word.getMp3File());
        }
        
        @Override
        public boolean isTranslation() { return false; }
    },
    
    TRANSLATION {
        
        @Override
        public String getText(Word word) {
            return word.getTranslation();
        }
        
        @Override
        public void sound(Word word) {
            PlayMp3.nextWord();
        }
                
        @Override
        public boolean isTranslation() { return true; }
    },
    
    BOTH {
        
        @Override
        public String getText(Word word) {
            return word.getBoth();
        }
        
        @Override
        public void sound(Word word) {
            PlayMp3.playFile(word.getMp3File());
        }
        
        @Override
        public boolean isTranslation() { return false; }
    },
    
    SYNONYMS {
        
        @Override
        public String getText(Word word) {
            if (word.getSynonyms().isEmpty())
                return WORD.getText(word) + " (no syns)";
            return "syns: " + word.getSynonyms();
        }
        
        @Override
        public void sound(Word word) {
            if (word.getSynonyms().isEmpty()) WORD.sound(word);
            else PlayMp3.nextWord();
        }
                
        @Override
        public boolean isTranslation() { return false; }
    },
    
    TRANSLATION_SHUFFLED {
        
        @Override
        public String getText(Word word) {
            String delimiter = Word.TRANSLATION_DELIMITER + " ";
            String translation = word.getTranslation();
            
            String[] parts = translation.split(delimiter);
            if (parts.length == 1) return word.getTranslation();
            
            RAND.setSeed(word.getWord().hashCode() + word.getTimesPicked());
            List<String> tokens = Arrays.asList(parts);
            Collections.shuffle(tokens, RAND);
            
            return tokens.stream().collect(Collectors.joining(delimiter));
        }
        
        @Override
        public void sound(Word word) {
            PlayMp3.nextWord();
        }        
        
        @Override
        public boolean isTranslation() { return true; }
    }, 
    
    TRANSLATION_PART {
        
        @Override
        public String getText(Word word) {
            String delimiter = Word.TRANSLATION_DELIMITER + " ";
            String translation = word.getTranslation();

            String[] parts = translation.split(delimiter);
            if (parts.length == 1) return word.getTranslation();
            
            RAND.setSeed(word.getWord().hashCode() + word.getTimesPicked());
            return "part: " + parts[RAND.nextInt(parts.length)];
        }
        
        @Override
        public void sound(Word word) {
            PlayMp3.nextWord();
        }
                
        @Override
        public boolean isTranslation() { return true; }
    };
    
    public abstract String getText(Word word);
    public abstract void sound(Word word);
    public abstract boolean isTranslation();
    
    // required to generate different seeds for different words
    private static final Random RAND = new Random();
}
