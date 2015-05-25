package com.words.controller.words;

import com.words.controller.words.wordkinds.WordComplexity;
import com.words.controller.words.wordkinds.WordType;
import com.words.controller.words.wordpool.WordPool;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.function.Predicate;

/**
 * Class is used for inheritance to execute some action if intrinsic condition
 * is not satisfied, e.g. used to delete word after N picks.
 * @author vlad
 */
abstract class WordWithCondition implements Word {
    
    protected final Word word;
    protected final WordPool wordPool;
    private Predicate<Word> condition;
    
    protected abstract void conditionIsNotSatisfied();
    
    // checks if condition doesn't hold
    private void check() {
        if (!condition.test(word)) {
            conditionIsNotSatisfied();
        }
    }

    WordWithCondition(Word word, WordPool wordPool) {
        this(word, wordPool, w -> true);
    }

    WordWithCondition(Word word, WordPool wordPool, 
        Predicate<Word> shouldHoldCondition) {
        this.word = word;
        this.wordPool = wordPool;
        this.condition = (shouldHoldCondition == null) ?
            w -> true : shouldHoldCondition;
    }
    
    /**
     * Create complex condition using logical AND.
     * Should be used by subclasses to implements more complex conditions.
     * @param otherCondition other condition
     */
    protected final void andCondition(Predicate<Word> otherCondition) {
        condition = otherCondition.and(condition);
    }
    
    @Override
    public String getWord() { return word.getWord(); }

    @Override
    public void setWord(String wordToSet) {
        word.setWord(wordToSet);
        check();
    }

    @Override
    public String getTranslation() { return word.getTranslation(); }

    @Override
    public void setTranslation(String translation) {
        word.setTranslation(translation);
        check();
    }

    @Override
    public LocalDate getBundle() { return word.getBundle(); }

    @Override
    public void setBundle(LocalDate date) {
        word.setBundle(date);
        check();
    }

    @Override
    public String getBoth() { return word.getBoth(); }

    @Override
    public Path getMp3File() { return word.getMp3File(); }

    @Override
    public void setMp3File(Path mp3File) {
        word.setMp3File(mp3File);
        check();
    }

    @Override
    public String timePassedString() { return word.timePassedString(); }

    @Override
    public String getSynonyms() { return word.getSynonyms(); }

    @Override
    public void setSynonyms(String synonyms) {
        word.setSynonyms(synonyms);
        check();
    }

    @Override
    public void hasBeenPicked(long millis, String previousWord) {
        word.hasBeenPicked(millis, previousWord);
        check();
    }

    @Override
    public WordType getWordType() {
        return word.getWordType() == WordType.REPEAT ?
            WordType.REPEAT : WordType.TEMPORARY;
    }
    
    @Override
    public void setWordType(WordType type) {
        word.setWordType(type);
        check();
    }

    @Override
    public WordComplexity getComplexity() { return word.getComplexity(); }

    @Override
    public void setComplexity(WordComplexity complexity) { 
        word.setComplexity(complexity);
        check();
    }

    @Override
    public long getLastPickedTimestamp() {
        return word.getLastPickedTimestamp();
    }

    @Override
    public void setLastPickedTimestamp(long millis) {
        word.setLastPickedTimestamp(millis);
        check();
    }

    @Override
    public String getLastPickedString() { return word.getLastPickedString(); }
    
    @Override
    public int getTimesPicked() { return word.getTimesPicked(); }

    @Override
    public void setTimesPicked(int n) {
        word.setTimesPicked(n);
        check();
    }

    @Override
    public boolean isSingleWord() { return true; }
}
