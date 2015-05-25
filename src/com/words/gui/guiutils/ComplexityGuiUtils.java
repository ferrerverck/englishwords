package com.words.gui.guiutils;

import com.words.controller.words.wordkinds.WordComplexity;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.ImageIcon;

public class ComplexityGuiUtils {
    
    private static final Map<WordComplexity, Color> COLORS =
        new EnumMap<>(WordComplexity.class);
    
    private static final Map<WordComplexity, ImageIcon> ICONS =
        new EnumMap<>(WordComplexity.class);
    
    static {
        COLORS.put(WordComplexity.ELEMENTARY, Color.decode("#58FAF4"));
//        COLORS.put(WordComplexity.ELEMENTARY, Color.decode("#3BB9FF"));
        COLORS.put(WordComplexity.SIMPLE, Color.GREEN);
        COLORS.put(WordComplexity.EASY, Color.decode("#D7D700"));
        COLORS.put(WordComplexity.NORMAL, Color.GRAY);
        COLORS.put(WordComplexity.TOUGH, Color.decode("#FE9A2E"));
        COLORS.put(WordComplexity.COMPLEX, Color.RED);
        COLORS.put(WordComplexity.CHALLENGING, Color.decode("#800080"));
        
        Arrays.stream(WordComplexity.values())
            .forEach(wc -> ICONS.put(wc, createIcon(COLORS.get(wc))));
    }
    
    private static final int WIDTH = 16;
    private static final int GAP = 3;
    
    private ComplexityGuiUtils() { throw new AssertionError(); }
    
    private static ImageIcon createIcon(Color color) {
        BufferedImage img = new BufferedImage(WIDTH + 2 * GAP,
            WIDTH + 2 * GAP, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        
        GradientPaint gp = new GradientPaint(0, 0, color.brighter(),
            WIDTH + GAP, WIDTH + GAP, color.darker());
        g2.setPaint(gp);
        g2.fillOval(GAP - 1, GAP - 1, WIDTH + 1, WIDTH + 1);
        
        g2.setColor(Color.GRAY);
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(GAP - 1, GAP - 1, WIDTH + 1, WIDTH + 1);
        
        g2.setColor(Color.LIGHT_GRAY);
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(GAP, GAP, WIDTH - 1, WIDTH - 1);
        
        return new ImageIcon(img);
    }
    
    public static ImageIcon getIcon(WordComplexity complexity) {
        return ICONS.get(complexity);
    }
    
    public static Color getColor(WordComplexity complexity) {
        return COLORS.get(complexity);
    }
}
