package com.words.controller.preferences;

import java.util.concurrent.TimeUnit;

public enum AutoMode {
    
    OFF("Off", 0),
    VERY_SLOW("Very slow", 120),
    FAST("Fast", 7),
    AVERAGE("Average", 35),
    SLOW("Slow", 70);
    
    private static final TimeUnit TIME_UNITS = TimeUnit.SECONDS;
    
    // define how many secods allowed to pass between autoMode changes
    private static final int SECONDS_TO_PASS = 3;
    private static long timeStamp;
    
    private final String description;
    private final int delay; // delay in TIME_UNITS
    
    private AutoMode(String description, int delay) {
        this.description = description;
        this.delay = delay;
    }
    
    public String getDescription() {
        String delayString = " (" + delay + " secs)";
        if (delay == 0) delayString = "";
        
        return description + delayString;
    }
    
    public int getDelay() {
        return delay;
    }
    
    /**
     * Pick next automode in predefined order.
     * If SECONDS_TO_PASS seconds passed, switch to SLOW mode.
     * @return new automode
     */
    public AutoMode nextAutoMode() {
        // updates static field but used exclusively in gui thread,
        // so no conflict here
        long lastTimeStamp = timeStamp;
        timeStamp = System.currentTimeMillis();
        long thisTimeStamp = timeStamp;
        
        if (this == OFF) {
            return SLOW;
        } else {
            if (thisTimeStamp - lastTimeStamp > SECONDS_TO_PASS * 1000) {
                return OFF;
            } else {
                return values()[(ordinal() - 1 + values().length) %
                    values().length];
            }
        }
    }
    
    /**
     * Get time units.
     * @return time units used for enum constants
     */
    public static TimeUnit getTimeUnits() {
        return TIME_UNITS;
    }
}
