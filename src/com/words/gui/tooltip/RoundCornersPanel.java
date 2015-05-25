package com.words.gui.tooltip;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Tooltip with rounded corners
 * @author vlad
 */
public class RoundCornersPanel extends TooltipPanel {
    
    private static final int ARC_WIDTH = 60;
    private static final int BORDER_WIDTH = 2;
    
    public RoundCornersPanel(int height) {
        super(height);
    }
    
    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        
        // clear
        g2.setComposite(AlphaComposite.Src);
        g2.clearRect(0, 0, getWidth(), getHeight());
        
        int x = (getWidth() - width) / 2;

        GradientPaint gp = new GradientPaint(0, 0, bgColor.darker(),
            0, height / 2, bgColor);
        g2.setPaint(gp);
        g2.fillRoundRect(x, 0, width, height, ARC_WIDTH, ARC_WIDTH);
        
        g2.setColor(LABEL_COLOR);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x + BORDER_WIDTH, BORDER_WIDTH,
            width - 2 * BORDER_WIDTH - 1, height - 2 * BORDER_WIDTH - 1,
            ARC_WIDTH - BORDER_WIDTH, ARC_WIDTH - BORDER_WIDTH);
    }
}
