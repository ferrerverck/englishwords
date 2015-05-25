package com.words.gui;

import com.words.controller.Controller;
import com.words.controller.words.Word;
import com.words.controller.words.wordkinds.WordComplexity;
import com.words.gui.guiutils.ComplexityGuiUtils;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * Class encapsulates complexity gui elements: button and popup menu.
 * @author vlad
 */
class ComplexityGui {
    
    private static final int BUTTON_BORDER = 14;
    private static final Font BUTTON_FONT =
        new Font(Font.SANS_SERIF, Font.BOLD, 14);
    
    private static final List<WordComplexity> COMPLEXITIES = 
        WordComplexity.sortedValues();
    
    private final Controller controller;
    
    private final JButton complexityButton = new JButton();
    private final JPopupMenu complPopup = new JPopupMenu();
    private final ButtonGroup buttonGroup = new ButtonGroup();
    
    //***********
    // init actions
    private final Action decreaseComplexityAction = newChangeComplexityAction("‒",
        word -> word.getComplexity().decreaseComplexity());
    private final JButton minusButton =
        newComplexityButton(decreaseComplexityAction);
    Action getDecreaseComplexityAction() { return decreaseComplexityAction; }
    
    private final Action increaseComplexityAction = newChangeComplexityAction("+",
        word -> word.getComplexity().increaseComplexity());
    private final JButton plusButton =
        newComplexityButton(increaseComplexityAction);
    Action getIncreaseComplexityAction() { return increaseComplexityAction; }
    //***********
    
    public ComplexityGui(MainFrame parent, Controller ctrl) {
        this.controller = ctrl;
        
        complexityButton.setToolTipText("<html><b>Complexity");
        complexityButton.setFocusPainted(false);
        complexityButton.setFocusable(false);
        complexityButton.setIcon(ComplexityGuiUtils.getIcon(WordComplexity.NORMAL));
        int width = parent.getHeight();
        complexityButton.setPreferredSize(new Dimension(width, width));
        complexityButton.setEnabled(false);
        
        // create popup
        JLabel label = new JLabel(" Word complexity ");
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        complPopup.add(label);
        
        // buttons to increase and decrease complexity and skipping a next word
        JPanel plusMinusPanel = new JPanel(new GridLayout(1, 2, 2, 0));
        plusMinusPanel.setOpaque(false);
        plusMinusPanel.setBorder(
            new EmptyBorder(1, BUTTON_BORDER, 4, BUTTON_BORDER));
        
        plusMinusPanel.add(minusButton);
        plusMinusPanel.add(plusButton);
        complPopup.add(plusMinusPanel);
        
        complPopup.add(new JSeparator());
        
//        for (WordComplexity wc : WordComplexity.values()) {
//        COMPLEXITIES.stream().forEach(wc -> {
        for (int i = COMPLEXITIES.size() - 1; i >= 0; i--) {
            WordComplexity wc = COMPLEXITIES.get(i);
            ImageIcon icon = ComplexityGuiUtils.getIcon(wc);
            
            JRadioButtonMenuItem rb =
                new JRadioButtonMenuItem(wc.toString(), icon);
            rb.setDisabledIcon(icon);
            rb.setActionCommand(wc.name());
            rb.setAccelerator(
                KeyStroke.getKeyStroke("" + COMPLEXITIES.indexOf(wc)));
            
            complPopup.add(rb);
            buttonGroup.add(rb);
            
            rb.addActionListener(e ->
                controller.setComplexity(controller.getCurrentWord(), wc));
        }
        
        complexityButton.addActionListener(e -> {
            if (!complPopup.isVisible()) {
                complPopup.show(parent, 0, parent.getHeight());
            } else {
                complPopup.setVisible(false);
            }
        });
        
        complPopup.pack();
        
        if (UIManager.getLookAndFeel().getClass().getName().contains("gtk")) {
            Border border =
                BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK);
            complPopup.setBorder(border);
        }
    }
    
    public JButton getButton() {
        return complexityButton;
    }
    
    public JPopupMenu getPopup() {
        return complPopup;
    }
    
    /**
     * Change state of this gui element. Depends on controller.
     */
    public void stateChanged() {
        if (!controller.hasCurrentWord()) {
            complexityButton.setEnabled(false);
            return;
        }
        
        complexityButton.setEnabled(true);
        minusButton.setEnabled(true);
        plusButton.setEnabled(true);
        
        Word currentWord = controller.getCurrentWord();
        WordComplexity complexity = currentWord.getComplexity();
        
        complexityButton.setIcon(ComplexityGuiUtils.getIcon(complexity));
        complexityButton.setToolTipText(
            "<html><b>Complexity: " + complexity.toString());
        
        Collections.list(buttonGroup.getElements()).stream()
            .filter(button -> button.getActionCommand().equals(complexity.name()))
            .findAny().ifPresent(button -> button.setSelected(true));
        
        // complexity might be decreased only by 1
        // disable all buttons with inappropriate complexities
        int currentIndex = COMPLEXITIES.indexOf(complexity);
        Collections.list(buttonGroup.getElements()).stream().forEach(button -> {
            WordComplexity c = WordComplexity.valueOf(button.getActionCommand());
            int index = COMPLEXITIES.indexOf(c);
            button.setEnabled(currentIndex - index <= 1);
        });
        
//        if (controller.getWordDisplayType() == WordDisplayType.BOTH)
//            complexityButton.setEnabled(false);
//        if (currentWord.getWordType() == WordType.TEMPORARY)
//            complexityButton.setEnabled(false);
        
        // complexity boundry case
        if (complexity == COMPLEXITIES.get(0))
            minusButton.setEnabled(false);
        else if (complexity == COMPLEXITIES.get(COMPLEXITIES.size() - 1))
            plusButton.setEnabled(false);
    }
        
    private JButton newComplexityButton(Action action) {
        JButton button = new JButton(action);
        button.setFocusable(false);
        button.setFocusPainted(false);
        button.setFont(BUTTON_FONT);
        button.setToolTipText(
            "<html><b>Change complexity and<br>move to a next word");
        return button;
    }
    
    private AbstractAction newChangeComplexityAction(String text,
        Function<Word, WordComplexity> func) {
        return new AbstractAction(text) {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!controller.hasCurrentWord()) return;
                
                Word word = controller.getCurrentWord();
                complPopup.setVisible(false);
                
                controller.setComplexity(word, func.apply(word));
                controller.nextWord();
            }
        };
    }
}
