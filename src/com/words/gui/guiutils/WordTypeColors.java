package com.words.gui.guiutils;

import com.words.controller.words.wordkinds.WordType;
import java.awt.Color;
import java.util.EnumMap;

public class WordTypeColors {
    private static final EnumMap<WordType, Color> colorMap =
        new EnumMap<>(WordType.class);
    static {
        colorMap.put(WordType.STANDARD, Color.BLACK);
        colorMap.put(WordType.REPEAT, Color.decode("#5F04B4"));
        colorMap.put(WordType.RANDOM, Color.decode("#A00000"));
        colorMap.put(WordType.EBBINHAUS, Color.decode("#009900"));
        colorMap.put(WordType.TEMPORARY, Color.decode("#666600"));
    }
    
    private static final Color DEFAULT_COLOR = Color.GREEN;
    
    private WordTypeColors() { throw new AssertionError(); }
    
    /**
     * Color of a specified word type.
     * @param type word type
     * @return color
     */
    public static Color getColor(WordType type) {
        if (!colorMap.containsKey(type)) return DEFAULT_COLOR;
        return colorMap.get(type);
    }
    
    /**
     * Get html color as string for JOptionPane dialogs.
     * @param type WordType
     * @return html color string
     */
    public static String getHtmlColor(WordType type) {
        Color color = getColor(type);
        return String.format("#%02x%02x%02x",
            color.getRed(),
            color.getGreen(),
            color.getBlue());
    }
    
    /**
     * Foreground color for repeat word.
     * @return Color for repeat word
     */
    public static Color getForegroundColor() {
        return Color.YELLOW;
    }
    
    /**
     * Get average color
     * @param type word type
     * @param c old color
     * @return average color
     */
    public static Color getAverageColor(WordType type, Color c) {
        Color color = getColor(type);
        
        int r = (color.getRed() + c.getRed()) / 2;
        int g = (color.getGreen() + c.getGreen()) / 2;
        int b = (color.getBlue() + c.getBlue()) / 2;
        
        return new Color(r, g, b);
    }
}
