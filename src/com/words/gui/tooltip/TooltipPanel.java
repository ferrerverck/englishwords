package com.words.gui.tooltip;

import com.words.controller.utils.DateTimeUtils;
import com.words.controller.words.Word;
import com.words.gui.guiutils.WordTypeColors;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

class TooltipPanel extends JPanel {
    
    private static final Font FONT = new Font(Font.DIALOG,
        Font.BOLD/* + Font.ITALIC*/, 24);
    private static final Font SMALL_FONT = FONT.deriveFont(18f);
    private static final Font INFO_FONT = FONT.deriveFont(16f);
    private static final int BORDER_WIDTH = 8;
    
    protected static final Color LABEL_COLOR = Color.WHITE;
    protected static final Color TRANSPARENT_COLOR
        = new Color(255, 255, 255, 0);
    // additional width to align borders
    private static final int ADDITIONAL_WIDTH = 40;

    protected final int height;
    protected int width;
    
    protected final JLabel wordLabel;
    protected final JLabel translationLabel;
    protected final JLabel infoLabel;
    
    protected Color bgColor; // tooltip background color
    
    public TooltipPanel(int height) {
        this.height = height;
        
        setBorder(new EmptyBorder(5, BORDER_WIDTH, 5, BORDER_WIDTH));
        
        width = 200;
        
        wordLabel = new JLabel("English Words");
        wordLabel.setForeground(LABEL_COLOR);
        wordLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        wordLabel.setFont(FONT);
        wordLabel.setHorizontalAlignment(JLabel.CENTER);
        
        translationLabel = new JLabel("");
        translationLabel.setForeground(LABEL_COLOR);
        translationLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        translationLabel.setFont(FONT);
        translationLabel.setHorizontalAlignment(JLabel.CENTER);
        
        infoLabel = new JLabel("meaning goes here");
        infoLabel.setForeground(LABEL_COLOR);
        infoLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        infoLabel.setFont(INFO_FONT);
        infoLabel.setHorizontalAlignment(JLabel.CENTER);

        GridLayout gridLayout = new GridLayout(3, 1, 0, 0);
        setLayout(gridLayout);
        add(wordLabel);
        add(translationLabel);
        add(infoLabel);
    }
    
    /**
     * Set word. Change tooltip size.
     * @param word word
     */
    public void setWord(int parentWidth, Word word) {
        if (word == null || word.getWord() == null) return;
        
        wordLabel.setText(word.getWord());
        
        translationLabel.setText(word.getTranslation());
        
        if (word.getSynonyms().isEmpty()) {
            infoLabel.setText(String.format("%s (%s, %d, %s)",
                DateTimeUtils.localDateToString(word.getBundle()),
                word.timePassedString(), word.getTimesPicked(),
                word.getComplexity()));
        } else {
            infoLabel.setText(word.getSynonyms());
        }
        
        wordLabel.setFont(FONT);
        translationLabel.setFont(FONT);
        
        int wordWidth = wordLabel.getFontMetrics(FONT)
            .stringWidth(wordLabel.getText());
        if (wordWidth >= parentWidth) {
            wordLabel.setFont(SMALL_FONT);
            wordWidth = wordLabel.getFontMetrics(SMALL_FONT)
                .stringWidth(wordLabel.getText());
        }
        
        int translationWidth = translationLabel
            .getFontMetrics(FONT).stringWidth(translationLabel.getText());
        if (translationWidth >= parentWidth - 2 * BORDER_WIDTH) {
            translationLabel.setFont(SMALL_FONT);
            translationWidth = translationLabel.getFontMetrics(SMALL_FONT)
                .stringWidth(translationLabel.getText());
        }
        
        width = wordWidth > translationWidth
            ? wordWidth : translationWidth;
        int infoWidth = infoLabel.getFontMetrics(infoLabel.getFont())
            .stringWidth(infoLabel.getText());
        if (width < infoWidth) width = infoWidth;
        
        width += ADDITIONAL_WIDTH;
        if (width > parentWidth) width = parentWidth;
        
        bgColor = WordTypeColors.getColor(word.getWordType());
        
        repaint();
    }
    
    @Override
    public void paintComponent(Graphics g) {
        ((Graphics2D) g).setComposite(
            AlphaComposite.getInstance(AlphaComposite.SRC));
        g.clearRect(0, 0, getWidth(), getHeight());
        
        int x = (getWidth() - width) / 2;
        g.setColor(bgColor);
        g.fillRect(x, 0, width, height);
        g.setColor(LABEL_COLOR);
        g.drawRect(x + 2, 2, width - 5, height - 5);
        g.drawRect(x + 3, 3, width - 7, height - 7);
    }
}