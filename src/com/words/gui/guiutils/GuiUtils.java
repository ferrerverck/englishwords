package com.words.gui.guiutils;

import com.words.controller.words.Word;
import com.words.controller.words.wordkinds.WordComplexity;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class GuiUtils {
    
    // color constants
    public static final Color SELECTION_COLOR = Color.decode("#AA0000");
    public static final Color ODD_COLOR = Color.decode("#E6E6E6");
    public static final Color EVEN_COLOR = Color.decode("#F2F2F2");
    public static final Color SYNONYMS_COLOR = Color.decode("#0000FF");
    public static final Color EDITED_COLOR = Color.decode("#E77471");
    
    private GuiUtils() { throw new AssertionError(); }
    
    /**
     * Create not focusable button.
     * @param text text on the button
     * @return new JButton instance
     */
    public static JButton newNotFocusableButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setFocusable(false);
        return b;
    }
    
    /**
     * Set buttons to the same the biggest one size
     * @param buttons buttons to change
     */
    public static void setSameButtonSize(JButton... buttons) {
        if (buttons.length < 2) return;
        
        int maxWidth = 0;
        int maxHeight = 0;
        
        for (JButton button : buttons) {
            maxWidth = Math.max(maxWidth, button.getPreferredSize().width);
            maxHeight = Math.max(maxHeight, button.getPreferredSize().height);
        }
        
        for (JButton button : buttons) {
            button.setPreferredSize(new Dimension(maxWidth, maxHeight));
        }
    }
    
    /**
     * Adds capability to change word complexity using active popup menu.
     * Method is used to prevent code replication.
     * Method uses higher-order functions to achieve flexibility.
     * @param menu menu to which you want to add more items
     * @param onCheckAction action which is executed on checkbox click
     * @param selectedWord used to auto select proper checkbox
     */
    public static void addComplexityMenuItem(JPopupMenu menu,
        Function<WordComplexity, ActionListener> onCheckAction,
        Supplier<Word> selectedWord) {
        
        ButtonGroup complButtonGroup = new ButtonGroup();
        
        // complexity submenu
        JMenu complMenu = new JMenu("Complexity");
        menu.add(complMenu);
        
        List<WordComplexity> complexities = WordComplexity.sortedValues();
        for (int i = complexities.size() - 1; i >= 0; i--) {
            WordComplexity wc = complexities.get(i);
            
            JRadioButtonMenuItem rb = new JRadioButtonMenuItem(wc.toString(),
                ComplexityGuiUtils.getIcon(wc));
            rb.setActionCommand(wc.name());
            rb.setAccelerator(KeyStroke.getKeyStroke("" + i));
            
            complMenu.add(rb);
            complButtonGroup.add(rb);
            
            rb.addActionListener(onCheckAction.apply(wc));
        }
        
        menu.addPopupMenuListener(new PopupMenuListener() {
            
            @Override
            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) { }
            
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) { }
            
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
                Collections.list(complButtonGroup.getElements()).stream()
                    .filter(b -> b.getActionCommand()
                        .equals(selectedWord.get().getComplexity().name()))
                    .findAny().ifPresent(button -> button.setSelected(true));
            }
        });
    }
}