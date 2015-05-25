package com.words.controller.preferences;

import com.words.controller.utils.DateTimeUtils;
import com.words.main.EnglishWords;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ApplicationPreferences {
    
    private static final String NUMBER_DELIMITER = ";";
    
    private static final String LOCATION_ON_SCREEN = "locationOnScreen";
    private static final String SOUND_PREFS = "soundPrefs";
    private static final String TOOLTIP_PREFS = "tooltipPrefs";
    private static final String LOADING_TIME = "loadingTime";
    private static final String COMPACT_GUI_MODE = "compactGuiMode";
    
    private final Preferences prefs;
    private final String todayAsString;
    private final Path wordLogFile;
    
    public ApplicationPreferences(String projectDir) {
        wordLogFile = Paths.get(projectDir, "word history");
        
        todayAsString = DateTimeUtils.todayAsString();
        prefs = Preferences.userRoot().node(EnglishWords.class.getName());
    }
    
    public static void main(String[] args) throws BackingStoreException {
        System.out.println(EnglishWords.class.getName());
        ApplicationPreferences ap = new ApplicationPreferences("");
        
//        ap.prefs.remove(WORD_LOG);
        
        System.out.println(Arrays.toString(ap.prefs.keys()));
    }
    
    /**
     * Define frame position.
     * @return frame position as 2 element array
     * @throws ApplicationLocationException if previous location is unavailable
     */
    public int[] getLocation() throws ApplicationLocationException {
        String locationTokens[] =
            prefs.get(LOCATION_ON_SCREEN, "").split(NUMBER_DELIMITER);
        
        if (locationTokens.length != 2)
            throw new ApplicationLocationException();

        try {
            int x = Integer.parseInt(locationTokens[0]);
            int y = Integer.parseInt(locationTokens[1]);
            
            return new int[] {x, y};
        } catch(NumberFormatException nfe) {
            throw new ApplicationLocationException();
        }
    }
    
    /**
     * Set application location.
     * @param x x coordinate of the application window
     * @param y y coordinate
     */
    public void setLocation(int x, int y) {
        prefs.put(LOCATION_ON_SCREEN, x + NUMBER_DELIMITER + y);
    }
    
    /**
     * Get sound preferences.
     * @return current sound mode
     */
    public SoundPreferences getSoundPreferences() {
        try { 
            return SoundPreferences.valueOf(
                prefs.get(SOUND_PREFS, SoundPreferences.STANDARD.name()));
        } catch (IllegalArgumentException iae) {
            return SoundPreferences.STANDARD;
        }
    }
    
    /**
     * Set sound preferences
     * @param sp sound preferences enum constant
     */
    public void setSoundPreferences(SoundPreferences sp) {
        prefs.put(SOUND_PREFS, sp.name());
    }
    
    /**
     * Get tooltip preferences.
     * @return current tooltip behavior
     */
    public TooltipPreferences getTooltipPreferences() {
        try {
            return TooltipPreferences.valueOf(
                prefs.get(TOOLTIP_PREFS, TooltipPreferences.STANDARD.name()));
        } catch (IllegalArgumentException iae) {
            return TooltipPreferences.STANDARD;
        }
    }
    
    /**
     * Set tooltip preferences
     * @param tp enum constant
     */
    public void setTooltipPreferences(TooltipPreferences tp) {
        prefs.put(TOOLTIP_PREFS, tp.name());
    }
    
    /**
     * Get average loading time for this application.
     * @param nanos current loading time in nanoseconds
     * @return average loading time in nanoseconds as first element of an array
     *         total number of runs - for the second array element
     */
    public long[] getAverageLoadingTime(long nanos) {
        String[] loadingTokens = prefs.get(LOADING_TIME, "")
            .split(NUMBER_DELIMITER);
        
        long total = 0L;
        long runs = 0L;
        
        if (loadingTokens.length == 2) {
            try {
                total = Long.parseLong(loadingTokens[0]);
                runs = Long.parseLong(loadingTokens[1]);
                
                if (total < 0 || runs < 0) {
                    total = 0L;
                    runs = 0L;
                }
            } catch(NumberFormatException nfe) { 
                total = 0L;
                runs = 0L;
            }
        }
        
        runs++;
        total += nanos;
        
        prefs.put(LOADING_TIME, total + NUMBER_DELIMITER + runs);
        
        return new long[] {total / runs, runs};
    }
    
    /**
     * Get current word log. Used to print in console today's iterations.
     * @return string with message to show in the console or empty string
     */
    public String getWordLog() {
        try(BufferedReader in =
            Files.newBufferedReader(wordLogFile, Charset.forName("UTF-8"))) {
            String dateString = in.readLine();
            
            if (todayAsString.equals(dateString)) {
                String line;
                StringBuilder sb = new StringBuilder();
                
                while ((line = in.readLine()) != null)
                    sb.append(line).append("\n");
                
                return sb.toString().trim();
            }
            
            return "";
        } catch(IOException ioe) {
            return "";
        }
    }
    
    /**
     * Set word log. Usually used on close to save state of the program.
     * @param wordLog string to save
     */
    public void setWordLog(String wordLog) {
        try(BufferedWriter out =
            Files.newBufferedWriter(wordLogFile, Charset.forName("UTF-8"))) {
            out.write(todayAsString);
            out.write('\n');
            out.write(wordLog);
        } catch(IOException ioe) { }
    }
    
    /**
     * Gets current mode of the gui.
     * @return true if mode is compact
     */
    public boolean getCompactGuiMode() {
        return prefs.getBoolean(COMPACT_GUI_MODE, false);
    }
    
    /**
     * Sets compact gui mode.
     * @param compactMode boolean mode
     */
    public void setCompactGuiMode(boolean compactMode) {
        prefs.putBoolean(COMPACT_GUI_MODE, compactMode);
    }
}
