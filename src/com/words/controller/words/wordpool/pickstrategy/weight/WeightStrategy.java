package com.words.controller.words.wordpool.pickstrategy.weight;

import com.words.controller.words.Word;
import com.words.controller.words.wordpool.pickstrategy.PickStrategy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class WeightStrategy implements PickStrategy {
   
    private final List<Integer> weights = new ArrayList<>();
    private final Weighter weighter;
    
    private double probability;
    
    public WeightStrategy(Weighter weighter) {
        this.weighter = weighter;
    }
    
    /**
     * Recalculates weights for current word pool.
     * @return weight sum
     */
    private long calculateWeights(List<Word> list) {
        long currentTime = System.currentTimeMillis();
        
        long sum = 0L;
        weights.clear();
        
        for (Word word : list) {
            int w8 = weighter.getWeight(word, currentTime);
            sum += w8;
            weights.add(w8);
        }
        
        return sum;
    }
      
    @Override
    public Word nextWord(List<Word> list) {
        long calculatedSum = calculateWeights(list);
        if (calculatedSum == 0L) return list.remove(RAND.nextInt(list.size()));
        
        long r = nextLong(calculatedSum);
        int index = 0;
        long sum = 0;

        for (Integer i : weights) {
            sum += i;
            
            if (r < sum) {
                probability = 1.0 * i / calculatedSum;
                Word word = list.remove(index);
                return word;
            }
            
            index++;
        }
        
        System.err.println("Incorrect behaviour in priority behavior");
        return list.remove(0);
    }
    
    @Override
    public void insertIntoQueue(LinkedList<Word> queue, Word word) {
        if (word.getComplexity().isPrivileged() && queue.size() >= 5) {
            int index = queue.size() * 2 / 3;
            ListIterator<Word> iter = queue.listIterator(index);
            while (iter.hasNext()) {
                if (!iter.next().getComplexity().isPrivileged()) {
                    iter.previous();
                    iter.add(word);
                    return;
                }
            }
  
            queue.add(word);
        } else {
            PickStrategy.super.insertIntoQueue(queue, word);
        }
    }
    
    // like random.nextInt returns uniform random number from 0 (inclusive)
    // to range (not inclusive)
    private static long nextLong(long range) {
        long bits, val;
        do {
            bits = (RAND.nextLong() << 1) >>> 1;
            val = bits % range;
        } while (bits - val +(range - 1) < 0L);
        return val;
    }

    @Override
    public double getLastProbability() {
        return probability;
    }
}
