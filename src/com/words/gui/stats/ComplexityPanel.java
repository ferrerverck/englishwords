package com.words.gui.stats;

import com.words.controller.words.wordkinds.WordComplexity;
import com.words.gui.guiutils.ComplexityGuiUtils;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class ComplexityPanel extends JPanel {
    
    private static final Color DEFAULT_COLOR = Color.LIGHT_GRAY;
    private static final int PREFERRED_HEIGHT = 90;
    
    private static final Color BORDER_COLOR = Color.LIGHT_GRAY;
    private static final int BORDER_WIDTH = 4;
    
    private final PaintPanel paintPanel = new PaintPanel();
    
    private final Map<WordComplexity, Long> values = new TreeMap<>();
    private long sum = 0;
    
    private final Map<WordComplexity, JLabel> labels;

    private final Font font;
    
    /*
    public static void main(String[] args) {
        Map<WordComplexity, Long> values = new HashMap<>();
        values.put(WordComplexity.EASY, 100L);
        values.put(WordComplexity.TOUGH, 100L);
        values.put(WordComplexity.NORMAL, 200L);
        
        try {
            UIManager.LookAndFeelInfo[] lafi
                = UIManager.getInstalledLookAndFeels();
            UIManager.setLookAndFeel(lafi[new Random().nextInt(lafi.length)]
                .getClassName());
        } catch (ClassNotFoundException | IllegalAccessException |
            InstantiationException | UnsupportedLookAndFeelException ex) {
        }
        
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Test Frame");
            
            ComplexityPanel cp = new ComplexityPanel();
            
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(cp);
            frame.setPreferredSize(new Dimension(400, 300));
            frame.pack();
            frame.setLocationRelativeTo(null);
            
            cp.updateValues(values);
            
            frame.setVisible(true);
        });
    }*/
    
    public ComplexityPanel(Font font) {
        this.font = font;
        
        setPreferredSize(new Dimension(300, PREFERRED_HEIGHT));
        
        setLayout(new BorderLayout());
        
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 1));
        labelPanel.setBorder(null);
        
        List<WordComplexity> sortedComplexities = WordComplexity.sortedValues();
        labels = new HashMap<>(sortedComplexities.size());
        sortedComplexities.stream().map(wc -> {
            JLabel label = new JLabel(wc.toString(),
                ComplexityGuiUtils.getIcon(wc), JLabel.CENTER);
            labels.put(wc, label);
            return label;            
        }).forEach(label -> {
            labelPanel.add(label);
        });
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(new EmptyBorder(3, 30, 3, 30));
        centerPanel.add(paintPanel, BorderLayout.CENTER);
        
        add(centerPanel, BorderLayout.CENTER);
        add(labelPanel, BorderLayout.SOUTH);
    }
    
    public void updateValues(Map<WordComplexity, Long> newValues) {
        values.clear();
        values.putAll(newValues);
        sum = values.values().stream().mapToLong(Long::longValue).sum();
        
        labels.entrySet().stream().forEach(entry -> {
            entry.getValue().setVisible(values.containsKey(entry.getKey()));
        });
        
        repaint();
    }
    
    private class PaintPanel extends JPanel {
        
        public PaintPanel() {
            Dimension d = new Dimension(1000, 30);
            setMinimumSize(d);
            setPreferredSize(d);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            
            RenderingHints qualityHints = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            qualityHints.put(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHints(qualityHints);
            
            if (sum == 0) {
                g2d.setColor(DEFAULT_COLOR);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                return;
            }
            
            int x = 0;
            int width;
            int height = getHeight();
            
            RoundRectangle2D clipArea = new RoundRectangle2D.Float(
                0, 0, getWidth(), height, height, height);
            g.setClip(clipArea);
            
            FontMetrics metrics = g2d.getFontMetrics(font);
            int textY = (height - metrics.getHeight()) / 2 +
                metrics.getAscent() + 1;
            
            for (Map.Entry<WordComplexity, Long> entry : values.entrySet()) {
                WordComplexity wc = entry.getKey();
                width = (int) (getWidth() * entry.getValue() / sum);
                
                Color color = ComplexityGuiUtils.getColor(wc);
                
                if (width > 80) {
                    GradientPaint gp1 = new GradientPaint(x, 0, color.darker(),
                        x + width / 2, height, color.brighter());
                    g2d.setPaint(gp1);
                    g2d.fillRect(x, 0, x + width / 2, height);
                    
                    GradientPaint gp2 = new GradientPaint(x + width / 2, height,
                        color.brighter(), x + width, 0, color.darker());
                    g2d.setPaint(gp2);
                    g2d.fillRect(x + width / 2, 0, x + width, height);
                } else {
                    GradientPaint gp3 = new GradientPaint(x, 0, color.brighter(),
                        x + width, height, color.darker());
                    g2d.setPaint(gp3);
                    g2d.fillRect(x, 0, x + width, height);
                }
                
                g2d.setColor(BORDER_COLOR);
                g2d.setStroke(new BasicStroke(BORDER_WIDTH));
                g2d.drawLine(x - BORDER_WIDTH / 2, 0,
                    x - BORDER_WIDTH / 2, height);
                
                String text = "" + entry.getValue();
                int textWidth = metrics.stringWidth(text);
                int textX = (width - textWidth) / 2 + x;
                
                if (textWidth <= width - 10) {
                    g2d.setColor(Color.BLACK);
                    g2d.setFont(font);
                    g2d.drawString(text, textX, textY);
                    labels.get(wc).setText(wc.toString());
                } else {
                    labels.get(wc).setText(
                        wc.toString() + " (" + entry.getValue() + ")");
                }
                
                x += width;
            }
            
            g2d.setColor(BORDER_COLOR);
            g2d.setStroke(new BasicStroke(BORDER_WIDTH));
            g2d.drawRoundRect(BORDER_WIDTH / 2 - 1, BORDER_WIDTH / 2 - 1,
                getWidth() - BORDER_WIDTH + 1, height - BORDER_WIDTH + 1,
                height, height);
        }
    }
}
