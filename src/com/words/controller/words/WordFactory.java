package com.words.controller.words;

import com.words.controller.callbacks.ConsoleCallback;
import com.words.controller.utils.DateTimeUtils;
import com.words.controller.words.wordkinds.WordComplexity;
import com.words.controller.words.wordkinds.WordType;
import com.words.controller.words.wordpool.WordPool;
import com.words.controller.words.wordpool.pickstrategy.PickStrategyFactory;
import com.words.controller.words.wordpool.pickstrategy.UniformStrategy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WordFactory {
    
    private static final Map<WordType, WordPool> MAP =
        new EnumMap<>(WordType.class);
    static {
        MAP.put(WordType.REPEAT,
            new WordPool(PickStrategyFactory.getRepeatStrategy()));
        MAP.put(WordType.RANDOM, new WordPool(
            PickStrategyFactory.getRecentStandardStrategy(Duration.ofDays(1L), 3)));
        MAP.put(WordType.EBBINHAUS,
            new WordPool(PickStrategyFactory.getEbbinghausStrategy()));
    }
    
    private WordFactory() { throw new AssertionError(); }
    
    private static Collection<Word> getWords(WordType wordType, int amount,
        Collection<Word> words) {
        WordPool wordPool = MAP.get(wordType);
        wordPool.clear();
        
        addWordsToPool(words, wordPool);
        
        if (wordPool.size() < 2) return Collections.emptyList();
        
        List<Word> list = new ArrayList<>();
        int n = 1 + wordPool.size() / 3;
        if (amount > n) amount = n;
        
        for (int i = 0; i < amount; i++)
            list.add(newInstance(wordType, wordPool));
        
        return list;
    }
    
    private static Word newInstance(WordType type, WordPool wordPool) {
        switch (type) {
        case REPEAT:
            return new RepeatWord(wordPool);
        case RANDOM:
            return new RandomWord(wordPool);
        case EBBINHAUS:
            return new EbbinghausWord(wordPool);
        default:
            throw new IllegalArgumentException();
        }
    }
    
    /**
     * Ebbinghaus words
     * @param amount amount of ebb words
     * @param words words to add to the pool
     * @return amount of words or less depending on words size
     */
    public static Collection<Word> getEbbinghausWords(int amount,
        Collection<Word> words) {
        return getWords(WordType.EBBINHAUS, amount, words);
    }
    
    /**
     * Random words
     * @param amount amount of words
     * @param words words to add to the pool
     * @return amount of random words or less depending on words size
     */
    public static Collection<Word> getRandomWords(int amount,
        Collection<Word> words) {
        return getWords(WordType.RANDOM, amount, words);
    }
    
    /**
     * Repeat words
     * @param amount amount of repeat words to return
     * @param words words for this set
     * @return amount of random words or less - depending on pool size
     */
    public static Collection<Word> getRepeatWords(int amount,
        Collection<Word> words) {
        return getWords(WordType.REPEAT, amount, words);
    }
    
    /**
     * Add new repeat word to the model. Correct word pools.
     * @param word concrete word not a wrapper class
     */
    public static void addRepeatWord(Word word) {
        MAP.get(WordType.REPEAT).addWordToQueue(word);
        MAP.get(WordType.RANDOM).deleteWord(word);
    }
    
    /**
     * Delete repeat word to the model. Correct word pools.
     * @param word concrete word not a wrapper class
     */
    public static void deleteRepeatWord(Word word) {
        MAP.get(WordType.REPEAT).deleteWord(word);
        MAP.get(WordType.RANDOM).addWordToQueue(word);
    }
    
    /**
     * New concrete empty word
     * @return word instance
     */
    public static Word newWord() { 
        Word word = new ConcreteWord();
        word.setLastPickedTimestamp(DateTimeUtils.getTodayAsMillis());
        return word;
    }
    
    /**
     * Dumps all currently available word pools.
     * @param console console to show word pools
     */
    public static void dumpWordPools(ConsoleCallback console) {
        WordPool wordPool;
        
        wordPool = MAP.get(WordType.RANDOM);
        if (!wordPool.isEmpty()) {
            console.addErrorMessage("Random word pool size: " +
                wordPool.size());
            console.addEmptyLine();
        }
        
        wordPool = MAP.get(WordType.REPEAT);
        if (!wordPool.isEmpty()) {
            wordPool.dumpWordPool(console, "Repeat word pool");
        }
        
        wordPool = MAP.get(WordType.EBBINHAUS);
        if (!wordPool.isEmpty()) {
            wordPool.dumpWordPool(console, "Ebbinhaus word pool");
        }
    }
    
    /**
     * New temporary self-deleting word.
     * @param wordToWrap word to add
     * @param wordPool wordPool to add
     * @param shouldHoldCondition condition than should hold to stay in the
     *                            pool. Can be null.
     * @param console console to show message after deletion
     * @return wrapped word
     */
    public static Word getSelfDeletingWord(Word wordToWrap, WordPool wordPool,
        Predicate<Word> shouldHoldCondition, ConsoleCallback console) {
        return new SelfDeletingWord(
            wordToWrap, wordPool, shouldHoldCondition, console);
    }
    
    /**
     * Get complexity self-deleting word, which deletes itself from the pool,
     * when complexity falls lower than specified one.
     * @param wordToWrap original word
     * @param wordPool word pool to work with
     * @param complexity threshold complexity
     * @return word instance
     */
    public static Word getComplexityWord(Word wordToWrap, WordPool wordPool,
        WordComplexity complexity) {
        return new ComplexityWord(wordToWrap, wordPool, complexity);
    }
    
    /**
     * Returns new instance of a word with exactly the same contents.
     * @param word original word which should be copied
     * @return new identical instance of the word
     */
    public static Word copyWord(Word word) {
        ConcreteWord copiedWord = new ConcreteWord();
        copiedWord.setWord(word.getWord());
        copiedWord.setTranslation(word.getTranslation());
        copiedWord.setSynonyms(word.getSynonyms());
        copiedWord.setComplexity(word.getComplexity());
        copiedWord.setBundle(word.getBundle());
        copiedWord.setLastPickedTimestamp(word.getLastPickedTimestamp());
        copiedWord.setTimesPicked(word.getTimesPicked());
        copiedWord.setWordType(word.getWordType());
        return copiedWord;
    }
    
    /**
     * Deletes specified word from pools.
     * @param word word to delete
     */
    public static void deleteWordFromPools(Word word) {
        MAP.values().forEach(wordPool -> wordPool.deleteWord(word));
    }

    /**
     * Add words to a pool. Fills queue according to lastPickedTimestamp.
     * @param words words to add
     * @param wordPool word pool
     * @throws NullPointerException if any of the arguments is null
     */
    public static void addWordsToPool(Collection<Word> words, WordPool wordPool) {
        Objects.requireNonNull(words);
        Objects.requireNonNull(wordPool);
        
        Map<Boolean, List<Word>> map = words.stream()
            .collect(Collectors.partitioningBy(word ->
                DateTimeUtils.isToday(word.getLastPickedTimestamp())));
        Collections.sort(map.get(true),
            Comparator.comparingLong(Word::getLastPickedTimestamp));
        wordPool.addWords(map.get(false));
        wordPool.addWordsToQueue(map.get(true));
        wordPool.adjustQueueSize();
    }
}
